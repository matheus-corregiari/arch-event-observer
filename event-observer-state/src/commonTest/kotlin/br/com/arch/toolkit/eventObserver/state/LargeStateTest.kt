package br.com.arch.toolkit.eventObserver.state

import androidx.lifecycle.SavedStateHandle
import br.com.arch.toolkit.result.DataResultStatus
import br.com.arch.toolkit.util.dataResultSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Decoder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.measureTime

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class LargeStateTest {
    @Test
    fun injectedDispatcherSupportsPlainAndResultUpdatesAndProjection() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val scope = CoroutineScope(backgroundScope.coroutineContext + dispatcher)
        val plain = ViewModelState.Regular(
            "plain",
            Int.serializer(),
            SavedStateHandle(),
            scope,
            workerDispatcher = dispatcher
        )
        val result = ViewModelState.Result(
            "result",
            Int.serializer(),
            SavedStateHandle(),
            scope,
            workerDispatcher = dispatcher
        )
        plain.bindMapped(flowOf(1, 2)) { it * 2 }.join()
        result.loadMapped<Int>({ it * 3 }) { flowOf(dataResultSuccess(1)) }.join()
        assertEquals(4, plain.get())
        assertEquals(dataResultSuccess(3), result.flow().value)
        plain.setAsync(5).join()
        val projection = plain.selectAsync(0) { (it ?: 0) * 2 }
        runCurrent()
        assertEquals(10, projection.value)
    }

    @Test
    fun asyncResultWritesPreserveFailureAndRestoreSuccess() = runTest {
        val state = ViewModelState.Result(
            "result",
            NonNegativeSerializer,
            SavedStateHandle(),
            backgroundScope,
            workerDispatcher = UnconfinedTestDispatcher(testScheduler)
        )
        state.setAsync(5).join()
        assertEquals(dataResultSuccess(5), state.flow().value)
        state.setAsync(-1).join()
        assertEquals(5, state.get())
        assertEquals(DataResultStatus.ERROR, state.flow().value.status)
        state.setAsync(null).join()
        assertEquals(DataResultStatus.NONE, state.flow().value.status)
    }

    @Test
    fun heavyProjectionComputesOnceAndReadsStayCheap() = runTest {
        val source = MutableStateFlow(List(100_000) { it })
        var computations = 0
        val result = source.selectAsync(
            backgroundScope,
            0L,
            UnconfinedTestDispatcher(testScheduler)
        ) { items ->
            computations++
            var total = 0L
            items.forEachIndexed { index, value ->
                if (index % 256 == 0) yield()
                repeat(50) { total += (value.toLong() * it) % 97 }
            }
            total
        }
        runCurrent()
        val snapshot = result.value
        repeat(100) { assertEquals(snapshot, result.value) }
        assertEquals(1, computations)
        source.value = emptyList()
        runCurrent()
        assertEquals(0L, result.value)
        assertEquals(2, computations)
    }

    @Test
    fun repeatedReadsDoNotDecodeALargeListAgain() {
        val delegate = ListSerializer(Int.serializer())
        var decodes = 0
        val serializer = object : KSerializer<List<Int>> by delegate {
            override fun deserialize(decoder: Decoder): List<Int> {
                decodes++
                return delegate.deserialize(decoder)
            }
        }
        val state = SavedStateHandle().value("large", serializer = serializer)
        val items = List(100_000) { it }
        state.set(items)
        val elapsed = measureTime {
            repeat(50) {
                assertEquals(100_000, state.flow().value?.size)
                assertEquals(99_999, state.get()?.last())
            }
        }
        println("100000 items, 100 reads: $elapsed; decodes=$decodes")
        assertEquals(1, decodes, "Reads must reuse the decoded saved snapshot")
    }
}
