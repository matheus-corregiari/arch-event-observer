@file:Suppress("TooManyFunctions", "FunctionNaming", "FunctionName")

package br.com.arch.toolkit.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import br.com.arch.toolkit.compose.observable.ComposeObservable
import br.com.arch.toolkit.compose.observable.DataObservable
import br.com.arch.toolkit.compose.observable.EmptyObservable
import br.com.arch.toolkit.compose.observable.ErrorObservable
import br.com.arch.toolkit.compose.observable.ErrorWithThrowableObservable
import br.com.arch.toolkit.compose.observable.HideLoadingObservable
import br.com.arch.toolkit.compose.observable.ManyObservable
import br.com.arch.toolkit.compose.observable.NoneObservable
import br.com.arch.toolkit.compose.observable.NotEmptyObservable
import br.com.arch.toolkit.compose.observable.ResultObservable
import br.com.arch.toolkit.compose.observable.ShowLoadingObservable
import br.com.arch.toolkit.compose.observable.SingleObservable
import br.com.arch.toolkit.compose.observable.StatusObservable
import br.com.arch.toolkit.compose.observable.SuccessObservable
import br.com.arch.toolkit.result.DataResult
import br.com.arch.toolkit.result.DataResultStatus
import br.com.arch.toolkit.result.EventDataStatus
import br.com.arch.toolkit.result.EventDataStatus.DoesNotMatter
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update

/**
 * A DSL wrapper used to register @Composable state observers for a [ComposableDataResult].
 *
 * This class provides a set of `On...` methods to define how different states of a [DataResult]
 * should be rendered in Compose.
 *
 * @param T The type of data being observed.
 */
@Stable
class ObserveComposableWrapper<T> internal constructor() {

    @DslMarker
    internal annotation class WrapperDsl

    private val observableList = atomic(listOf<ComposeObservable<T, *>>())
    internal val list by observableList

    // region Success

    /**
     * Registers a composable to be displayed when the [DataResult] is in a success state.
     *
     * @param dataStatus Optional filter for the presence of data.
     * @param func The @Composable content to render.
     */
    @WrapperDsl
    fun OnSuccess(
        dataStatus: EventDataStatus = DoesNotMatter,
        func: @Composable () -> Unit
    ) = register(SuccessObservable(dataStatus, func))

    // endregion

    // region Loading

    /**
     * Registers a composable to be displayed when the [DataResult] is in a loading state.
     *
     * @param dataStatus Optional filter for the presence of data.
     * @param func The @Composable content to render.
     */
    @WrapperDsl
    fun OnShowLoading(
        dataStatus: EventDataStatus = DoesNotMatter,
        func: @Composable () -> Unit
    ) = register(ShowLoadingObservable(dataStatus, func))

    /**
     * Registers a composable to be displayed when the [DataResult] transitions out of a loading state.
     *
     * @param dataStatus Optional filter for the presence of data.
     * @param func The @Composable content to render.
     */
    @WrapperDsl
    fun OnHideLoading(
        dataStatus: EventDataStatus = DoesNotMatter,
        func: @Composable () -> Unit
    ) = register(HideLoadingObservable(dataStatus, func))

    // endregion

    // region Error

    /**
     * Registers a composable to be displayed when the [DataResult] is in an error state.
     *
     * @param dataStatus Optional filter for the presence of data.
     * @param func The @Composable content to render.
     */
    @WrapperDsl
    fun OnError(
        dataStatus: EventDataStatus = DoesNotMatter,
        func: @Composable () -> Unit
    ) = register(ErrorObservable(dataStatus, func))

    /**
     * Registers a composable to be displayed when the [DataResult] is in an error state,
     * providing the [Throwable] that caused the error.
     *
     * @param dataStatus Optional filter for the presence of data.
     * @param func The @Composable content to render, receiving the [Throwable].
     */
    @WrapperDsl
    fun OnError(
        dataStatus: EventDataStatus = DoesNotMatter,
        func: @Composable (Throwable) -> Unit
    ) = register(ErrorWithThrowableObservable(dataStatus, func))

    // endregion

    //region None

    /**
     * Registers a composable to be displayed when the [DataResult] is in a 'None' state
     * (neither loading, success, nor error).
     *
     * @param func The @Composable content to render.
     */
    @WrapperDsl
    fun OnNone(func: @Composable () -> Unit) = register(NoneObservable(func))

    //endregion

    // region Data

    /**
     * Registers a composable to be displayed whenever there is data available in the [DataResult],
     * regardless of the current status (Success, Error, etc.).
     *
     * @param func The @Composable content to render, receiving the data [T].
     */
    @WrapperDsl
    inline fun OnData(
        crossinline func: @Composable (T) -> Unit
    ) = OnData { data, _, _ -> func(data) }

    /**
     * Registers a composable to be displayed whenever there is data available,
     * also providing the current [DataResultStatus].
     *
     * @param func The @Composable content to render, receiving data [T] and [DataResultStatus].
     */
    @WrapperDsl
    inline fun OnData(
        crossinline func: @Composable (T, DataResultStatus) -> Unit
    ) = OnData { data, status, _ -> func(data, status) }

    /**
     * Registers a composable to be displayed whenever there is data available,
     * providing the data, the status, and any potential [Throwable].
     *
     * @param func The @Composable content to render.
     */
    @WrapperDsl
    fun OnData(
        func: @Composable (T, DataResultStatus, Throwable?) -> Unit
    ) = register(DataObservable(func))

    // endregion

    // region Result

    /**
     * Registers a composable to be displayed for every [DataResult] emission,
     * providing the nullable data.
     *
     * @param func The @Composable content to render, receiving the nullable data [T?].
     */
    @WrapperDsl
    inline fun OnResult(
        crossinline func: @Composable (T?) -> Unit
    ) = OnResult { data, _ -> func(data) }

    /**
     * Registers a composable to be displayed for every [DataResult] emission,
     * providing nullable data and the current status.
     *
     * @param func The @Composable content to render.
     */
    @WrapperDsl
    inline fun OnResult(
        crossinline func: @Composable (T?, DataResultStatus) -> Unit
    ) = OnResult { data, status, _ -> func(data, status) }

    /**
     * Registers a composable to be displayed for every [DataResult] emission,
     * providing nullable data, the status, and any potential [Throwable].
     *
     * @param func The @Composable content to render.
     */
    @WrapperDsl
    fun OnResult(
        func: @Composable (T?, DataResultStatus, Throwable?) -> Unit
    ) = register(ResultObservable(func))
    // endregion

    // region Status

    /**
     * Registers a composable to be displayed when the [DataResult] matches a specific status.
     *
     * @param dataStatus Optional filter for the presence of data.
     * @param func The @Composable content to render, receiving the [DataResultStatus].
     */
    @WrapperDsl
    fun OnStatus(
        dataStatus: EventDataStatus = DoesNotMatter,
        func: @Composable (DataResultStatus) -> Unit
    ) = register(StatusObservable(dataStatus, func))

    // endregion

    // region List Type

    /**
     * Registers a composable to be displayed when the data is a collection and it is empty.
     *
     * @param func The @Composable content to render.
     */
    @WrapperDsl
    inline fun OnEmpty(
        crossinline func: @Composable () -> Unit
    ) = OnEmpty { _ -> func() }

    /**
     * Registers a composable to be displayed when the data is a collection and it is empty,
     * providing the current [DataResultStatus].
     *
     * @param func The @Composable content to render.
     */
    @WrapperDsl
    inline fun OnEmpty(
        crossinline func: @Composable (DataResultStatus) -> Unit
    ) = OnEmpty { status, _ -> func(status) }

    /**
     * Registers a composable to be displayed when the data is a collection and it is empty,
     * providing the status and any potential [Throwable].
     *
     * @param func The @Composable content to render.
     */
    @WrapperDsl
    fun OnEmpty(
        func: @Composable (DataResultStatus, Throwable?) -> Unit
    ) = register(EmptyObservable(func))

    /**
     * Registers a composable to be displayed when the data is a collection and it is NOT empty.
     *
     * @param func The @Composable content to render, receiving the data [T].
     */
    @WrapperDsl
    inline fun OnNotEmpty(
        crossinline func: @Composable (T) -> Unit
    ) = OnNotEmpty { data, _ -> func(data) }

    /**
     * Registers a composable to be displayed when the data is a collection and it is NOT empty,
     * providing the status.
     *
     * @param func The @Composable content to render.
     */
    @WrapperDsl
    inline fun OnNotEmpty(
        crossinline func: @Composable (T, DataResultStatus) -> Unit
    ) = OnNotEmpty { data, status, _ -> func(data, status) }

    /**
     * Registers a composable to be displayed when the data is a collection and it is NOT empty,
     * providing the data, status, and any potential [Throwable].
     *
     * @param func The @Composable content to render.
     */
    @WrapperDsl
    fun OnNotEmpty(
        func: @Composable (T, DataResultStatus, Throwable?) -> Unit
    ) = register(NotEmptyObservable(func))

    /**
     * Registers a composable to be displayed when the data is a collection and it contains exactly one item.
     *
     * @param R The type of the single item in the collection.
     * @param func The @Composable content to render, receiving the item [R].
     */
    @WrapperDsl
    inline fun <R> OnSingle(
        crossinline func: @Composable (R) -> Unit
    ) = OnSingle<R> { data, _ -> func(data) }

    /**
     * Registers a composable to be displayed when the data is a collection and it contains exactly one item,
     * providing the status.
     *
     * @param R The type of the single item in the collection.
     * @param func The @Composable content to render.
     */
    @WrapperDsl
    inline fun <R> OnSingle(
        crossinline func: @Composable (R, DataResultStatus) -> Unit
    ) = OnSingle<R> { data, status, _ -> func(data, status) }

    /**
     * Registers a composable to be displayed when the data is a collection and it contains exactly one item,
     * providing the item, status, and any potential [Throwable].
     *
     * @param R The type of the single item in the collection.
     * @param func The @Composable content to render.
     */
    @WrapperDsl
    fun <R> OnSingle(
        func: @Composable (R, DataResultStatus, Throwable?) -> Unit
    ) = register(SingleObservable(func))

    /**
     * Registers a composable to be displayed when the data is a collection and it contains multiple items.
     *
     * @param func The @Composable content to render, receiving the data [T].
     */
    @WrapperDsl
    inline fun OnMany(
        crossinline func: @Composable (T) -> Unit
    ) = OnMany { data, _ -> func(data) }

    /**
     * Registers a composable to be displayed when the data is a collection and it contains multiple items,
     * providing the status.
     *
     * @param func The @Composable content to render.
     */
    @WrapperDsl
    inline fun OnMany(
        crossinline func: @Composable (T, DataResultStatus) -> Unit
    ) = OnMany { data, status, _ -> func(data, status) }

    /**
     * Registers a composable to be displayed when the data is a collection and it contains multiple items,
     * providing the data, status, and any potential [Throwable].
     *
     * @param func The @Composable content to render.
     */
    @WrapperDsl
    fun OnMany(
        func: @Composable (T, DataResultStatus, Throwable?) -> Unit
    ) = register(ManyObservable(func))
    // endregion


    internal fun clear() = observableList.update { emptyList() }
    private fun register(observable: ComposeObservable<T, *>) =
        observableList.update { it + observable }

}
