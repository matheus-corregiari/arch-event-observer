package br.com.arch.toolkit.compose

import br.com.arch.toolkit.result.DataResult
import br.com.arch.toolkit.result.DataResultStatus.ERROR
import br.com.arch.toolkit.result.DataResultStatus.LOADING
import br.com.arch.toolkit.result.DataResultStatus.SUCCESS
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class DataResultAnimationKeyTest {

    @Test
    fun `payload changes with same structural state keep animation key`() {
        val first = DataResult("First", null, SUCCESS)
        val second = DataResult("Second", null, SUCCESS)

        assertEquals(first.animationContentKey(), second.animationContentKey())
    }

    @Test
    fun `status changes update animation key`() {
        val loading = DataResult("Data", null, LOADING)
        val success = DataResult("Data", null, SUCCESS)

        assertNotEquals(loading.animationContentKey(), success.animationContentKey())
    }

    @Test
    fun `data presence changes update animation key`() {
        val withoutData = DataResult<String>(null, null, SUCCESS)
        val withData = DataResult("Data", null, SUCCESS)

        assertNotEquals(withoutData.animationContentKey(), withData.animationContentKey())
    }

    @Test
    fun `error presence changes update animation key`() {
        val withoutThrowable = DataResult<String>(null, null, ERROR)
        val withThrowable = DataResult<String>(null, IllegalStateException("Error"), ERROR)

        assertNotEquals(withoutThrowable.animationContentKey(), withThrowable.animationContentKey())
    }

    @Test
    fun `collection payload mutations do not restart animation`() {
        val single = DataResult(listOf("One"), null, SUCCESS)
        val many = DataResult(listOf("One", "Two"), null, SUCCESS)

        assertEquals(single.animationContentKey(), many.animationContentKey())
    }
}
