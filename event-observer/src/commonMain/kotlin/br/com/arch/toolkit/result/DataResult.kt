@file:Suppress("TooManyFunctions")

package br.com.arch.toolkit.result

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope

/**
 * Snapshot of an operation result.
 *
 * Holds the payload, the error, and the current [DataResultStatus]. The helper
 * properties below describe the current shape of the result and the observation
 * helpers at the bottom attach callbacks for the matching state.
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
     * Sets the [CoroutineScope] used by the observation helpers.
     *
     * @param scope scope used to launch callbacks
     */
    fun scope(scope: CoroutineScope) = apply { this.scope = scope }

    /**
     * Creates a [CoroutineScope] from [dispatcher] and stores it for observers.
     *
     * @param dispatcher dispatcher used to create the internal [CoroutineScope]
     */
    fun scope(scope: CoroutineDispatcher) = apply { this.scope = CoroutineScope(scope) }

    /**
     * Sets the dispatcher used by transformation callbacks.
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
     * Sequence checks are eager and consume the sequence.
     */
    val isEmpty: Boolean
        get() = when (data) {
            is Collection<*> -> data.isEmpty()
            is Map<*, *> -> data.isEmpty()
            is Sequence<*> -> data.count() == 0
            else -> false
        }

    /**
     * `true` when [data] is a non-empty [Collection], [Map], or [Sequence].
     *
     * Sequence checks are eager and consume the sequence.
     */
    val isNotEmpty: Boolean
        get() = when (data) {
            is Collection<*> -> data.isNotEmpty()
            is Map<*, *> -> data.isNotEmpty()
            is Sequence<*> -> data.count() > 0
            else -> false
        }

    /**
     * `true` when [data] is a single-item [Collection], [Map], or [Sequence].
     *
     * Sequence checks are eager and consume the sequence.
     */
    val hasOneItem: Boolean
        get() = when (data) {
            is Collection<*> -> data.size == 1
            is Map<*, *> -> data.size == 1
            is Sequence<*> -> data.count() == 1
            else -> false
        }

    /**
     * `true` when [data] is a multi-item [Collection], [Map], or [Sequence].
     *
     * Sequence checks are eager and consume the sequence.
     */
    val hasManyItems: Boolean
        get() = when (data) {
            is Collection<*> -> data.size > 1
            is Map<*, *> -> data.size > 1
            is Sequence<*> -> data.count() > 1
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
    //endregion
}
