package br.com.arch.toolkit.util

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CoverageBoostTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Test
    fun combineNotNull_emitsInitialAndUpdatedPairs() {
        val left = MutableLiveData(1)
        val right = MutableLiveData("one")
        val values = mutableListOf<Pair<Int, String>>()

        left.combineNotNull(right).observeForever { values += it }
        left.value = 2
        right.value = "two"

        assertTrue(values.contains(1 to "one"))
        assertTrue(values.contains(2 to "two"))
    }

    @Test
    fun combineNotNull_ignoresNullUntilBothSourcesHaveValues() {
        val left = MutableLiveData<Int>()
        val right = MutableLiveData<String>()
        val values = mutableListOf<Pair<Int, String>>()

        left.combineNotNull(right).observeForever { values += it }
        left.value = 7
        assertTrue(values.isEmpty())

        right.value = "ready"

        assertEquals(listOf(7 to "ready"), values)
    }

    @Test
    fun chainNotNullWith_emitsOnlyWhenConditionAndSourcesAreValid() {
        val source = MutableLiveData(2)
        val chained = MutableLiveData("value")
        val values = mutableListOf<Pair<Int, String>>()

        source.chainNotNullWith({ chained }) { it > 1 }.observeForever { values += it }

        assertEquals(listOf(2 to "value"), values)

        source.value = 0
        assertEquals(1, values.size)
    }

    @Test
    fun chainWith_handlesConditionFailureAndOtherFailure() {
        val source = MutableLiveData(1)
        val values = mutableListOf<Pair<Int?, String?>>()

        source.chainWith(
            other = { _: Int? -> MutableLiveData("unused") },
            condition = { _: Int? -> false }
        ).observeForever { values += it }

        source.value = 2

        assertTrue(values.isEmpty())
    }

    @Test
    fun combine_nullableEmitsWhenOnlyOneSourceChanges() {
        val left = MutableLiveData<Int>()
        val right = MutableLiveData<String>()
        val values = mutableListOf<Pair<Int?, String?>>()

        left.combine(Dispatchers.Main.immediate, right).observeForever { values += it }
        left.value = 3
        right.value = "three"

        assertTrue(values.any { it == (3 to null) })
        assertTrue(values.any { it == (3 to "three") })
    }
}
