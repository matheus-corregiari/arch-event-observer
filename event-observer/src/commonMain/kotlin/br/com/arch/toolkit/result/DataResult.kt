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
 * constructor properties. Legacy observation settings are local to this instance;
 * `copy()` and [transform] reset them. Prefer [unwrap] with an explicit scope to
 * configure execution per observation without mutating this value.
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

    private var scope: CoroutineScope? = null
    private var transformDispatcher: CoroutineDispatcher? = null

    /**
     * Sets the [CoroutineScope] used by the observation helpers on this instance.
     * Not propagated by `copy()` or [transform]. Prefer explicit-scope [unwrap].
     *
     * @param scope scope used to launch callbacks
     */
    fun scope(scope: CoroutineScope) = apply { this.scope = scope }

    /**
     * Creates a [CoroutineScope] from the supplied dispatcher and stores it for observers.
     * No external owner Job is supplied. Prefer an owned scope with [unwrap].
     * Not propagated by `copy()` or [transform].
     *
     * @param dispatcher dispatcher used to create the internal [CoroutineScope]
     */
    fun scope(scope: CoroutineDispatcher) = apply { this.scope = CoroutineScope(scope) }

    /**
     * Sets the dispatcher used by transformation callbacks on this instance.
     * Not propagated by `copy()` or [transform]. Prefer explicit-scope [unwrap].
     */
    fun transformDispatcher(dispatcher: CoroutineDispatcher) =
        apply { this.transformDispatcher = dispatcher }

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
     * The returned value does not inherit legacy observation settings.
     */
    fun <R> transform(transform: (T) -> R): DataResult<R> = data?.runCatching {
        DataResult(transform(this), error, status)
    }?.getOrElse { error ->
        DataResult<R>(null, error, DataResultStatus.ERROR)
    } ?: DataResult(null, error, status)

    /**
     * Creates an [ObserveWrapper], applies [config], and attaches this result.
     */
    fun unwrap(config: ObserveWrapper<T>.() -> Unit) = ObserveWrapper<T>().also {
        scope?.let(it::scope)
        transformDispatcher?.let(it::transformDispatcher)
    }.apply(config).attachTo(this)

    /**
     * Observes this value with execution settings local to this call.
     *
     * Ignores legacy instance settings. [config] may override these settings on
     * the wrapper. The caller owns [scope] and its cancellation; transformations
     * use [transformDispatcher] when supplied, otherwise the wrapper default.
     */
    fun unwrap(
        scope: CoroutineScope,
        transformDispatcher: CoroutineDispatcher? = null,
        config: ObserveWrapper<T>.() -> Unit
    ) = ObserveWrapper<T>().also {
        it.scope(scope)
        transformDispatcher?.let(it::transformDispatcher)
    }.apply(config).attachTo(this)

    //region Data

    /**
     * Invokes [func] when [data] is not `null`.
     */
    fun data(func: suspend (T) -> Unit) = unwrap { data(observer = func) }

    /**
     * Transforms [data] before invoking [func].
     */
    fun <R> data(transformer: suspend (T) -> R, func: suspend (R) -> Unit) =
        unwrap { data(transformer = transformer, observer = func) }
    //endregion

    //region Loading

    /**
     * Invokes [func] with the loading flag for this result.
     */
    fun loading(func: suspend (Boolean) -> Unit) = unwrap { loading(observer = func) }

    /**
     * Invokes [func] when [status] is [DataResultStatus.LOADING].
     */
    fun showLoading(func: suspend () -> Unit) = unwrap { showLoading(observer = func) }

    /**
     * Invokes [func] when [status] is not [DataResultStatus.LOADING].
     */
    fun hideLoading(func: suspend () -> Unit) = unwrap { hideLoading(observer = func) }
    //endregion

    //region Error

    /**
     * Invokes [func] with [error] when [status] is [DataResultStatus.ERROR].
     */
    fun error(func: suspend (Throwable) -> Unit) = unwrap { error(observer = func) }

    /**
     * Invokes [func] when [status] is [DataResultStatus.ERROR].
     */
    fun error(func: suspend () -> Unit) = unwrap { error(observer = func) }

    /**
     * Transforms [error] before invoking [func].
     */
    fun <R> error(transformer: suspend (Throwable) -> R, func: suspend (R) -> Unit) =
        unwrap { error(transformer = transformer, observer = func) }

    /**
     * Invokes [func] with [error] when it is an instance of [E].
     */
    @JvmName("errorTyped")
    inline fun <reified E : Throwable> error(noinline func: suspend (E) -> Unit) =
        error(E::class, func)

    /**
     * Invokes [func] when [error] is an instance of [E].
     */
    @JvmName("errorTyped")
    inline fun <reified E : Throwable> error(noinline func: suspend () -> Unit) =
        error(E::class, func)

    /**
     * Transforms [error] before invoking [func] when it is an instance of [E].
     */
    @JvmName("errorTyped")
    inline fun <reified E : Throwable, R> error(
        noinline transformer: suspend (E) -> R,
        noinline func: suspend (R) -> Unit
    ) = error(E::class, transformer, func)

    /**
     * Invokes [func] with [error] when [status] is [DataResultStatus.ERROR] and [error] is an instance of [clazz].
     */
    fun <E : Throwable> error(clazz: KClass<E>, func: suspend (E) -> Unit) =
        unwrap { error(clazz = clazz, observer = func) }

    /**
     * Invokes [func] when [status] is [DataResultStatus.ERROR] and [error] is an instance of [clazz].
     */
    fun <E : Throwable> error(clazz: KClass<E>, func: suspend () -> Unit) =
        unwrap { error(clazz = clazz, observer = func) }

    /**
     * Transforms [error] before invoking [func] when [error] is an instance of [clazz].
     */
    fun <E : Throwable, R> error(
        clazz: KClass<E>,
        transformer: suspend (E) -> R,
        func: suspend (R) -> Unit
    ) = unwrap { error(clazz = clazz, transformer = transformer, observer = func) }
    //endregion
}
