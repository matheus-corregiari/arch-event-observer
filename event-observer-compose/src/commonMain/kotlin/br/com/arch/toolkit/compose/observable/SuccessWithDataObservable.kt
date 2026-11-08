package br.com.arch.toolkit.compose.observable

import androidx.compose.runtime.Composable
import br.com.arch.toolkit.result.DataResult
import br.com.arch.toolkit.result.EventDataStatus

internal class SuccessWithDataObservable<T, R>(
    private val status: EventDataStatus,
    private val selectData: (T) -> R?,
    private val func: @Composable (R) -> Unit
) : ComposeObservable<T, R>() {
    @Composable
    override fun Content(result: DataResult<T>) {
        val data = result.data?.let(selectData) ?: return
        func(data)
    }

    override fun hasVisibleContent(result: DataResult<T>) =
        result.isSuccess && result.data?.let(selectData) != null && status.considerEvent(result)
}
