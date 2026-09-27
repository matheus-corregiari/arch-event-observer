package br.com.arch.toolkit.result

import br.com.arch.toolkit.result.DataResultStatus.ERROR
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveWrapperTest_errorWithClass {

    private val illegalStateException = IllegalStateException("Illegal State")
    private val illegalArgumentException = IllegalArgumentException("Illegal Argument")
    private val mockedIllegalStateObserver: (IllegalStateException) -> Unit = mockk()
    private val mockedIllegalArgumentObserver: (IllegalArgumentException) -> Unit = mockk()
    private val mockedFallbackObserver: (Throwable) -> Unit = mockk()

    init {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @BeforeTest
    fun init() {
        every { mockedIllegalStateObserver.invoke(any()) } returns Unit
        every { mockedIllegalArgumentObserver.invoke(any()) } returns Unit
        every { mockedFallbackObserver.invoke(any()) } returns Unit
    }

    @Test
    fun `matching exception class triggers callback`() = runTest {
        val result = DataResult<Any>(null, illegalStateException, ERROR)
        val wrapper = ObserveWrapper<Any>()
        wrapper.error(IllegalStateException::class) { error ->
            mockedIllegalStateObserver.invoke(error)
        }
        wrapper.attachTo(result)

        verify(exactly = 1) { mockedIllegalStateObserver.invoke(illegalStateException) }
    }

    @Test
    fun `non-matching exception class skips callback`() = runTest {
        val result = DataResult<Any>(null, illegalArgumentException, ERROR)
        val wrapper = ObserveWrapper<Any>()
        wrapper.error(IllegalStateException::class) { error ->
            mockedIllegalStateObserver.invoke(error)
        }
        wrapper.attachTo(result)

        verify(exactly = 0) { mockedIllegalStateObserver.invoke(any()) }
    }

    @Test
    fun `subclass matches parent exception class`() = runTest {
        val result = DataResult<Any>(null, illegalStateException, ERROR)
        val wrapper = ObserveWrapper<Any>()
        wrapper.error(Exception::class) { error ->
            mockedFallbackObserver.invoke(error)
        }
        wrapper.attachTo(result)

        verify(exactly = 1) { mockedFallbackObserver.invoke(illegalStateException) }
    }

    @Test
    fun `null error in error state skips class filtered callback`() = runTest {
        val result = DataResult<Any>(null, null, ERROR)
        val wrapper = ObserveWrapper<Any>()
        wrapper.error(IllegalStateException::class) { error ->
            mockedIllegalStateObserver.invoke(error)
        }
        wrapper.attachTo(result)

        verify(exactly = 0) { mockedIllegalStateObserver.invoke(any()) }
    }

    @Test
    fun `multiple typed error handlers and fallback execute correctly`() = runTest {
        val result = DataResult<Any>(null, illegalStateException, ERROR)
        val wrapper = ObserveWrapper<Any>()
        wrapper.error(IllegalArgumentException::class) { error ->
            mockedIllegalArgumentObserver.invoke(error)
        }
        wrapper.error(IllegalStateException::class) { error ->
            mockedIllegalStateObserver.invoke(error)
        }
        wrapper.error { error ->
            mockedFallbackObserver.invoke(error)
        }
        wrapper.attachTo(result)

        verify(exactly = 0) { mockedIllegalArgumentObserver.invoke(any()) }
        verify(exactly = 1) { mockedIllegalStateObserver.invoke(illegalStateException) }
        verify(exactly = 1) { mockedFallbackObserver.invoke(illegalStateException) }
    }

    @Test
    fun `transformer with exception class filter works correctly`() = runTest {
        val result = DataResult<Any>(null, illegalStateException, ERROR)
        val wrapper = ObserveWrapper<Any>()
        val mockedTransformedObserver: (String) -> Unit = mockk()
        every { mockedTransformedObserver.invoke(any()) } returns Unit

        wrapper.error(
            clazz = IllegalStateException::class,
            transformer = { it.message ?: "default" },
            observer = mockedTransformedObserver
        )
        wrapper.attachTo(result)

        verify(exactly = 1) { mockedTransformedObserver.invoke("Illegal State") }
    }

    @Test
    fun `DataResult error with class filter delegates correctly`() = runTest {
        val result = DataResult<Any>(null, illegalStateException, ERROR)
        result.error(IllegalStateException::class) { error ->
            mockedIllegalStateObserver.invoke(error)
        }

        verify(exactly = 1) { mockedIllegalStateObserver.invoke(illegalStateException) }
    }
}
