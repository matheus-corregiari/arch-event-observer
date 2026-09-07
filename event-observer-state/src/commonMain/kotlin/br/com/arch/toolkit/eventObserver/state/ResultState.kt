package br.com.arch.toolkit.eventObserver.state

import br.com.arch.toolkit.result.DataResult
import br.com.arch.toolkit.result.DataResultStatus
import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalForInheritanceCoroutinesApi::class)
internal class ResultState<T>(
    private val data: StateFlow<T?>,
    private val result: StateFlow<DataResult<T>>
) : StateFlow<DataResult<T>> {
    override val value: DataResult<T> get() = snapshot()
    override val replayCache: List<DataResult<T>> get() = listOf(value)

    private fun snapshot(): DataResult<T> {
        val current = result.value
        val payload = data.value
        val status = if (current.status == DataResultStatus.NONE &&
            current.data != payload &&
            payload != null
        ) {
            DataResultStatus.SUCCESS
        } else {
            current.status
        }
        return current.copy(data = payload, status = status)
    }

    override suspend fun collect(collector: FlowCollector<DataResult<T>>): Nothing {
        combine(data, result) { _, _ -> snapshot() }.distinctUntilChanged().collect(collector)
        error("StateFlow collection unexpectedly completed")
    }
}
