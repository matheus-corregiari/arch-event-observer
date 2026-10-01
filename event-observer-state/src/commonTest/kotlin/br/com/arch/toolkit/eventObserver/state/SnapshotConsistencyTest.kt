package br.com.arch.toolkit.eventObserver.state

import androidx.lifecycle.SavedStateHandle
import br.com.arch.toolkit.result.DataResult
import br.com.arch.toolkit.util.dataResultError
import br.com.arch.toolkit.util.dataResultLoading
import br.com.arch.toolkit.util.dataResultNone
import br.com.arch.toolkit.util.dataResultSuccess
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SnapshotConsistencyTest {
    @Test
    fun clearingFromPayloadCollectorKeepsNoneStatus() = runTest {
        val handle = SavedStateHandle()
        val state = ViewModelState.Result("result", Int.serializer(), handle, backgroundScope)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            state.data.collect { if (it == 1) state.invalidate() }
        }
        state.set(1)
        assertEquals(dataResultNone(), state.flow().value)
        assertEquals("null", handle.get<String>("result"))
    }

    @Test
    fun replacementFromPayloadCollectorKeepsLatestStatus() = runTest {
        val state = ViewModelState.Result(
            "result",
            Int.serializer(),
            SavedStateHandle(),
            backgroundScope
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            state.data.collect { if (it == 1) state.set(2) }
        }
        state.set(dataResultError(IllegalStateException("old"), 1))
        assertEquals(dataResultSuccess(2), state.flow().value)
    }

    @Test
    fun asyncCommitCanBeClearedAndCancelledByPayloadCollector() = runTest {
        val state = ViewModelState.Result(
            "result",
            Int.serializer(),
            SavedStateHandle(),
            backgroundScope,
            workerDispatcher = UnconfinedTestDispatcher(testScheduler)
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            state.data.collect {
                if (it == 1) {
                    state.cancel()
                    state.invalidate()
                }
            }
        }
        state.load { flowOf(dataResultError(IllegalStateException("old"), 1)) }.join()
        assertEquals(dataResultNone(), state.flow().value)
    }

    @Test
    fun payloadCollectorCanStartLatestOperation() = runTest {
        val state = ViewModelState.Result(
            "result",
            Int.serializer(),
            SavedStateHandle(),
            backgroundScope,
            workerDispatcher = UnconfinedTestDispatcher(testScheduler)
        )
        var latest: Job? = null
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            state.data.collect {
                if (it == 1) latest = state.load { flowOf(dataResultSuccess(2)) }
            }
        }
        val old = state.load {
            flow {
                emit(dataResultLoading(1))
                awaitCancellation()
            }
        }
        runCurrent()
        latest!!.join()
        assertTrue(old.isCancelled)
        assertEquals(dataResultSuccess(2), state.flow().value)
    }

    @Test
    fun statusOnlyUpdatesDoNotEmitPayloadAgain() = runTest {
        val state = ViewModelState.Result(
            "result",
            Int.serializer(),
            SavedStateHandle(),
            backgroundScope,
            default = 1
        )
        val observed = mutableListOf<Int?>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            state.data.collect {
                observed +=
                    it
            }
        }
        val payload = state.get()
        state.set(dataResultLoading())
        state.set(dataResultError(IllegalStateException("error")))
        state.set(1)
        assertEquals(listOf<Int?>(1), observed)
        assertEquals(payload, state.get())
        assertSame(state.flow(), state.flow())
    }

    @Test
    fun synchronousWriteDuringPreparationIsRebased() = runTest {
        val state = ViewModelState.Regular(
            "value",
            Int.serializer(),
            SavedStateHandle(),
            backgroundScope,
            workerDispatcher = UnconfinedTestDispatcher(testScheduler)
        )
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val operation = state.bindMapped(flowOf(1)) {
            entered.complete(Unit)
            release.await()
            it
        }
        entered.await()
        state.set(2)
        release.complete(Unit)
        operation.join()
        assertEquals(1, state.get())
    }

    @Test
    fun resultAndDataAlwaysReadTheSameSnapshotDuringCallbacks() = runTest {
        val state = ViewModelState.Result(
            "result",
            Int.serializer(),
            SavedStateHandle(),
            backgroundScope
        )
        val seen = mutableListOf<DataResult<Int>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            state.data.collect {
                assertEquals(it, state.flow().value.data)
                seen += state.flow().value
            }
        }
        val failure = dataResultError(IllegalStateException("partial"), 1)
        state.set(failure)
        assertEquals(failure, seen.last())
    }

    @Test
    fun preparedSnapshotRejectsStaleBaseAndCanBeRebased() {
        val handle = SavedStateHandle()
        val stored = StoredState(handle, "value", Int.serializer(), Json, null)
        val prepared = stored.prepare(1)
        stored.set(2)
        assertFailsWith<IllegalStateException> { stored.commit(prepared) }
        assertEquals(2, stored.flow.value)
        assertEquals("2", handle.get<String>("value"))
        stored.commit(stored.rebase(prepared, stored.current))
        assertEquals(1, stored.flow.value)
    }

    @Test
    fun equalAsyncSnapshotsAndIdenticalResultsStayDistinct() = runTest {
        val state = ViewModelState.Result(
            "result",
            Int.serializer(),
            SavedStateHandle(),
            backgroundScope,
            workerDispatcher = UnconfinedTestDispatcher(testScheduler)
        )
        val observed = mutableListOf<DataResult<Int>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            state.flow().collect { observed += it }
        }
        state.setAsync(1).join()
        state.setAsync(1).join()
        state.set(dataResultLoading())
        state.set(dataResultLoading())
        state.set(1, distinct = true)
        assertEquals(
            listOf(
                dataResultNone(),
                dataResultSuccess(1),
                dataResultLoading(1),
                dataResultSuccess(1)
            ),
            observed
        )
    }

    @Test
    fun equalAsyncWriteRecomparesAfterAnInterveningOwnerWrite() = runTest {
        lateinit var state: ViewModelState.Regular<Int>
        var intercept = false
        val codec = object : KSerializer<Int> by Int.serializer() {
            override fun serialize(encoder: Encoder, value: Int) {
                if (intercept) {
                    intercept = false
                    backgroundScope.launch { state.set(2) }
                }
                Int.serializer().serialize(encoder, value)
            }
        }
        val handle = SavedStateHandle()
        state = ViewModelState.Regular(
            "value",
            codec,
            handle,
            backgroundScope,
            workerDispatcher = StandardTestDispatcher(testScheduler)
        )
        state.set(1)
        val observed = mutableListOf<Int?>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            state.flow().collect { observed += it }
        }
        intercept = true
        state.setAsync(1).join()
        assertEquals(listOf<Int?>(1, 2, 1), observed)
        assertEquals(1, state.get())
        assertEquals("1", handle.get<String>("value"))
    }
}
