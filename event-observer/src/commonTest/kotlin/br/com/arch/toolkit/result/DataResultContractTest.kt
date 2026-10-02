package br.com.arch.toolkit.result

import br.com.arch.toolkit.util.dataResultSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.ContinuationInterceptor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DataResultContractTest {

    @Test
    fun `execution settings do not change value equality or copying`() = runTest {
        val result = dataResultSuccess("value")
        val copy = result.copy()
        result.scope(this).transformDispatcher(StandardTestDispatcher(testScheduler))
        assertEquals(copy, result)
        assertEquals(copy.hashCode(), result.hashCode())
        assertEquals(copy, result.transform { it })
        assertEquals("value", result.component1())
    }

    @Test
    fun `explicit observation works on original copy and transformed value`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val cancelled = CoroutineScope(Job() + dispatcher).also { it.cancel() }
        val result = dataResultSuccess("value").scope(cancelled)
        val values = mutableListOf<String>()
        result.data { values.add("legacy") }
        listOf(result, result.copy(), result.transform { it.uppercase() }).forEach {
            it.unwrap(this, dispatcher) { data { value -> values.add(value) } }
        }
        testScheduler.advanceUntilIdle()
        assertEquals(listOf("value", "value", "VALUE"), values)
    }

    @Test
    fun `transformation uses the supplied dispatcher and callback returns to owner`() = runTest {
        val worker = StandardTestDispatcher(testScheduler, "worker")
        val owner = currentCoroutineContext()[ContinuationInterceptor]
        var observed: String? = null
        dataResultSuccess("value").unwrap(this, worker) {
            data(transformer = {
                assertEquals(worker, currentCoroutineContext()[ContinuationInterceptor])
                it.uppercase()
            }) {
                assertEquals(owner, currentCoroutineContext()[ContinuationInterceptor])
                observed = it
            }
        }
        testScheduler.advanceUntilIdle()
        assertEquals("VALUE", observed)
    }

    @Test
    fun `explicit observation respects owner cancellation`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val owner = CoroutineScope(Job() + dispatcher)
        var called = false
        dataResultSuccess("value").unwrap(owner, dispatcher) { data { called = true } }
        owner.cancel()
        testScheduler.advanceUntilIdle()
        assertFalse(called)
    }

    @Test
    fun `sequence shape matches finite cardinality`() {
        for (size in 0..3) {
            val result = dataResultSuccess((0 until size).asSequence())
            assertEquals(size == 0, result.isEmpty)
            assertEquals(size > 0, result.isNotEmpty)
            assertEquals(size == 1, result.hasOneItem)
            assertEquals(size > 1, result.hasManyItems)
        }
    }

    @Test
    fun `sequence checks inspect at most two values even for unbounded input`() {
        var reads = 0
        val sequence = generateSequence(0) { it + 1 }.onEach {
            reads++
            check(reads <= 2)
        }
        val result = dataResultSuccess(sequence)
        assertFalse(result.isEmpty)
        assertTrue(reads <= 1)
        reads = 0
        assertTrue(result.isNotEmpty)
        assertTrue(reads <= 1)
        reads = 0
        assertFalse(result.hasOneItem)
        assertEquals(2, reads)
        reads = 0
        assertTrue(result.hasManyItems)
        assertEquals(2, reads)
    }

    @Test
    fun `large finite sequence does not traverse its tail`() {
        var reads = 0
        val sequence = (0 until 1_000_000).asSequence().onEach {
            reads++
            check(reads <= 2)
        }
        val result = dataResultSuccess(sequence)
        assertTrue(result.hasManyItems)
        assertEquals(2, reads)
    }

    @Test
    fun `constrained sequence remains single use after inspection`() {
        val result = dataResultSuccess(sequenceOf(1, 2, 3).constrainOnce())
        assertTrue(result.isNotEmpty)
        assertFailsWith<IllegalStateException> { result.hasOneItem }
    }
}
