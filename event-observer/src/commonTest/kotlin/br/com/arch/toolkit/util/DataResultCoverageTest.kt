package br.com.arch.toolkit.util

import br.com.arch.toolkit.result.DataResultStatus.ERROR
import br.com.arch.toolkit.result.DataResultStatus.LOADING
import br.com.arch.toolkit.result.DataResultStatus.NONE
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DataResultCoverageTest {

    @Test
    fun `result factories and nullable merges preserve status and data`() {
        val loading = dataResultLoading("cached")
        val error = dataResultError<String>(IllegalStateException("failure"))
        val none = dataResultNone<String>()

        assertEquals(LOADING, loading.status)
        assertEquals("cached", loading.data)
        assertEquals(ERROR, error.status)
        assertEquals(NONE, none.status)
        val empty: br.com.arch.toolkit.result.DataResult<String>? = null
        assertNull(empty.merge<String, String>(null).data)
        assertEquals(ERROR, none.merge(error).status)
    }
}
