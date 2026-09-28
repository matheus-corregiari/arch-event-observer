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
    fun `collection payload changes with same shape keep animation key`() {
        val first = DataResult(listOf("One", "Two"), null, SUCCESS)
        val second = DataResult(listOf("Three", "Four"), null, SUCCESS)

        assertEquals(first.animationContentKey(), second.animationContentKey())
    }

    @Test
    fun `collection shape changes update animation key`() {
        val empty = DataResult(emptyList<String>(), null, SUCCESS)
        val single = DataResult(listOf("One"), null, SUCCESS)
        val many = DataResult(listOf("One", "Two"), null, SUCCESS)

        assertNotEquals(empty.animationContentKey(), single.animationContentKey())
        assertNotEquals(single.animationContentKey(), many.animationContentKey())
        assertNotEquals(empty.animationContentKey(), many.animationContentKey())
    }

    @Test
    fun `animation key does not consume sequence data`() {
        val sequence = sequenceOf("One").constrainOnce()
        val result = DataResult(sequence, null, SUCCESS)

        result.animationContentKey()

        assertEquals("One", sequence.first())
    }

}
