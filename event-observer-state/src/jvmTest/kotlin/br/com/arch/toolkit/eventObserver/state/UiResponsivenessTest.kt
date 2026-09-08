package br.com.arch.toolkit.eventObserver.state

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.measureTime

/** Real threads: virtual coroutine time cannot detect a blocked UI executor. */
class UiResponsivenessTest {
    @Test
    fun replacementDoesNotWaitForAnObsoleteSerializerOrCommitItsResult() = runBlocking {
        val ui = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        val scope = CoroutineScope(SupervisorJob() + ui)
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val delegate = Int.serializer()
        val serializer = object : KSerializer<Int> by delegate {
            override fun serialize(encoder: Encoder, value: Int) {
                if (value == 1) {
                    started.countDown()
                    check(release.await(5, TimeUnit.SECONDS)) { "Old serializer timed out" }
                }
                delegate.serialize(encoder, value)
            }
        }
        try {
            val state = withContext(ui) {
                ViewModelState.Regular("value", serializer, SavedStateHandle(), scope)
            }
            val old = withContext(ui) { state.setAsync(1) }
            assertTrue(started.await(5, TimeUnit.SECONDS))
            withTimeout(5_000) { withContext(ui) { state.setAsync(2) }.join() }
            assertEquals(2, withContext(ui) { state.get() })
            release.countDown()
            old.join()
            assertTrue(old.isCancelled)
            assertEquals(2, withContext(ui) { state.get() })
        } finally {
            release.countDown()
            scope.cancel()
            ui.close()
        }
    }

    @Test
    fun largeAsyncWriteEncodesAndDecodesOffUiAndReadsReuseSnapshot() = runBlocking {
        val ui = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        val scope = CoroutineScope(SupervisorJob() + ui)
        val codecThreads = ConcurrentLinkedQueue<Thread>()
        val delegate = ListSerializer(Int.serializer())
        val serializer = object : KSerializer<List<Int>> by delegate {
            override fun serialize(encoder: Encoder, value: List<Int>) {
                codecThreads += Thread.currentThread()
                delegate.serialize(encoder, value)
            }
            override fun deserialize(decoder: Decoder): List<Int> {
                codecThreads += Thread.currentThread()
                return delegate.deserialize(decoder)
            }
        }
        try {
            val uiThread = withContext(ui) { Thread.currentThread() }
            val state = withContext(ui) {
                ViewModelState.Regular("large", serializer, SavedStateHandle(), scope)
            }
            for (size in listOf(100_000, 1_000_000)) {
                codecThreads.clear()
                val items = List(size) { it }
                val elapsed = measureTime {
                    withContext(ui) { state.setAsync(items) }.join()
                }
                val reads = measureTime {
                    withContext(ui) {
                        repeat(100) { assertEquals(size, state.get()?.size) }
                    }
                }
                assertEquals(2, codecThreads.size)
                assertTrue(codecThreads.none { it === uiThread }, "Codec work ran on UI")
                println("$size items: async save=$elapsed; 100 UI reads=$reads")
            }
        } finally {
            scope.cancel()
            ui.close()
        }
    }

    @Test
    fun uiCanRunWhileANonSuspendingTransformIsBusy() = runBlocking {
        val ui = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        val scope = CoroutineScope(SupervisorJob() + ui)
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val heartbeat = CountDownLatch(1)
        try {
            val state = withContext(ui) {
                ViewModelState.Regular("value", Int.serializer(), SavedStateHandle(), scope)
            }
            val operation = withContext(ui) {
                state.bindMapped(flowOf(1)) {
                    started.countDown()
                    check(release.await(5, TimeUnit.SECONDS)) { "Transform timed out" }
                    it
                }
            }
            assertTrue(started.await(5, TimeUnit.SECONDS))
            ui.executor.execute { heartbeat.countDown() }
            val responsive = heartbeat.await(1, TimeUnit.SECONDS)
            release.countDown()
            operation.join()
            assertTrue(responsive, "A busy transformation blocked the UI executor")
        } finally {
            release.countDown()
            scope.cancel()
            ui.close()
        }
    }
}
