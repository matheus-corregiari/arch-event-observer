package br.com.arch.toolkit.result

import br.com.arch.toolkit.util.dataResultError
import br.com.arch.toolkit.util.dataResultLoading
import br.com.arch.toolkit.util.dataResultSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
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
        result.unwrap(this) { data { } }
        assertEquals(copy, result)
        assertEquals(copy.hashCode(), result.hashCode())
        assertEquals(copy, result.transform { it })
        assertEquals("value", result.component1())
    }

    @Test
    fun `explicit observation works on original copy and transformed value`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val cancelled = CoroutineScope(Job() + dispatcher).also { it.cancel() }
        val result = dataResultSuccess("value")
        val values = mutableListOf<String>()
        result.data(scope = cancelled) { values.add("cancelled") }
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
    @OptIn(ExperimentalCoroutinesApi::class)
    fun `omitted scope uses Main without storing settings on the result`() = runTest {
        val main = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(main)
        try {
            val result = dataResultSuccess("value")
            var observed: String? = null
            result.data {
                assertEquals(Dispatchers.Main, currentCoroutineContext()[ContinuationInterceptor])
                observed = it
            }
            testScheduler.advanceUntilIdle()
            assertEquals("value", observed)
            assertEquals(result, result.copy())
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `direct helpers use independent scopes for the same value`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val cancelled = CoroutineScope(Job() + dispatcher).also { it.cancel() }
        val result = dataResultSuccess("value")
        val values = mutableListOf<String>()
        result.data(scope = cancelled) { values.add("cancelled") }
        result.data(scope = this) { values.add(it) }
        result.data(
            transformer = { it.uppercase() },
            scope = this,
            transformDispatcher = dispatcher
        ) { values.add(it) }
        testScheduler.advanceUntilIdle()
        assertEquals(listOf("value", "VALUE"), values)
        assertEquals(result, result.copy())
    }

    @Test
    fun `loading helpers execute on supplied owner scope`() = runTest {
        val calls = mutableListOf<String>()
        val loading = dataResultLoading<String>()
        loading.loading(scope = this) { calls.add("loading=$it") }
        loading.showLoading(scope = this) { calls.add("show") }
        dataResultSuccess("value").hideLoading(scope = this) { calls.add("hide") }
        testScheduler.advanceUntilIdle()
        assertEquals(listOf("loading=true", "show", "hide"), calls)
    }

    @Test
    fun `direct error transformations use worker then return to owner`() = runTest {
        val worker = StandardTestDispatcher(testScheduler, "worker")
        val owner = currentCoroutineContext()[ContinuationInterceptor]
        val failure = IllegalStateException("failure")
        val result = dataResultError<String>(failure)
        val values = mutableListOf<String>()
        val transformer: suspend (IllegalStateException) -> String = {
            assertEquals(worker, currentCoroutineContext()[ContinuationInterceptor])
            it.message.orEmpty()
        }
        val observer: suspend (String) -> Unit = {
            assertEquals(owner, currentCoroutineContext()[ContinuationInterceptor])
            values.add(it)
        }
        result.error<IllegalStateException, String>(
            transformer,
            scope = this,
            transformDispatcher = worker,
            func = observer
        )
        result.error(
            transformer = {
                assertEquals(worker, currentCoroutineContext()[ContinuationInterceptor])
                it.message.orEmpty()
            },
            scope = this,
            transformDispatcher = worker,
            func = observer
        )
        testScheduler.advanceUntilIdle()
        assertEquals(listOf("failure", "failure"), values)
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
