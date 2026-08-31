package br.com.arch.toolkit.livedata

import br.com.arch.toolkit.result.DataResult
import br.com.arch.toolkit.result.DataResultStatus.SUCCESS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class ResponseLiveDataCoverageTest {

    @Test
    fun `response live data exposes current values and projections`() {
        val source = MutableResponseLiveData(DataResult("value", null, SUCCESS))

        assertSame(source, source.liveData)
        assertEquals("value", source.data)
        assertEquals(SUCCESS, source.status)
        assertEquals(null, source.error)
        assertSame(source, source.scope(CoroutineScope(Dispatchers.Unconfined)))
        assertSame(source, source.transformDispatcher(Dispatchers.Unconfined))
        assertEquals(null, source.map { it.length }.value)
        assertEquals(null, source.mapError { it }.value)
        assertEquals(null, source.onErrorReturn { "fallback" }.value)
        assertEquals(null, source.transform { it }.value)
        source.onNext { }
        source.onError { }
    }
}
