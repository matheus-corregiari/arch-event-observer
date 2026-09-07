package br.com.arch.toolkit.eventObserver.state

import androidx.lifecycle.SavedStateHandle
import br.com.arch.toolkit.result.DataResult
import br.com.arch.toolkit.result.DataResultStatus
import br.com.arch.toolkit.util.dataResultError
import br.com.arch.toolkit.util.dataResultLoading
import br.com.arch.toolkit.util.dataResultNone
import br.com.arch.toolkit.util.dataResultSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.serializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class OperationsTest {
    @Test
    fun plainOneShotAndContinuousStreamsKeepOneState() = runTest {
        val state = ViewModelState.Regular(
            "value",
            Int.serializer(),
            SavedStateHandle(),
            backgroundScope
        )
        val observed = mutableListOf<Int?>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            state.flow().collect {
                observed +=
                    it
            }
        }
        val first = state.bind(flowOf(1))
        first.join()
        assertTrue(first.isCompleted)
        assertEquals(1, state.get())
        val stream = MutableSharedFlow<Int>()
        val connected = state.bind(stream)
        runCurrent()
        stream.emit(2)
        stream.emit(3)
        runCurrent()
        assertFalse(connected.isCompleted)
        state.cancel()
        assertTrue(connected.isCancelled)
        assertEquals(listOf(null, 1, 2, 3), observed)
        state.invalidate()
        state.set(4)
        assertEquals(4, state.flow().value)
    }

    @Test
    fun mappingReductionAndCompoundProjections() = runTest {
        val state = ViewModelState.Regular(
            "screen",
            Screen.serializer(),
            SavedStateHandle(),
            backgroundScope
        )
        state.bindMapped(
            flowOf("Ada")
        ) { name -> Screen(listOf(Profile(name)), mapOf("total" to 1)) }.join()
        val users = state.select { it?.users.orEmpty() }
        val count = state.select { it?.counts?.get("total") ?: 0 }
        assertEquals("Ada", users.value.single().name)
        state.bindReducing(flowOf("Lin", "Grace")) { previous, name ->
            val next = previous!!.users + Profile(name)
            Screen(next, mapOf("total" to next.size))
        }.join()
        assertEquals(3, count.value)
        assertEquals(3, users.value.size)
        val encoded = SavedStateHandle()
        val restored = ViewModelState.Regular(
            "screen",
            Screen.serializer(),
            encoded,
            backgroundScope
        )
        restored.set(state.get())
        assertEquals(3, restored.select { it!!.users.size }.value)
    }

    @Test
    fun plainFailuresKeepOldDataAndReportToCallback() = runTest {
        val state = ViewModelState.Regular(
            "value",
            Int.serializer(),
            SavedStateHandle(),
            backgroundScope,
            default = 7
        )
        val failure = IllegalStateException("offline")
        var received: Throwable? = null
        state.bind(flow { throw failure }, onError = { received = it }).join()
        assertSame(failure, received)
        assertEquals(7, state.get())
        state.bindMapped(flowOf("invalid"), onError = { received = it }) { it.toInt() }.join()
        assertTrue(received is NumberFormatException)
        assertEquals(7, state.get())
    }

    @Test
    fun defaultErrorPolicyPropagatesToScopeAndDistinctWritesRetainValue() = runTest {
        val errors = mutableListOf<Throwable>()
        val scope = CoroutineScope(
            SupervisorJob() + UnconfinedTestDispatcher(testScheduler) +
                CoroutineExceptionHandler { _, error -> errors += error }
        )
        try {
            val state = ViewModelState.Regular("value", Int.serializer(), SavedStateHandle(), scope)
            state.set(3, distinct = true)
            state.set(3, distinct = true)
            state.set(4, distinct = false)
            assertEquals(4, state.get())
            state.bind(flow { error("source") }).join()
            state.bindMapped(flowOf(1)) { error("mapping") }.join()
            state.bindReducing(flowOf(1)) { _, _ -> error("reduction") }.join()
            assertEquals(listOf("source", "mapping", "reduction"), errors.map { it.message })
            assertEquals(4, state.get())
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun cancellationIsNotReportedAsAnError() = runTest {
        val state = ViewModelState.Regular(
            "value",
            Int.serializer(),
            SavedStateHandle(),
            backgroundScope
        )
        var errorReported = false
        val job = state.bind(flow { throw CancellationException("stop") }, onError = {
            errorReported =
                true
        })
        job.join()
        assertTrue(job.isCancelled)
        assertFalse(errorReported)
    }

    @Test
    fun immediateErrorCallbackCanStartACancellableRetry() = runTest {
        val scope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher(testScheduler))
        val state = ViewModelState.Regular("value", Int.serializer(), SavedStateHandle(), scope)
        var retry: kotlinx.coroutines.Job? = null
        try {
            state.bind(flow { error("retry") }, onError = {
                retry = state.bind(
                    flow {
                        emit(2)
                        awaitCancellation()
                    }
                )
            }).join()
            assertEquals(2, state.get())
            assertFalse(retry!!.isCompleted)
            state.cancel()
            assertTrue(retry!!.isCancelled)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun newFilterRejectsLateTransformFromCancelledOperation() = runTest {
        val state = ViewModelState.Regular(
            "value",
            String.serializer(),
            SavedStateHandle(),
            backgroundScope
        )
        val old = state.bindMapped(flowOf("old")) {
            withContext(NonCancellable) { delay(100) }
            it
        }
        runCurrent()
        state.bind(flowOf("new")).join()
        old.join()
        assertEquals("new", state.get())
    }

    @Test
    fun resultsSurviveRefreshAndRestorePayloadAsSuccess() = runTest {
        val handle = SavedStateHandle()
        val state = ViewModelState.Result("result", Int.serializer(), handle, backgroundScope)
        val stream = state.flow()
        assertEquals(DataResultStatus.NONE, stream.value.status)
        state.load { flowOf(dataResultLoading(), dataResultSuccess(1)) }.join()
        assertEquals(dataResultSuccess(1), stream.value)
        var requests = 0
        repeat(2) {
            state.load {
                requests++
                flowOf(dataResultSuccess(requests + 1))
            }.join()
        }
        assertEquals(2, requests)
        assertSame(stream, state.flow())
        state.set(dataResultLoading())
        assertEquals(3, stream.value.data)
        assertEquals(DataResultStatus.LOADING, stream.value.status)
        val restoredHandle = SavedStateHandle(mapOf("result" to handle.get<String>("result")))
        val restored = ViewModelState.Result(
            "result",
            Int.serializer(),
            restoredHandle,
            backgroundScope,
            default = 99
        )
        assertEquals(dataResultSuccess(3), restored.flow().value)
        assertEquals(listOf(dataResultSuccess(3)), restored.flow().replayCache)
    }

    @Test
    fun resultErrorsRetainDataAndClearIsExplicit() = runTest {
        val state = ViewModelState.Result(
            "result",
            Int.serializer(),
            SavedStateHandle(),
            backgroundScope,
            default = 5
        )
        val failure = IllegalStateException("offline")
        state.set(dataResultError(failure))
        assertEquals(5, state.get())
        assertEquals(failure, state.flow().value.error)
        state.set(dataResultNone())
        assertEquals(5, state.get())
        assertEquals(DataResultStatus.NONE, state.flow().value.status)
        state.invalidate()
        assertNull(state.get())
        assertEquals(dataResultNone(), state.flow().value)
        state.set(6)
        assertEquals(dataResultSuccess(6), state.flow().value)
    }

    @Test
    fun resultProducerCollectorAndMapperErrorsBecomeErrorResults() = runTest {
        val state = ViewModelState.Result(
            "result",
            Int.serializer(),
            SavedStateHandle(),
            backgroundScope,
            default = 5
        )
        val failure = IllegalStateException("offline")
        state.load { throw failure }.join()
        assertEquals(dataResultError(failure, 5), state.flow().value)
        state.load {
            flow {
                emit(dataResultSuccess(6))
                throw failure
            }
        }.join()
        assertEquals(dataResultError(failure, 6), state.flow().value)
        state.loadMapped<String>({ it.toInt() }) { flowOf(dataResultSuccess("bad")) }.join()
        assertEquals(6, state.get())
        assertTrue(state.flow().value.error is NumberFormatException)
    }

    @Test
    fun resultMappingReductionAndContinuousSources() = runTest {
        val state = ViewModelState.Result(
            "result",
            serializer<List<Int>>(),
            SavedStateHandle(),
            backgroundScope
        )
        state.loadMapped<String>({ listOf(it.length) }) { flowOf(dataResultSuccess("abc")) }.join()
        state.loadReducing<Int>({ previous, next -> previous.orEmpty() + next }) {
            flowOf(dataResultLoading(), dataResultSuccess(4), dataResultSuccess(5))
        }.join()
        assertEquals(listOf(3, 4, 5), state.get())
        val stream = MutableSharedFlow<DataResult<List<Int>>>()
        val operation = state.load { stream }
        runCurrent()
        stream.emit(dataResultSuccess(listOf(9)))
        runCurrent()
        assertEquals(listOf(9), state.flow().value.data)
        assertFalse(operation.isCompleted)
        state.cancel()
    }

    @Test
    fun resultSubscribersSeeExternalHandleChangesAndNewSubscriptions() = runTest {
        val handle = SavedStateHandle()
        val state = ViewModelState.Result("result", Int.serializer(), handle, backgroundScope)
        val observed = mutableListOf<DataResult<Int>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            state.flow().collect {
                observed +=
                    it
            }
        }
        runCurrent()
        handle["result"] = "4"
        runCurrent()
        assertEquals(dataResultSuccess(4), state.flow().value)
        assertEquals(dataResultSuccess(4), observed.last())
        state.set(dataResultLoading())
        runCurrent()
        assertEquals(DataResultStatus.LOADING, observed.last().status)
        state.invalidate()
        runCurrent()
        state.set(7)
        runCurrent()
        assertEquals(dataResultSuccess(7), observed.last())
        val late = mutableListOf<DataResult<Int>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            state.flow().collect {
                late +=
                    it
            }
        }
        runCurrent()
        assertEquals(dataResultSuccess(7), late.single())
    }

    @Test
    fun resultReplacementAndCancellationDoNotProduceErrors() = runTest {
        val state = ViewModelState.Result(
            "result",
            Int.serializer(),
            SavedStateHandle(),
            backgroundScope
        )
        val old = state.loadMapped<Int>({
            withContext(NonCancellable) { delay(100) }
            it
        }) {
            flowOf(dataResultSuccess(1))
        }
        runCurrent()
        state.load { flowOf(dataResultSuccess(2)) }.join()
        old.join()
        assertEquals(dataResultSuccess(2), state.flow().value)
        val cancelled = state.load { throw CancellationException("stop") }
        cancelled.join()
        assertTrue(cancelled.isCancelled)
        assertEquals(dataResultSuccess(2), state.flow().value)
    }

    @Test
    fun serializationFailureIsSynchronousAndPreservesState() = runTest {
        val state = ViewModelState.Result(
            "result",
            NonNegativeSerializer,
            SavedStateHandle(),
            backgroundScope,
            default = 5
        )
        assertFailsWith<kotlinx.serialization.SerializationException> { state.set(-1) }
        assertEquals(dataResultSuccess(5), state.flow().value)
        state.load { flowOf(dataResultSuccess(-1)) }.join()
        assertEquals(5, state.get())
        assertEquals(DataResultStatus.ERROR, state.flow().value.status)
    }
}
