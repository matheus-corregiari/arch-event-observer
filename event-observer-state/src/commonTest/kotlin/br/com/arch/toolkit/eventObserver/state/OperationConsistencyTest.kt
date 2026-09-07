package br.com.arch.toolkit.eventObserver.state

import androidx.lifecycle.SavedStateHandle
import br.com.arch.toolkit.result.DataResult
import br.com.arch.toolkit.util.dataResultError
import br.com.arch.toolkit.util.dataResultSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.builtins.serializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class OperationConsistencyTest {
    @Test
    fun resultUpdateDoesNotExposeNewPayloadWithPreviousStatus() = runTest {
        val state = ViewModelState.Result(
            "result",
            Int.serializer(),
            SavedStateHandle(),
            backgroundScope
        )
        val observed = mutableListOf<DataResult<Int>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            state.flow().collect { observed += it }
        }
        runCurrent()
        state.set(dataResultSuccess(1))
        runCurrent()
        observed.clear()
        val expected = dataResultError(IllegalStateException("partial"), 2)
        state.set(expected)
        runCurrent()
        assertEquals(listOf(expected), observed)
    }

    @Test
    fun replacementStartedByCompletionHandlerIsTheLatestOperation() = runTest {
        val scope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher(testScheduler))
        val state = ViewModelState.Regular("value", Int.serializer(), SavedStateHandle(), scope)
        var latest: Job? = null
        try {
            val first = state.bind(flow { awaitCancellation() })
            first.invokeOnCompletion {
                latest = state.bind(flowOf(3))
            }
            val second = state.bind(
                flow {
                    emit(2)
                    awaitCancellation()
                }
            )
            latest!!.join()
            assertEquals(3, state.get())
            assertTrue(second.isCancelled)
        } finally {
            scope.cancel()
        }
    }
}
