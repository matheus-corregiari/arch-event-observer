@file:Suppress("TooManyFunctions")

package br.com.arch.toolkit.result

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlin.jvm.JvmName
import kotlin.reflect.KClass

/**
 * Snapshot of an operation result.
 *
 * Holds the payload, the error, and the current [DataResultStatus]. The helper
 * properties below describe the current shape of the result and the observation
 * helpers at the bottom attach callbacks for the matching state.
 *
 * Equality, hash codes, destructuring and generated `copy()` use only these three
 * constructor properties. Execution settings belong to individual observation
 * calls and are never stored on this value. A null scope/dispatcher uses the
 * [ObserveWrapper] defaults: Main with a SupervisorJob, and Default for transforms.
 * Prefer an owned scope so cancellation follows the caller lifecycle.
 *
 * @param T payload type
 * @property data current payload, or `null`
 * @property error current error, or `null`
 * @property status current result status
 */
data class DataResult<T>(
    val data: T?,
    val error: Throwable?,
    val status: DataResultStatus
) {

    /**
     * `true` when [data] is not `null`.
     */
    val hasData: Boolean get() = data != null

    /**
     * `true` when [error] is not `null`.
     */
    val hasError: Boolean get() = error != null

    /**
     * `true` when [data] is an empty [Collection], [Map], or [Sequence].
     *
     * Sequence checks obtain one iterator and call `hasNext()` once.
     * They may trigger producer work in `hasNext()`. A constrained-once sequence
     * cannot be inspected again; materialize finite sequences before repeated observation.
     */
    val isEmpty: Boolean
        get() = when (data) {
            is Collection<*> -> data.isEmpty()
            is Map<*, *> -> data.isEmpty()
            is Sequence<*> -> !data.iterator().hasNext()
            else -> false
        }

    /**
     * `true` when [data] is a non-empty [Collection], [Map], or [Sequence].
     *
     * Sequence checks obtain one iterator and call `hasNext()` once.
     * They may trigger producer work in `hasNext()`. A constrained-once sequence
     * cannot be inspected again; materialize finite sequences before repeated observation.
     */
    val isNotEmpty: Boolean
        get() = when (data) {
            is Collection<*> -> data.isNotEmpty()
            is Map<*, *> -> data.isNotEmpty()
            is Sequence<*> -> data.iterator().hasNext()
            else -> false
        }

    /**
     * `true` when [data] is a single-item [Collection], [Map], or [Sequence].
     *
     * Sequence checks obtain one iterator and inspect at most two elements.
     * They may trigger producer work in `hasNext()`. A constrained-once sequence
     * cannot be inspected again; materialize finite sequences before repeated observation.
     */
    val hasOneItem: Boolean
        get() = when (data) {
            is Collection<*> -> data.size == 1
            is Map<*, *> -> data.size == 1
            // A second element is enough to disprove a single-item sequence.
            is Sequence<*> -> data.take(2).count() == 1
            else -> false
        }

    /**
     * `true` when [data] is a multi-item [Collection], [Map], or [Sequence].
     *
     * Sequence checks obtain one iterator and inspect at most two elements.
     * They may trigger producer work in `hasNext()`. A constrained-once sequence
     * cannot be inspected again; materialize finite sequences before repeated observation.
     */
    val hasManyItems: Boolean
        get() = when (data) {
            is Collection<*> -> data.size > 1
            is Map<*, *> -> data.size > 1
            // Two elements establish multiplicity; the remaining tail is irrelevant.
            is Sequence<*> -> data.take(2).count() > 1
            else -> false
        }

    /**
     * `true` when [data] is a [Collection], [Map], or [Sequence].
     */
    val isListType: Boolean
        get() = hasData &&
            when (data) {
                is Collection<*>,
                is Map<*, *>,
                is Sequence<*> -> true

                else -> false
            }

    /**
     * `true` when [status] is [DataResultStatus.LOADING].
     */
    val isLoading: Boolean get() = status == DataResultStatus.LOADING

    /**
     * `true` when [status] is [DataResultStatus.ERROR].
     */
    val isError: Boolean get() = status == DataResultStatus.ERROR

    /**
     * `true` when [status] is [DataResultStatus.SUCCESS].
     */
    val isSuccess: Boolean get() = status == DataResultStatus.SUCCESS

    /**
     * `true` when [status] is [DataResultStatus.NONE].
     */
    val isNone: Boolean get() = status == DataResultStatus.NONE

    /**
     * Maps [data] into a new [DataResult] while preserving [error] and [status].
     *
     * If [data] is `null`, the current instance is represented as-is. If the
     * transformation throws, the returned result switches to [DataResultStatus.ERROR].
     * This synchronous value mapping uses no observation scope or dispatcher.
     */
    fun <R> transform(transform: (T) -> R): DataResult<R> = data?.runCatching {
        DataResult(transform(this), error, status)
    }?.getOrElse { error ->
        DataResult<R>(null, error, DataResultStatus.ERROR)
    } ?: DataResult(null, error, status)

    /**
     * Observes this value with execution settings local to this call.
     *
     * Null settings use [ObserveWrapper] defaults. [config] may override the
     * supplied settings. When provided, the caller owns [scope] and cancellation.
     * Transformations use [transformDispatcher]; callbacks run in [scope].
     */
    fun unwrap(
        scope: CoroutineScope? = null,
        transformDispatcher: CoroutineDispatcher? = null,
        config: ObserveWrapper<T>.() -> Unit
    ) = ObserveWrapper<T>().also {
        scope?.let(it::scope)
        transformDispatcher?.let(it::transformDispatcher)
    }.apply(config).attachTo(this)

    //region Data

    /**
     * Invokes [func] when [data] is not `null`.
     */
    fun data(
        scope: CoroutineScope? = null,
        transformDispatcher: CoroutineDispatcher? = null,
        func: suspend (T) -> Unit
    ) = unwrap(scope, transformDispatcher) { data(observer = func) }

    /**
     * Transforms [data] before invoking [func].
     */
    fun <R> data(
        transformer: suspend (T) -> R,
        scope: CoroutineScope? = null,
        transformDispatcher: CoroutineDispatcher? = null,
        func: suspend (R) -> Unit
    ) = unwrap(scope, transformDispatcher) { data(transformer = transformer, observer = func) }
    //endregion

    //region Loading

    /**
     * Invokes [func] with the loading flag for this result.
     */
    fun loading(
        scope: CoroutineScope? = null,
        func: suspend (Boolean) -> Unit
    ) = unwrap(scope = scope) { loading(observer = func) }

    /**
     * Invokes [func] when [status] is [DataResultStatus.LOADING].
     */
    fun showLoading(
        scope: CoroutineScope? = null,
        func: suspend () -> Unit
    ) = unwrap(scope = scope) { showLoading(observer = func) }

    /**
     * Invokes [func] when [status] is not [DataResultStatus.LOADING].
     */
    fun hideLoading(
        scope: CoroutineScope? = null,
        func: suspend () -> Unit
    ) = unwrap(scope = scope) { hideLoading(observer = func) }
    //endregion

    //region Error

    /**
     * Invokes [func] with [error] when [status] is [DataResultStatus.ERROR].
     */
    fun error(
        scope: CoroutineScope? = null,
        transformDispatcher: CoroutineDispatcher? = null,
        func: suspend (Throwable) -> Unit
    ) = unwrap(scope, transformDispatcher) { error(observer = func) }

    /**
     * Invokes [func] when [status] is [DataResultStatus.ERROR].
     */
    fun error(
        scope: CoroutineScope? = null,
        transformDispatcher: CoroutineDispatcher? = null,
        func: suspend () -> Unit
    ) = unwrap(scope, transformDispatcher) { error(observer = func) }

    /**
     * Transforms [error] before invoking [func].
     */
    fun <R> error(
        transformer: suspend (Throwable) -> R,
        scope: CoroutineScope? = null,
        transformDispatcher: CoroutineDispatcher? = null,
        func: suspend (R) -> Unit
    ) = unwrap(scope, transformDispatcher) { error(transformer = transformer, observer = func) }

    /**
     * Invokes [func] with [error] when it is an instance of [E].
     */
    @JvmName("errorTyped")
    inline fun <reified E : Throwable> error(
        scope: CoroutineScope? = null,
        transformDispatcher: CoroutineDispatcher? = null,
        noinline func: suspend (E) -> Unit
    ) = error(E::class, scope, transformDispatcher, func)

    /**
     * Invokes [func] when [error] is an instance of [E].
     */
    @JvmName("errorTyped")
    inline fun <reified E : Throwable> error(
        scope: CoroutineScope? = null,
        transformDispatcher: CoroutineDispatcher? = null,
        noinline func: suspend () -> Unit
    ) = error(E::class, scope, transformDispatcher, func)

    /**
     * Transforms [error] before invoking [func] when it is an instance of [E].
     */
    @JvmName("errorTyped")
    inline fun <reified E : Throwable, R> error(
        noinline transformer: suspend (E) -> R,
        scope: CoroutineScope? = null,
        transformDispatcher: CoroutineDispatcher? = null,
        noinline func: suspend (R) -> Unit
    ) = error(E::class, transformer, scope, transformDispatcher, func)

    /**
     * Invokes [func] with [error] when [status] is [DataResultStatus.ERROR] and [error] is an instance of [clazz].
     */
    fun <E : Throwable> error(
        clazz: KClass<E>,
        scope: CoroutineScope? = null,
        transformDispatcher: CoroutineDispatcher? = null,
        func: suspend (E) -> Unit
    ) = unwrap(scope, transformDispatcher) { error(clazz = clazz, observer = func) }

    /**
     * Invokes [func] when [status] is [DataResultStatus.ERROR] and [error] is an instance of [clazz].
     */
    fun <E : Throwable> error(
        clazz: KClass<E>,
        scope: CoroutineScope? = null,
        transformDispatcher: CoroutineDispatcher? = null,
        func: suspend () -> Unit
    ) = unwrap(scope, transformDispatcher) { error(clazz = clazz, observer = func) }

    /**
     * Transforms [error] before invoking [func] when [error] is an instance of [clazz].
     */
    fun <E : Throwable, R> error(
        clazz: KClass<E>,
        transformer: suspend (E) -> R,
        scope: CoroutineScope? = null,
        transformDispatcher: CoroutineDispatcher? = null,
        func: suspend (R) -> Unit
    ) = unwrap(scope, transformDispatcher) {
        error(clazz = clazz, transformer = transformer, observer = func)
    }
    //endregion
}
