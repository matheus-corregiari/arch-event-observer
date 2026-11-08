package br.com.arch.toolkit.compose.observable

import androidx.compose.runtime.Composable
import br.com.arch.toolkit.result.DataResult
import br.com.arch.toolkit.result.EventDataStatus
import kotlin.reflect.KClass

internal class ErrorWithThrowableObservable<T, E : Throwable>(
    private val status: EventDataStatus,
    private val clazz: KClass<E>,
    private val func: @Composable (E, T?) -> Unit
) : ComposeObservable<T, E>() {
    @Suppress("UNCHECKED_CAST")
    @Composable
    override fun Content(result: DataResult<T>) {
        val error = result.error
        if (clazz.isInstance(error)) func(error as E, result.data)
    }

    override fun hasVisibleContent(result: DataResult<T>) =
        result.isError && clazz.isInstance(result.error) && status.considerEvent(result)
}
