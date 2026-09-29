package br.com.arch.toolkit.result

import br.com.arch.toolkit.exception.DataResultException
import br.com.arch.toolkit.util.dataResultSuccess
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveWrapperTypedFailureTest {
    @Test
    fun `mismatching typed handler does not swallow observer failure`() {
        val thrown = IllegalArgumentException("callback failed")
        var calls = 0
        val failure = assertFailsWith<DataResultException> {
            runTest {
                ObserveWrapper<String>().scope(this).apply {
                    data { throw thrown }
                    error<IllegalStateException> { _: IllegalStateException -> calls++ }
                }.attachTo(dataResultSuccess("data"))
                advanceUntilIdle()
            }
        }
        assertSame(thrown, failure.error)
        assertEquals(0, calls)
    }

    @Test
    fun `matching superclass handler receives observer failure`() = runTest {
        val thrown = IllegalArgumentException("callback failed")
        var received: Throwable? = null
        ObserveWrapper<String>().scope(this).apply {
            data { throw thrown }
            error<Exception> { error -> received = error }
        }.attachTo(dataResultSuccess("data"))
        advanceUntilIdle()
        assertSame(thrown, received)
    }

    @Test
    fun `untyped fallback receives failure rejected by typed handler`() = runTest {
        val thrown = IllegalArgumentException("callback failed")
        var received: Throwable? = null
        var typedCalls = 0
        ObserveWrapper<String>().scope(this).apply {
            data { throw thrown }
            error<IllegalStateException> { _: IllegalStateException -> typedCalls++ }
            error { error: Throwable -> received = error }
        }.attachTo(dataResultSuccess("data"))
        advanceUntilIdle()
        assertSame(thrown, received)
        assertEquals(0, typedCalls)
    }

    @Test
    fun `data-filtered error handler cannot swallow a failure without data`() {
        val thrown = IllegalArgumentException("callback failed")
        val failure = assertFailsWith<DataResultException> {
            runTest {
                ObserveWrapper<String>().scope(this).apply {
                    data { throw thrown }
                    error<Exception>(dataStatus = EventDataStatus.WithData) { _: Exception -> }
                }.attachTo(dataResultSuccess("data"))
                advanceUntilIdle()
            }
        }
        assertSame(thrown, failure.error)
    }
}
