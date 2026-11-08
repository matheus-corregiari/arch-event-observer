package br.com.arch.toolkit.compose.observable

import androidx.compose.runtime.Composable
import br.com.arch.toolkit.result.DataResult
import br.com.arch.toolkit.result.EventDataStatus

internal class SuccessWithDataObservable<T>(
    private val status: EventDataStatus,
    private val func: @Composable (T) -> Unit
) : ComposeObservable<T, T>() {
    @Composable
    override fun Content(result: DataResult<T>) {
        val data = result.data ?: return
        func(data)
    }

    override fun hasVisibleContent(result: DataResult<T>) =
        result.isSuccess && result.hasData && status.considerEvent(result)
}
