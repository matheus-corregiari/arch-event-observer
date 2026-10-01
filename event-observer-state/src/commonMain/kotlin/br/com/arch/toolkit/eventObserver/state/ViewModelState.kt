package br.com.arch.toolkit.eventObserver.state

import androidx.lifecycle.SavedStateHandle
import br.com.arch.toolkit.result.DataResult
import br.com.arch.toolkit.result.DataResultStatus
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

/**
 * Screen state stored as JSON in the supplied SavedStateHandle. Call APIs on the main thread,
 * using a main-thread scope. Restoration guarantees are those of the handle's owner.
 * Keep payloads small and replace collections instead of mutating them in place.
 */
sealed class ViewModelState<T : Any>(
    /** Key owned by this state; its raw SavedStateHandle representation is a JSON string. */
    val name: String,
    serializer: KSerializer<T>,
    stateHandle: SavedStateHandle,
    /** Scope controlling operations, usually viewModelScope. */
    val scope: CoroutineScope,
    json: Json,
    default: T?,
    /** Dispatcher for repository work, transformations and serialization. */
    val workerDispatcher: CoroutineDispatcher
) {
    internal val stored = StoredState(stateHandle, name, serializer, json, default)
    internal val operation = StateOperation(scope)

    /** Saved payload stream, independent of execution status. */
    val data: StateFlow<T?> get() = stored.flow

    /** Reads the restored or latest saved payload. */
    fun get(): T? = data.value

    /**
     * Synchronous write for small values. Throws on codec failure, preserving the prior state.
     * Prefer [setAsync] for expensive payloads; this method runs on the calling thread.
     */
    open fun set(value: T?) = stored.set(value)

    /** Compatibility overload: skips encoding equal values when [distinct] is true. */
    open fun set(value: T?, distinct: Boolean) {
        if (!distinct || value != get()) set(value)
    }

    /**
     * Prepares a detached saved snapshot on [workerDispatcher], then commits on the owner scope.
     * Replaces the active operation. Prefer this to synchronous [set] for expensive payloads.
     * Plain failures propagate to the scope; result holders expose an Error with the old payload.
     */
    fun setAsync(value: T?): Job = operation.start(::onFailure) { checkCurrent ->
        val prepared = withContext(workerDispatcher) { stored.prepare(value) }
        checkCurrent()
        commitAsync(prepared, checkCurrent)
    }

    internal open fun commit(prepared: PreparedState<T>) = stored.commit(prepared)

    // A synchronous write can occur while preparation is running. Recompare off-owner until
    // the prepared base is current; there is no suspension between the final check and commit.
    internal suspend fun commitAsync(
        prepared: PreparedState<T>,
        checkCurrent: suspend () -> Unit,
        status: DataResultStatus? = null,
        error: Throwable? = null
    ) {
        var ready = prepared
        while (ready.base !== stored.current) {
            val base = stored.current
            ready = withContext(workerDispatcher) { stored.rebase(ready, base) }
            checkCurrent()
        }
        checkCurrent()
        if (status == null) commit(ready) else stored.commit(ready, status, error)
    }

    internal open fun onFailure(failure: Throwable): Unit = throw failure

    /** Clears the payload without disconnecting observers. */
    fun invalidate() = set(null)

    /** Stops the active operation without clearing state or completing its observers. */
    fun cancel() = operation.cancel()

    /** Synchronous projection for cheap work. Use [selectAsync] for expensive computations. */
    fun <R> select(transform: (T?) -> R): StateFlow<R> = data.select(transform)

    /** Computes an expensive derived state asynchronously; reads reuse its last completed value. */
    fun <R> selectAsync(initialValue: R, transform: suspend (T?) -> R): StateFlow<R> =
        data.selectAsync(scope, initialValue, workerDispatcher, transform)

    /** Plain values from one-shot or continuous sources. */
    class Regular<T : Any>(
        name: String,
        serializer: KSerializer<T>,
        stateHandle: SavedStateHandle,
        scope: CoroutineScope,
        json: Json = Json,
        default: T? = null,
        workerDispatcher: CoroutineDispatcher = Dispatchers.Default
    ) : ViewModelState<T>(name, serializer, stateHandle, scope, json, default, workerDispatcher) {
        /** Stable state; completing a bound flow leaves its last value here. */
        fun flow(): StateFlow<T?> = data

        /** Replaces the old operation. Unhandled errors propagate to the supplied scope. */
        fun bind(source: Flow<T>, onError: (Throwable) -> Unit = { throw it }): Job =
            bindMapped(source, onError) { it }

        /** Transforms each repository value into a saved view value. */
        fun <A> bindMapped(
            source: Flow<A>,
            onError: (Throwable) -> Unit = { throw it },
            transform: suspend (A) -> T
        ): Job = bindReducing(source, onError) { _, value -> transform(value) }

        /** Accumulates, merges or replaces data using a consumer-defined reducer. */
        fun <A> bindReducing(
            source: Flow<A>,
            onError: (Throwable) -> Unit = { throw it },
            reduce: suspend (T?, A) -> T
        ): Job = operation.start(onError) { checkCurrent ->
            val ownerContext = currentCoroutineContext().minusKey(Job)
            withContext(workerDispatcher) {
                source.collect { value ->
                    val current = withContext(ownerContext) {
                        checkCurrent()
                        get()
                    }
                    val next = reduce(current, value)
                    currentCoroutineContext().ensureActive()
                    val prepared = stored.prepare(next)
                    withContext(ownerContext) {
                        checkCurrent()
                        commitAsync(prepared, checkCurrent)
                    }
                }
            }
        }
    }

    /** Persists payloads; loading and errors are transient. Restored non-null data starts as Success. */
    class Result<T : Any>(
        name: String,
        serializer: KSerializer<T>,
        stateHandle: SavedStateHandle,
        scope: CoroutineScope,
        json: Json = Json,
        default: T? = null,
        workerDispatcher: CoroutineDispatcher = Dispatchers.Default
    ) : ViewModelState<T>(name, serializer, stateHandle, scope, json, default, workerDispatcher) {
        /** One stable stream across all executions, with payload and status from one snapshot. */
        fun flow(): StateFlow<DataResult<T>> = stored.results

        override fun set(value: T?) = set(value, distinct = false)

        /** Skips equal payload encoding while still resetting transient status. */
        override fun set(value: T?, distinct: Boolean) {
            if (!distinct || value != get()) {
                super.set(value)
            } else {
                stored.updateStatus(
                    if (value == null) DataResultStatus.NONE else DataResultStatus.SUCCESS,
                    null
                )
            }
        }

        internal override fun onFailure(failure: Throwable) {
            stored.updateStatus(DataResultStatus.ERROR, failure)
        }

        /** Results without payload retain the last saved data; invalidate explicitly clears it. */
        fun set(value: DataResult<T>) {
            val prepared = value.data?.let(stored::prepare)
            if (prepared == null) {
                stored.updateStatus(value.status, value.error)
            } else {
                stored.commit(prepared, value.status, value.error)
            }
        }

        /** Always starts a new request. To reuse restored data, check get() before calling. */
        fun load(func: suspend () -> Flow<DataResult<T>>): Job = loadMapped({ it }, func)

        /** Maps payloads while preserving each result's status and error. */
        fun <A> loadMapped(
            transform: suspend (A) -> T,
            func: suspend () -> Flow<DataResult<A>>
        ): Job = loadReducing({ _, value -> transform(value) }, func)

        /** Reduces each non-null payload into the current saved state. */
        fun <A> loadReducing(
            reduce: suspend (T?, A) -> T,
            func: suspend () -> Flow<DataResult<A>>
        ): Job = operation.start(::onFailure) { checkCurrent ->
            val ownerContext = currentCoroutineContext().minusKey(Job)
            withContext(workerDispatcher) {
                val source = func()
                source.collect { result ->
                    val current = withContext(ownerContext) {
                        checkCurrent()
                        get()
                    }
                    val prepared = result.data?.let { payload ->
                        val next = reduce(current, payload)
                        currentCoroutineContext().ensureActive()
                        stored.prepare(next)
                    }
                    withContext(ownerContext) {
                        checkCurrent()
                        if (prepared == null) {
                            stored.updateStatus(result.status, result.error)
                        } else {
                            commitAsync(prepared, checkCurrent, result.status, result.error)
                        }
                    }
                }
            }
        }
    }
}
