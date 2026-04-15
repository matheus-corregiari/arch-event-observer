@file:Suppress(
    "KotlinNullnessAnnotation",
    "TooManyFunctions",
    "CyclomaticComplexMethod",
    "UNCHECKED_CAST"
)

package br.com.arch.toolkit.result

import br.com.arch.toolkit.exception.DataResultException
import br.com.arch.toolkit.exception.DataResultTransformationException
import br.com.arch.toolkit.result.EventDataStatus.DoesNotMatter
import br.com.arch.toolkit.util.dataResultError
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

/**
 * Fluent observer DSL for [DataResult].
 *
 * The wrapper collects callbacks for the states you care about and executes
 * them against the attached result.
 */
class ObserveWrapper<T> internal constructor() {
    /**
     * Registered observation events.
     */
    internal val eventList = mutableListOf<ObserveEvent<*>>()

    /**
     * Catches unexpected exceptions raised while dispatching observers.
     */
    private val uncaughtHandler = CoroutineExceptionHandler { _, throwable ->
        when (throwable) {
            is DataResultException, is DataResultTransformationException -> throw throwable
        }

        when (val cause = throwable.cause) {
            is DataResultException, is DataResultTransformationException -> throw cause
        }

        if (eventList.none { it is ErrorEvent }) {
            throw DataResultException(
                message = "Any error event found, please add one error { ... } to retry",
                error = throwable
            )
        }

        suspendFunc {
            runCatching {
                handleResult(dataResultError(throwable))
            }.onFailure {
                throw DataResultException(
                    message = "Error retried but without any success",
                    error = throwable
                )
            }
        }
    }

    /**
     * Sets the [CoroutineScope] used to launch callbacks.
     */
    private var scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    fun scope(scope: CoroutineScope): ObserveWrapper<T> {
        this.scope = scope
        return this
    }

    /**
     * Sets the dispatcher used for transformation callbacks.
     */
    private var transformDispatcher: CoroutineDispatcher = Dispatchers.Default

    fun transformDispatcher(dispatcher: CoroutineDispatcher): ObserveWrapper<T> {
        transformDispatcher = dispatcher
        return this
    }

    //region Loading

    /**
     * Observes the loading flag as a boolean.
     */
    fun loading(
        single: Boolean = false,
        dataStatus: EventDataStatus = DoesNotMatter,
        observer: suspend (Boolean) -> Unit
    ): ObserveWrapper<T> {
        eventList.add(LoadingEvent(observer, single, dataStatus))
        return this
    }

    /**
     * Runs [observer] when the result is loading.
     */
    fun showLoading(
        single: Boolean = false,
        dataStatus: EventDataStatus = DoesNotMatter,
        observer: suspend () -> Unit
    ): ObserveWrapper<T> {
        eventList.add(ShowLoadingEvent(observer, single, dataStatus))
        return this
    }

    /**
     * Runs [observer] when the result is not loading.
     */
    fun hideLoading(
        single: Boolean = false,
        dataStatus: EventDataStatus = DoesNotMatter,
        observer: suspend () -> Unit
    ): ObserveWrapper<T> {
        eventList.add(HideLoadingEvent(observer, single, dataStatus))
        return this
    }
    //endregion

    //region Error

    /**
     * Runs [observer] when the result is in [DataResultStatus.ERROR].
     */
    fun error(
        single: Boolean = false,
        dataStatus: EventDataStatus = DoesNotMatter,
        observer: suspend () -> Unit
    ): ObserveWrapper<T> {
        eventList.add(
            ErrorEvent(
                wrapper = WrapObserver<Throwable, Any>(emptyObserver = observer),
                single = single,
                dataStatus = dataStatus
            )
        )
        return this
    }

    /**
     * Runs [observer] when the result is in [DataResultStatus.ERROR] and [DataResult.error] is available.
     */
    fun error(
        single: Boolean = false,
        dataStatus: EventDataStatus = DoesNotMatter,
        observer: suspend (Throwable) -> Unit
    ): ObserveWrapper<T> {
        eventList.add(
            ErrorEvent(
                wrapper = WrapObserver<Throwable, Any>(observer = observer),
                single = single,
                dataStatus = dataStatus
            )
        )
        return this
    }

    /**
     * Transforms the error before invoking [observer].
     */
    fun <R> error(
        single: Boolean = false,
        dataStatus: EventDataStatus = DoesNotMatter,
        transformer: suspend (Throwable) -> R,
        observer: suspend (R) -> Unit
    ): ObserveWrapper<T> {
        eventList.add(
            ErrorEvent(
                wrapper = WrapObserver(
                    transformer = transformer,
                    transformerObserver = observer
                ),
                single = single,
                dataStatus = dataStatus
            )
        )
        return this
    }
    //endregion

    //region Success

    /**
     * Runs [observer] when the result is in [DataResultStatus.SUCCESS].
     */
    fun success(
        single: Boolean = false,
        dataStatus: EventDataStatus = DoesNotMatter,
        observer: suspend () -> Unit
    ): ObserveWrapper<T> {
        eventList.add(
            SuccessEvent(
                wrapper = WrapObserver<Nothing, Any>(emptyObserver = observer),
                single = single,
                dataStatus = dataStatus
            )
        )
        return this
    }
    //endregion

    //region Data

    /**
     * Runs [observer] when [DataResult.data] is available.
     */
    fun data(
        single: Boolean = false,
        observer: suspend (T) -> Unit
    ): ObserveWrapper<T> {
        eventList.add(DataEvent(WrapObserver<T, Any>(observer = observer), single))
        return this
    }

    /**
     * Transforms [DataResult.data] before invoking [observer].
     */
    fun <R> data(
        single: Boolean = false,
        transformer: suspend (T) -> R,
        observer: suspend (R) -> Unit
    ): ObserveWrapper<T> {
        eventList.add(
            DataEvent(
                WrapObserver(
                    transformer = transformer,
                    transformerObserver = observer
                ),
                single
            )
        )
        return this
    }
    //endregion

    //region Result

    /**
     * Runs [observer] with the full [DataResult] snapshot.
     */
    fun result(
        single: Boolean = false,
        observer: suspend (DataResult<T>) -> Unit
    ): ObserveWrapper<T> {
        eventList.add(
            ResultEvent(
                WrapObserver<DataResult<T>, Any>(observer = observer),
                single
            )
        )
        return this
    }

    /**
     * Transforms the full [DataResult] before invoking [observer].
     */
    fun <R> result(
        single: Boolean = false,
        transformer: suspend (DataResult<T>) -> R,
        observer: suspend (R) -> Unit
    ): ObserveWrapper<T> {
        eventList.add(
            ResultEvent(
                WrapObserver(
                    transformer = transformer,
                    transformerObserver = observer
                ),
                single
            )
        )
        return this
    }
    //endregion

    //region Status

    /**
     * Runs [observer] with the current [DataResultStatus].
     */
    fun status(
        single: Boolean = false,
        observer: suspend (DataResultStatus) -> Unit
    ): ObserveWrapper<T> {
        eventList.add(
            StatusEvent(
                WrapObserver<DataResultStatus, Any>(observer = observer),
                single
            )
        )
        return this
    }

    /**
     * Transforms the current [DataResultStatus] before invoking [observer].
     */
    fun <R> status(
        single: Boolean = false,
        transformer: suspend (DataResultStatus) -> R,
        observer: suspend (R) -> Unit
    ): ObserveWrapper<T> {
        eventList.add(
            StatusEvent(
                WrapObserver(
                    transformer = transformer,
                    transformerObserver = observer
                ),
                single
            )
        )
        return this
    }
    //endregion

    //region Empty

    /**
     * Runs [observer] when [data] is an empty list-like value.
     */
    fun empty(
        single: Boolean = false,
        observer: suspend () -> Unit
    ): ObserveWrapper<T> {
        eventList.add(
            EmptyEvent(
                WrapObserver<Nothing, Any>(emptyObserver = observer),
                single
            )
        )
        return this
    }

    /**
     * Runs [observer] when [data] is a non-empty list-like value.
     */
    fun notEmpty(
        single: Boolean = false,
        observer: suspend () -> Unit
    ): ObserveWrapper<T> {
        eventList.add(
            NotEmptyEvent(
                WrapObserver<Nothing, Any>(emptyObserver = observer),
                single
            )
        )
        return this
    }

    /**
     * Runs [observer] when [data] contains exactly one list-like item.
     */
    fun oneItem(
        single: Boolean = false,
        observer: suspend () -> Unit
    ): ObserveWrapper<T> {
        eventList.add(
            OneItemEvent(
                WrapObserver<Nothing, Any>(emptyObserver = observer),
                single
            )
        )
        return this
    }

    /**
     * Runs [observer] when [data] contains more than one list-like item.
     */
    fun manyItems(
        single: Boolean = false,
        observer: suspend () -> Unit
    ): ObserveWrapper<T> {
        eventList.add(
            ManyItemsEvent(
                WrapObserver<Nothing, Any>(emptyObserver = observer),
                single
            )
        )
        return this
    }
    //endregion

    //region None

    /**
     * Runs [observer] when the result is [DataResultStatus.NONE].
     */
    fun none(
        single: Boolean = false,
        observer: suspend () -> Unit
    ): ObserveWrapper<T> {
        eventList.add(
            NoneEvent(
                WrapObserver<Nothing, Any>(emptyObserver = observer),
                single
            )
        )
        return this
    }
    //endregion

    //region Attach Methods

    /**
     * Launches a suspending block in the configured [CoroutineScope].
     */
    internal fun suspendFunc(func: suspend ObserveWrapper<T>.() -> Unit) =
        scope.launchWithErrorTreatment { func() }

    /**
     * Attaches this wrapper to [dataResult] and schedules the observers.
     */
    internal fun attachTo(dataResult: DataResult<T>) =
        apply { suspendFunc { handleResult(dataResult) } }
    //endregion

    @Suppress("LongMethod")
    internal suspend fun handleResult(
        result: DataResult<T>?,
        evaluateBeforeDispatch: suspend () -> Boolean = { true }
    ) {
        if (result == null) return

        eventList.iterate(result) { event ->
            return@iterate when {
                // Handle None
                result.isNone -> (event as? NoneEvent)?.wrapper?.handle(
                    data = null,
                    dispatcher = transformDispatcher,
                    evaluate = evaluateBeforeDispatch
                ) == true

                // Handle Loading
                event is LoadingEvent -> event.wrapper.handle(
                    data = result.isLoading,
                    dispatcher = transformDispatcher,
                    evaluate = evaluateBeforeDispatch
                ) &&
                    result.isLoading.not()

                // Handle ShowLoading
                event is ShowLoadingEvent && result.isLoading -> event.wrapper.handle(
                    data = true,
                    dispatcher = transformDispatcher,
                    evaluate = evaluateBeforeDispatch
                )

                // Handle HideLoading
                event is HideLoadingEvent && result.isLoading.not() -> event.wrapper.handle(
                    data = result.isLoading,
                    dispatcher = transformDispatcher,
                    evaluate = evaluateBeforeDispatch
                )

                // Handle Error
                event is ErrorEvent && result.isError -> event.wrapper.handle(
                    data = result.error,
                    dispatcher = transformDispatcher,
                    evaluate = evaluateBeforeDispatch
                )

                // Handle Success
                event is SuccessEvent && result.isSuccess -> event.wrapper.handle(
                    data = null,
                    dispatcher = transformDispatcher,
                    evaluate = evaluateBeforeDispatch
                )

                // Handle Data
                event is DataEvent -> (event as DataEvent<T>).wrapper.handle(
                    data = result.data,
                    dispatcher = transformDispatcher,
                    evaluate = evaluateBeforeDispatch
                ) &&
                    (result.data != null)

                // Handle Empty
                event is EmptyEvent && result.isListType && result.isEmpty -> event.wrapper.handle(
                    data = null,
                    dispatcher = transformDispatcher,
                    evaluate = evaluateBeforeDispatch
                )

                // Handle Not Empty
                event is NotEmptyEvent && result.isListType && result.isNotEmpty ->
                    event.wrapper.handle(
                        data = null,
                        dispatcher = transformDispatcher,
                        evaluate = evaluateBeforeDispatch
                    )

                // Handle One Item
                event is OneItemEvent && result.isListType && result.hasOneItem ->
                    event.wrapper.handle(
                        data = null,
                        dispatcher = transformDispatcher,
                        evaluate = evaluateBeforeDispatch
                    )

                // Handle Many Items
                event is ManyItemsEvent && result.isListType && result.hasManyItems ->
                    event.wrapper.handle(
                        data = null,
                        dispatcher = transformDispatcher,
                        evaluate = evaluateBeforeDispatch
                    )

                // Handle Result
                event is ResultEvent<*> -> (event as ResultEvent<T>).wrapper.handle(
                    data = result,
                    dispatcher = transformDispatcher,
                    evaluate = evaluateBeforeDispatch
                )

                // Handle Status
                event is StatusEvent -> event.wrapper.handle(
                    data = result.status,
                    dispatcher = transformDispatcher,
                    evaluate = evaluateBeforeDispatch
                )

                else -> false
            }
        }
    }

    private suspend inline fun MutableList<ObserveEvent<*>>.iterate(
        result: DataResult<*>,
        crossinline onEach: suspend (ObserveEvent<*>) -> Boolean
    ) {
        val iterator = iterator()
        while (iterator.hasNext()) {
            val wrapObserver = iterator.next()
            val eventDataStatusHandled = wrapObserver.dataStatus.considerEvent(result)
            val handled = eventDataStatusHandled && onEach.invoke(wrapObserver)
            if (wrapObserver.single && handled) {
                iterator.remove()
            }
        }
    }

    internal fun CoroutineScope.launchWithErrorTreatment(func: suspend () -> Unit) {
        fun Result<*>.catch() = onFailure { uncaughtHandler.handleException(coroutineContext, it) }
        runCatching { launch(uncaughtHandler) { runCatching { func() }.catch() } }.catch()
    }
}

internal class WrapObserver<T, V>(
    val observer: (suspend (T) -> Unit)? = null,
    val emptyObserver: (suspend () -> Unit)? = null,
    val transformer: (suspend (T) -> V)? = null,
    val transformerObserver: (suspend (V) -> Unit)? = null
) {
    suspend fun handle(
        data: T?,
        dispatcher: CoroutineDispatcher,
        evaluate: suspend () -> Boolean
    ) = when {
        evaluate.invoke().not() -> false

        emptyObserver != null -> {
            emptyObserver.invoke()
            true
        }

        data != null && observer != null -> {
            observer.invoke(data)
            true
        }

        data != null -> executeTransformer(data, dispatcher, evaluate)

        else -> false
    }

    private suspend fun executeTransformer(
        data: T,
        dispatcher: CoroutineDispatcher,
        evaluate: suspend () -> Boolean
    ) = when {
        transformerObserver == null -> false
        transformer == null -> false
        else -> {
            val result = withContext(dispatcher) {
                transformer.runCatching { invoke(data) }
            }

            val catch = CoroutineExceptionHandler { _, error -> throw error }
            withContext(coroutineContext + catch) {
                if (evaluate.invoke()) {
                    result.onSuccess {
                        transformerObserver.invoke(it)
                    }.onFailure {
                        throw DataResultTransformationException(
                            message = "Error performing transformation",
                            error = it
                        )
                    }
                    true
                } else {
                    false
                }
            }
        }
    }
}

internal sealed class ObserveEvent<T>(
    val wrapper: WrapObserver<T, *>,
    val single: Boolean,
    val dataStatus: EventDataStatus
)

private class LoadingEvent(
    observer: suspend (Boolean) -> Unit,
    single: Boolean,
    dataStatus: EventDataStatus
) : ObserveEvent<Boolean>(
    wrapper = WrapObserver<Boolean, Any>(observer),
    single = single,
    dataStatus = dataStatus
)

private class ShowLoadingEvent(
    observer: suspend () -> Unit,
    single: Boolean,
    dataStatus: EventDataStatus
) : ObserveEvent<Boolean>(
    wrapper = WrapObserver<Boolean, Any>(emptyObserver = observer),
    single = single,
    dataStatus = dataStatus
)

private class HideLoadingEvent(
    observer: suspend () -> Unit,
    single: Boolean,
    dataStatus: EventDataStatus
) : ObserveEvent<Boolean>(
    wrapper = WrapObserver<Boolean, Any>(emptyObserver = observer),
    single = single,
    dataStatus = dataStatus
)

private class ErrorEvent(
    wrapper: WrapObserver<Throwable, *>,
    single: Boolean,
    dataStatus: EventDataStatus
) : ObserveEvent<Throwable>(wrapper, single, dataStatus)

private class SuccessEvent(
    wrapper: WrapObserver<Nothing, *>,
    single: Boolean,
    dataStatus: EventDataStatus
) : ObserveEvent<Nothing>(wrapper, single, dataStatus)

private class DataEvent<T>(
    wrapper: WrapObserver<T, *>,
    single: Boolean
) : ObserveEvent<T>(wrapper, single, DoesNotMatter)

private class ResultEvent<T>(
    wrapper: WrapObserver<DataResult<T>, *>,
    single: Boolean
) : ObserveEvent<DataResult<T>>(wrapper, single, DoesNotMatter)

private class StatusEvent(
    wrapper: WrapObserver<DataResultStatus, *>,
    single: Boolean
) : ObserveEvent<DataResultStatus>(wrapper, single, DoesNotMatter)

private class NoneEvent(
    wrapper: WrapObserver<Nothing, *>,
    single: Boolean
) : ObserveEvent<Nothing>(wrapper, single, DoesNotMatter)

private class EmptyEvent(
    wrapper: WrapObserver<Nothing, *>,
    single: Boolean
) : ObserveEvent<Nothing>(wrapper, single, DoesNotMatter)

private class NotEmptyEvent(
    wrapper: WrapObserver<Nothing, *>,
    single: Boolean
) : ObserveEvent<Nothing>(wrapper, single, DoesNotMatter)

private class OneItemEvent(
    wrapper: WrapObserver<Nothing, *>,
    single: Boolean
) : ObserveEvent<Nothing>(wrapper, single, DoesNotMatter)

private class ManyItemsEvent(
    wrapper: WrapObserver<Nothing, *>,
    single: Boolean
) : ObserveEvent<Nothing>(wrapper, single, DoesNotMatter)
