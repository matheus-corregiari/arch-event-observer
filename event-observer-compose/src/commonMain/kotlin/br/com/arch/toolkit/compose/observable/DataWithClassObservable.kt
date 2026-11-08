package br.com.arch.toolkit.compose.observable

import androidx.compose.runtime.Composable
import br.com.arch.toolkit.result.DataResult
import br.com.arch.toolkit.result.DataResultStatus
import br.com.arch.toolkit.result.EventDataStatus
import kotlin.reflect.KClass

internal class DataWithClassObservable<T, R : Any>(
    private val clazz: KClass<R>,
    private val dataStatus: EventDataStatus = EventDataStatus.DoesNotMatter,
    private val statuses: Set<DataResultStatus>? = null,
    private val func: @Composable (R, DataResultStatus, Throwable?) -> Unit
) : ComposeObservable<T, R>() {
    @Suppress("UNCHECKED_CAST")
    @Composable
    override fun Content(result: DataResult<T>) {
        val (data, error, status) = result
        if (clazz.isInstance(data)) func(data as R, status, error)
    }

    override fun hasVisibleContent(result: DataResult<T>) =
        !result.isNone &&
            (statuses == null || result.status in statuses) &&
            dataStatus.considerEvent(result) &&
            clazz.isInstance(result.data)
}
