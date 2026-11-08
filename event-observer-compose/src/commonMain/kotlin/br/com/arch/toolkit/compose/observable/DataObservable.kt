package br.com.arch.toolkit.compose.observable

import androidx.compose.runtime.Composable
import br.com.arch.toolkit.result.DataResult
import br.com.arch.toolkit.result.DataResultStatus

internal class DataObservable<T, R>(
    private val selectData: (T) -> R?,
    private val func: @Composable (R, DataResultStatus, Throwable?) -> Unit
) : ComposeObservable<T, R>() {
    @Composable
    override fun Content(result: DataResult<T>) {
        val (data, error, status) = result
        val selectedData = data?.let(selectData) ?: return
        func(selectedData, status, error)
    }

    override fun hasVisibleContent(result: DataResult<T>) =
        result.isNone.not() && result.data?.let(selectData) != null
}
