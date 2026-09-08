package br.com.arch.toolkit.eventObserver.state

import br.com.arch.toolkit.result.DataResult
import br.com.arch.toolkit.result.DataResultStatus
import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull

@OptIn(ExperimentalForInheritanceCoroutinesApi::class)
internal class ResultState<T>(
    private val data: StateFlow<T?>,
    private val result: StateFlow<DataResult<T>>
) : StateFlow<DataResult<T>> {
    private val updating = MutableStateFlow(false)
    private var beforeUpdate: DataResult<T>? = null
    override val value: DataResult<T> get() = beforeUpdate ?: snapshot()
    override val replayCache: List<DataResult<T>> get() = listOf(value)

    /** Publishes payload and transient status as one result, even with immediate collectors. */
    fun update(action: () -> Unit) {
        if (updating.value) {
            action()
            return
        }
        beforeUpdate = snapshot()
        updating.value = true
        try {
            action()
        } finally {
            beforeUpdate = null
            updating.value = false
        }
    }

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
        combine(data, result, updating) { _, _, _ ->
            if (updating.value) null else snapshot()
        }.filterNotNull().distinctUntilChanged().collect(collector)
        error("StateFlow collection unexpectedly completed")
    }
}
