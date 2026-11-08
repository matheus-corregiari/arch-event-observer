package br.com.arch.toolkit.compose.observable

import androidx.compose.runtime.Composable
import br.com.arch.toolkit.result.DataResult
import br.com.arch.toolkit.result.EventDataStatus
import kotlin.reflect.KClass

internal class SuccessObservable<T>(
    private val status: EventDataStatus,
    private val func: @Composable () -> Unit
) : ComposeObservable<T, Unit>() {
    @Composable
    override fun Content(result: DataResult<T>) = func()

    override fun hasVisibleContent(result: DataResult<T>) =
        result.isSuccess && status.considerEvent(result) && result.isNone.not()
}

internal class TypedSuccessObservable<T, R : Any>(
    private val status: EventDataStatus,
    private val clazz: KClass<R>,
    private val func: @Composable (R) -> Unit
) : ComposeObservable<T, R>() {
    @Suppress("UNCHECKED_CAST")
    @Composable
    override fun Content(result: DataResult<T>) {
        val data = result.data
        if (clazz.isInstance(data)) func(data as R)
    }

    override fun hasVisibleContent(result: DataResult<T>) =
        result.isSuccess && clazz.isInstance(result.data) && status.considerEvent(result)
}
