package br.com.arch.toolkit.flow

import br.com.arch.toolkit.result.DataResultStatus.ERROR
import br.com.arch.toolkit.result.DataResultStatus.LOADING
import br.com.arch.toolkit.result.DataResultStatus.NONE
import br.com.arch.toolkit.result.DataResultStatus.SUCCESS
import br.com.arch.toolkit.util.dataResultSuccess
import br.com.arch.toolkit.util.valueOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class ResponseFlowCoverageTest {

    init {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @Test
    fun `mutable state flow custom emitters update every status`() = runTest {
        val flow = ResponseMutableStateFlow<Int>()

        flow.emitData(1)
        assertEquals(SUCCESS, flow.status)
        assertEquals(1, flow.data)
        flow.emitLoading(2)
        assertEquals(LOADING, flow.status)
        assertEquals(2, flow.data)
        flow.emitError(IllegalStateException("failure"), 3)
        assertEquals(ERROR, flow.status)
        assertEquals(3, flow.data)
        flow.emitNone()
        assertEquals(NONE, flow.status)
        assertNull(flow.data)
    }

    @Test
    fun `mutable state flow try emitters update every status`() {
        val flow = ResponseMutableStateFlow<Int>()

        flow.tryEmitSuccess()
        flow.tryEmitData(1)
        flow.tryEmitLoading(2)
        flow.tryEmitError(IllegalStateException("failure"), 3)
        flow.tryEmitNone()

        assertEquals(NONE, flow.status)
        assertNull(flow.data)
    }

    @Test
    fun `response flow factories map and convert values`() = runTest {
        val mapped = ResponseFlow(dataResultSuccess(2)).map { it * 2 }
        assertEquals(listOf(4), mapped.toList().map { it.data })

        val fromFlow = ResponseFlow.fromFlow(flowOf("a", "b"))
        assertEquals(listOf("a", "b"), fromFlow.toList().map { it.data })

        val transformed = ResponseFlow.fromFlow(flowOf(1, 2)) { it.toString() }
        assertEquals(listOf("1", "2"), transformed.toList().map { it.data })
    }

    @Test
    fun `state and shared factories preserve initial values`() = runTest {
        val source = ResponseMutableStateFlow(dataResultSuccess(7))
        val state = source.state(scope = backgroundScope)
        val shared = source.shared(scope = backgroundScope, replay = 1)

        assertEquals(7, state.data)
        assertEquals(0, shared.replayCache.size)
        assertEquals(7, source.valueOrNull()?.data)
    }

    @Test
    fun `shared cold flow stops after successful result`() = runTest {
        val source = ResponseSharedFlow.from(flowOf(dataResultSuccess(9)))
        val values = source.cold().toList()

        assertEquals(listOf(9), values.map { it.data })
    }

    @Test
    fun `flow factories transform values`() = runTest {
        val plainFlow = ResponseFlow.fromFlow(flowOf(3))
        val resultFlow = ResponseFlow.from(flowOf(dataResultSuccess(1))) { it + 1 }

        assertEquals(SUCCESS, plainFlow.toList().single().status)
        assertEquals(2, resultFlow.toList().single().data)
        assertNull(flowOf(1).valueOrNull())
    }

    @Test
    fun `empty and shared factory flows are constructible`() = runTest {
        assertEquals(0, ResponseFlow<Int>().toList().size)
        assertEquals(0, ResponseSharedFlow.fromFlow(flowOf(1), replay = 0).replayCache.size)
    }

    @Test
    fun `response flow constructors expose standard flow behavior`() {
        val result = dataResultSuccess(4)
        val emptyState = ResponseStateFlow<Int>()
        val emptyMutable = ResponseMutableStateFlow<Int>()
        val state = ResponseStateFlow(result)
        val mutable = ResponseMutableStateFlow(result)
        val shared = ResponseSharedFlow<Int>()

        assertEquals(NONE, emptyState.status)
        assertEquals(NONE, emptyMutable.status)
        assertEquals(result, state.value)
        assertEquals(result, mutable.value)
        assertNull(shared.valueOrNull())
        assertEquals(state.hashCode(), state.hashCode())
        assertEquals(state.toString(), state.toString())
    }
}
