package br.com.arch.toolkit.result

import br.com.arch.toolkit.result.DataResultStatus.ERROR
import br.com.arch.toolkit.result.EventDataStatus.WithData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveWrapperTest_errorWithClass {

    private val illegalStateException = IllegalStateException("Illegal State")
    private val illegalArgumentException = IllegalArgumentException("Illegal Argument")

    @Test
    fun `matching exception class triggers callback`() = runTest {
        var received: IllegalStateException? = null
        val wrapper = ObserveWrapper<Any>().scope(this)
            .transformDispatcher(StandardTestDispatcher(testScheduler))

        wrapper.error(IllegalStateException::class) { error -> received = error }
        wrapper.attachTo(DataResult(null, illegalStateException, ERROR))

        advanceUntilIdle()
        assertSame(illegalStateException, received)
    }

    @Test
    fun `non-matching exception class skips callback`() = runTest {
        var calls = 0
        val wrapper = ObserveWrapper<Any>().scope(this)
            .transformDispatcher(StandardTestDispatcher(testScheduler))

        wrapper.error(IllegalStateException::class) { _: IllegalStateException -> calls++ }
        wrapper.attachTo(DataResult(null, illegalArgumentException, ERROR))

        advanceUntilIdle()
        assertEquals(0, calls)
    }

    @Test
    fun `subclass matches parent exception class`() = runTest {
        var received: Exception? = null
        val wrapper = ObserveWrapper<Any>().scope(this)
            .transformDispatcher(StandardTestDispatcher(testScheduler))

        wrapper.error(Exception::class) { error -> received = error }
        wrapper.attachTo(DataResult(null, illegalStateException, ERROR))

        advanceUntilIdle()
        assertSame(illegalStateException, received)
    }

    @Test
    fun `null error in error state skips class filtered callback`() = runTest {
        var calls = 0
        val wrapper = ObserveWrapper<Any>().scope(this)
            .transformDispatcher(StandardTestDispatcher(testScheduler))

        wrapper.error(IllegalStateException::class) { _: IllegalStateException -> calls++ }
        wrapper.attachTo(DataResult(null, null, ERROR))

        advanceUntilIdle()
        assertEquals(0, calls)
    }

    @Test
    fun `typed and untyped error handlers are additive`() = runTest {
        var typedCalls = 0
        var fallbackCalls = 0
        val wrapper = ObserveWrapper<Any>().scope(this)
            .transformDispatcher(StandardTestDispatcher(testScheduler))

        wrapper.error<IllegalStateException> { _: IllegalStateException -> typedCalls++ }
        wrapper.error { _: Throwable -> fallbackCalls++ }
        wrapper.attachTo(DataResult(null, illegalStateException, ERROR))

        advanceUntilIdle()
        assertEquals(1, typedCalls)
        assertEquals(1, fallbackCalls)
    }

    @Test
    fun `reified error handler filters by exception type`() = runTest {
        var received: IllegalStateException? = null
        val wrapper = ObserveWrapper<Any>().scope(this)
            .transformDispatcher(StandardTestDispatcher(testScheduler))

        wrapper.error<IllegalStateException> { error -> received = error }
        wrapper.attachTo(DataResult(null, illegalStateException, ERROR))

        advanceUntilIdle()
        assertSame(illegalStateException, received)
    }

    @Test
    fun `typed error handler without argument executes only for matching type`() = runTest {
        var calls = 0
        val wrapper = ObserveWrapper<Any>().scope(this)
            .transformDispatcher(StandardTestDispatcher(testScheduler))

        wrapper.error(
            clazz = IllegalStateException::class,
            observer = { -> calls++ }
        )
        wrapper.attachTo(DataResult(null, illegalStateException, ERROR))

        advanceUntilIdle()
        assertEquals(1, calls)
    }

    @Test
    fun `transformer with exception class filter works correctly`() = runTest {
        var received: String? = null
        val wrapper = ObserveWrapper<Any>().scope(this)
            .transformDispatcher(StandardTestDispatcher(testScheduler))

        wrapper.error<IllegalStateException, String>(
            transformer = { it.message ?: "default" },
            observer = { error -> received = error }
        )
        wrapper.attachTo(DataResult(null, illegalStateException, ERROR))

        advanceUntilIdle()
        assertEquals("Illegal State", received)
    }

    @Test
    fun `non-matching error does not consume single typed handler`() = runTest {
        var calls = 0
        val wrapper = ObserveWrapper<Any>()

        wrapper.error<IllegalStateException>(single = true) { _: IllegalStateException -> calls++ }
        wrapper.handleResult(DataResult(null, illegalArgumentException, ERROR))
        wrapper.handleResult(DataResult(null, illegalStateException, ERROR))
        wrapper.handleResult(DataResult(null, illegalStateException, ERROR))

        advanceUntilIdle()
        assertEquals(1, calls)
    }

    @Test
    fun `typed error handler respects data status`() = runTest {
        var calls = 0
        val wrapper = ObserveWrapper<Any>()

        wrapper.error<IllegalStateException>(
            dataStatus = WithData
        ) { _: IllegalStateException -> calls++ }

        wrapper.handleResult(DataResult(null, illegalStateException, ERROR))
        wrapper.handleResult(DataResult(Any(), illegalStateException, ERROR))

        advanceUntilIdle()
        assertEquals(1, calls)
    }

    @Test
    fun `DataResult reified error helper delegates correctly`() = runTest {
        var received: IllegalStateException? = null
        val result = DataResult<Any>(null, illegalStateException, ERROR)

        result.error<IllegalStateException>(scope = this) { error -> received = error }

        advanceUntilIdle()
        assertSame(illegalStateException, received)
    }

    @Test
    fun `reified no-argument handler skips mismatches and handles matching errors`() = runTest {
        var calls = 0
        val wrapper = ObserveWrapper<Any>()
        val observer: suspend () -> Unit = { calls++ }

        wrapper.error<IllegalStateException>(observer = observer)
        wrapper.handleResult(DataResult(null, illegalArgumentException, ERROR))
        advanceUntilIdle()
        assertEquals(0, calls)
        wrapper.handleResult(DataResult(null, illegalStateException, ERROR))
        advanceUntilIdle()
        assertEquals(1, calls)
    }

    @Test
    fun `DataResult reified no-argument helper filters errors`() = runTest {
        var calls = 0
        val observer: suspend () -> Unit = { calls++ }

        DataResult<Any>(null, illegalArgumentException, ERROR)
            .error<IllegalStateException>(scope = this, func = observer)
        advanceUntilIdle()
        assertEquals(0, calls)
        DataResult<Any>(null, illegalStateException, ERROR)
            .error<IllegalStateException>(scope = this, func = observer)
        advanceUntilIdle()
        assertEquals(1, calls)
    }

    @Test
    fun `DataResult reified transformer skips mismatches before transforming`() = runTest {
        var transformations = 0
        var received: String? = null
        val transformer: suspend (IllegalStateException) -> String = {
            transformations++
            it.message.orEmpty()
        }

        DataResult<Any>(null, illegalArgumentException, ERROR)
            .error<IllegalStateException, String>(
                transformer,
                scope = this,
                transformDispatcher = StandardTestDispatcher(testScheduler)
            ) { error -> received = error }
        advanceUntilIdle()
        assertEquals(0, transformations)
        assertEquals(null, received)
        DataResult<Any>(null, illegalStateException, ERROR)
            .error<IllegalStateException, String>(
                transformer,
                scope = this,
                transformDispatcher = StandardTestDispatcher(testScheduler)
            ) { error -> received = error }
        advanceUntilIdle()
        assertEquals(1, transformations)
        assertEquals("Illegal State", received)
    }
}
