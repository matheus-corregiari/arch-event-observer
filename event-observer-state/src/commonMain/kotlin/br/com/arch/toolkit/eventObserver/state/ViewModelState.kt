package br.com.arch.toolkit.eventObserver.state

import androidx.lifecycle.SavedStateHandle
import br.com.arch.toolkit.result.DataResult
import br.com.arch.toolkit.util.dataResultError
import br.com.arch.toolkit.util.dataResultNone
import br.com.arch.toolkit.util.dataResultSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    default: T?
) {
    private val stored = StoredState(stateHandle, name, serializer, json, default)
    internal val operation = StateOperation(scope)

    /** Saved payload stream, independent of execution status. */
    val data: StateFlow<T?> get() = stored.flow

    /** Reads the restored or latest saved payload. */
    fun get(): T? = data.value

    /** Encodes before writing. Throws to the caller if encoding fails, preserving the prior state. */
    open fun set(value: T?) = stored.set(value)

    /** Compatibility overload: skips encoding equal values when [distinct] is true. */
    open fun set(value: T?, distinct: Boolean) {
        if (!distinct || value != get()) set(value)
    }

    /** Clears the payload without disconnecting observers. */
    fun invalidate() = set(null)

    /** Stops the active operation without clearing state or completing its observers. */
    fun cancel() = operation.cancel()

    /** Derives a smaller state without another persisted copy. */
    fun <R> select(transform: (T?) -> R): StateFlow<R> = data.select(transform)

    /** Plain values from one-shot or continuous sources. */
    class Regular<T : Any>(
        name: String,
        serializer: KSerializer<T>,
        stateHandle: SavedStateHandle,
        scope: CoroutineScope,
        json: Json = Json,
        default: T? = null
    ) : ViewModelState<T>(name, serializer, stateHandle, scope, json, default) {
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
            source.collect { value ->
                checkCurrent()
                val next = reduce(get(), value)
                checkCurrent()
                set(next)
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
        default: T? = null
    ) : ViewModelState<T>(name, serializer, stateHandle, scope, json, default) {
        private val transient = MutableStateFlow(
            get()?.let(::dataResultSuccess) ?: dataResultNone<T>()
        )
        private val results = ResultState(data, transient)

        /** One stable stream across all executions, including after each producer completes. */
        fun flow(): StateFlow<DataResult<T>> = results

        override fun set(value: T?) = set(value, distinct = false)

        /** Skips equal payload encoding while still resetting transient status. */
        override fun set(value: T?, distinct: Boolean) = results.update {
            if (!distinct || value != get()) super.set(value)
            transient.value = value?.let(::dataResultSuccess) ?: dataResultNone()
        }

        /** Results without payload keep the last saved data; use invalidate to explicitly clear. */
        fun set(value: DataResult<T>) = results.update {
            value.data?.let { super.set(it) }
            transient.value = value.copy(data = get())
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
        ): Job = operation.start({ transient.value = dataResultError(it, get()) }) { checkCurrent ->
            val source = func()
            checkCurrent()
            source.collect { result ->
                checkCurrent()
                val next = result.data?.let { reduce(get(), it) }
                checkCurrent()
                set(DataResult(next, result.error, result.status))
            }
        }
    }
}
