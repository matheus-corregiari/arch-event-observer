package br.com.arch.toolkit.eventObserver.state

import androidx.lifecycle.SavedStateHandle
import br.com.arch.toolkit.util.dataResultLoading
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.lang.management.ManagementFactory
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.measureTime

class PublicationPerformanceTest {
    private class Payload(val values: List<Int>, val comparisons: ConcurrentLinkedQueue<Thread>) {
        override fun equals(other: Any?): Boolean {
            comparisons += Thread.currentThread()
            return other is Payload && values == other.values
        }
        override fun hashCode(): Int = values.hashCode()
    }

    @Test
    fun asyncPublicationAndStatusChangesNeverComparePayloadsOnOwner() = runBlocking {
        val ui = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        val scope = CoroutineScope(SupervisorJob() + ui)
        val comparisons = ConcurrentLinkedQueue<Thread>()
        val codecThreads = ConcurrentLinkedQueue<Thread>()
        val delegate = ListSerializer(Int.serializer())
        val serializer = object : KSerializer<Payload> {
            override val descriptor = delegate.descriptor
            override fun serialize(encoder: Encoder, value: Payload) {
                codecThreads += Thread.currentThread()
                delegate.serialize(encoder, value.values)
            }
            override fun deserialize(decoder: Decoder): Payload {
                codecThreads += Thread.currentThread()
                return Payload(delegate.deserialize(decoder), comparisons)
            }
        }
        try {
            val ownerThread = withContext(ui) { Thread.currentThread() }
            val handle = SavedStateHandle()
            val state = withContext(ui) { ViewModelState.Result("rows", serializer, handle, scope) }
            val subscribed = CountDownLatch(2)
            scope.launch { state.data.collect { subscribed.countDown() } }
            scope.launch { state.flow().collect { subscribed.countDown() } }
            assertTrue(subscribed.await(5, TimeUnit.SECONDS))
            val bean = ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean
            for (size in listOf(100, 1_000, 10_000)) {
                comparisons.clear()
                codecThreads.clear()
                val payload = Payload(List(size) { it }, comparisons)
                val allocatedBefore = bean.getThreadAllocatedBytes(ownerThread.threadId())
                val elapsed = measureTime { withContext(ui) { state.setAsync(payload) }.join() }
                val reads = measureTime { withContext(ui) { repeat(100) { state.get() } } }
                withContext(ui) { state.set(dataResultLoading()) }
                assertTrue(comparisons.none { it === ownerThread }, "Payload equality ran on owner")
                assertTrue(codecThreads.none { it === ownerThread }, "Codec ran on owner")
                assertEquals(2, codecThreads.size)
                val bytes =
                    withContext(ui) { handle.get<String>("rows")!!.encodeToByteArray().size }
                val allocated =
                    bean.getThreadAllocatedBytes(ownerThread.threadId()) - allocatedBefore
                println(
                    "rows=$size; jsonBytes=$bytes; async=$elapsed; reads100=$reads; ownerAllocatedBytes=$allocated"
                )
            }
        } finally {
            scope.cancel()
            ui.close()
        }
    }

    @Test
    fun asyncProjectionComparesItsOutputOnWorker() = runBlocking {
        val ui = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        val scope = CoroutineScope(SupervisorJob() + ui)
        val comparisons = ConcurrentLinkedQueue<Thread>()
        try {
            val ownerThread = withContext(ui) { Thread.currentThread() }
            val source = MutableStateFlow(1)
            val projection = source.selectAsync(scope, Payload(emptyList(), comparisons)) { value ->
                Payload(List(10_000) { value }, comparisons)
            }
            withTimeout(5_000) { projection.first { it.values.firstOrNull() == 1 } }
            withContext(ui) { source.value = 2 }
            withTimeout(5_000) { projection.first { it.values.firstOrNull() == 2 } }
            assertTrue(comparisons.isNotEmpty())
            assertTrue(comparisons.none { it === ownerThread }, "Projection equality ran on owner")
        } finally {
            scope.cancel()
            ui.close()
        }
    }
}
