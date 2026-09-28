package br.com.arch.toolkit.result

import br.com.arch.toolkit.result.DataResultStatus.ERROR
import br.com.arch.toolkit.result.EventDataStatus.WithData
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class ObserveWrapperTest_errorWithClass {

    private val illegalStateException = IllegalStateException("Illegal State")
    private val illegalArgumentException = IllegalArgumentException("Illegal Argument")

    @Test
    fun `matching exception class triggers callback`() = runTest {
        var received: IllegalStateException? = null
        val wrapper = ObserveWrapper<Any>().scope(this)

        wrapper.error(IllegalStateException::class) { received = it }
        wrapper.attachTo(DataResult(null, illegalStateException, ERROR))

        assertSame(illegalStateException, received)
    }

    @Test
    fun `non-matching exception class skips callback`() = runTest {
        var calls = 0
        val wrapper = ObserveWrapper<Any>().scope(this)

        wrapper.error(IllegalStateException::class) { _: IllegalStateException -> calls++ }
        wrapper.attachTo(DataResult(null, illegalArgumentException, ERROR))

        assertEquals(0, calls)
    }

    @Test
    fun `subclass matches parent exception class`() = runTest {
        var received: Exception? = null
        val wrapper = ObserveWrapper<Any>().scope(this)

        wrapper.error(Exception::class) { received = it }
        wrapper.attachTo(DataResult(null, illegalStateException, ERROR))

        assertSame(illegalStateException, received)
    }

    @Test
    fun `null error in error state skips class filtered callback`() = runTest {
        var calls = 0
        val wrapper = ObserveWrapper<Any>().scope(this)

        wrapper.error(IllegalStateException::class) { _: IllegalStateException -> calls++ }
        wrapper.attachTo(DataResult(null, null, ERROR))

        assertEquals(0, calls)
    }

    @Test
    fun `typed and untyped error handlers are additive`() = runTest {
        var typedCalls = 0
        var fallbackCalls = 0
        val wrapper = ObserveWrapper<Any>().scope(this)

        wrapper.error<IllegalStateException> { _: IllegalStateException -> typedCalls++ }
        wrapper.error { _: Throwable -> fallbackCalls++ }
        wrapper.attachTo(DataResult(null, illegalStateException, ERROR))

        assertEquals(1, typedCalls)
        assertEquals(1, fallbackCalls)
    }

    @Test
    fun `reified error handler filters by exception type`() = runTest {
        var received: IllegalStateException? = null
        val wrapper = ObserveWrapper<Any>().scope(this)

        wrapper.error<IllegalStateException> { received = it }
        wrapper.attachTo(DataResult(null, illegalStateException, ERROR))

        assertSame(illegalStateException, received)
    }

    @Test
    fun `typed error handler without argument executes only for matching type`() = runTest {
        var calls = 0
        val wrapper = ObserveWrapper<Any>().scope(this)

        wrapper.error(
            clazz = IllegalStateException::class,
            observer = { calls++ }
        )
        wrapper.attachTo(DataResult(null, illegalStateException, ERROR))

        assertEquals(1, calls)
    }

    @Test
    fun `transformer with exception class filter works correctly`() = runTest {
        var received: String? = null
        val wrapper = ObserveWrapper<Any>().scope(this)

        wrapper.error<IllegalStateException, String>(
            transformer = { it.message ?: "default" },
            observer = { received = it }
        )
        wrapper.attachTo(DataResult(null, illegalStateException, ERROR))

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

        assertEquals(1, calls)
    }

    @Test
    fun `DataResult reified error helper delegates correctly`() = runTest {
        var received: IllegalStateException? = null
        val result = DataResult<Any>(null, illegalStateException, ERROR).scope(this)

        result.error<IllegalStateException> { received = it }

        assertSame(illegalStateException, received)
    }
}
