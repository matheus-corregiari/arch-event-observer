package br.com.arch.toolkit.eventObserver.state

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import br.com.arch.toolkit.eventObserver.state.StateValue.Companion.default
import br.com.arch.toolkit.eventObserver.state.StateValue.Companion.required
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@Serializable
internal data class Profile(val name: String, val tags: List<String?> = emptyList())

@Serializable
internal data class Screen(val users: List<Profile>, val counts: Map<String, Int>)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class StoredStateTest {
    private inline fun <reified T : Any> roundTrip(value: T) {
        val handle = SavedStateHandle()
        val state = handle.value<T>("value")
        state.set(value)
        assertEquals(value, state.get())
        assertEquals(value, state.flow().value)
        assertEquals(listOf(value), state.flow().replayCache)
        assertSame(state.flow(), state.flow())
        val restored = SavedStateHandle(mapOf("value" to handle.get<String>("value")))
        assertEquals(value, restored.value<T>("value").get())
    }

    @Test
    fun primitivesAndCollectionsRoundTrip() {
        roundTrip(true)
        roundTrip(1.toByte())
        roundTrip(2.toShort())
        roundTrip('a')
        roundTrip(42)
        roundTrip(42L)
        roundTrip(2.5)
        roundTrip(2.5f)
        roundTrip("hello")
        roundTrip(Profile("Ada", listOf("admin", null)))
        roundTrip(listOf(Profile("Ada"), Profile("Lin")))
        roundTrip(mapOf("first" to Profile("Ada")))
        roundTrip(mapOf("first" to Profile("Ada"), "second" to null))
        roundTrip(mapOf(1 to listOf(Profile("Lin"))))
        roundTrip(emptyList<Profile>())
        roundTrip(emptyMap<String, Profile>())
        roundTrip(Screen(listOf(Profile("Ada")), mapOf("total" to 1)))
    }

    @Test
    fun defaultsOnlyInitializeMissingKeysIncludingSavedNull() {
        val handle = SavedStateHandle()
        val state = handle.value<Int>("count", default = 7)
        assertEquals(7, state.get())
        state.set(null)
        assertTrue(handle.contains("count"))
        assertEquals("null", handle.get<String>("count"))
        assertNull(handle.value<Int>("count", default = 99).get())
        state.set(3)
        assertEquals(3, handle.value<Int>("count", default = 99).get())
    }

    @Test
    fun observersSurviveClearingAndSeeExternalWrites() = runTest {
        val handle = SavedStateHandle()
        val state = handle.value<Int>("count")
        val values = mutableListOf<Int?>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            state.flow().collect { values += it }
        }
        state.set(1)
        state.set(1)
        state.set(null)
        state.set(2)
        handle["count"] = "3"
        assertEquals(listOf(null, 1, null, 2, 3), values)
        assertEquals(3, state.get())
        assertEquals(3, state.flow().value)
        assertEquals(3, state.flow().replayCache.single())
    }

    @Test
    fun projectionsReadCurrentValueAndDeduplicate() = runTest {
        val state = SavedStateHandle().value<List<Int>>("items")
        val count = state.flow().select { it?.size ?: 0 }
        val observed = mutableListOf<Int>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            count.collect {
                observed +=
                    it
            }
        }
        state.set(listOf(1))
        state.set(listOf(2))
        state.set(listOf(2, 3))
        assertEquals(listOf(0, 1, 2), observed)
        assertEquals(2, count.value)
        assertEquals(listOf(2), count.replayCache)
    }

    @Test
    fun implicitAndExplicitPropertyKeysAreStable() {
        val handle = SavedStateHandle()
        val implicit = handle.value<String>()
        class Holder {
            var title by implicit
            var other by handle.value<Int>("explicit", default = 4)
        }
        val holder = Holder()
        holder.title = "screen"
        assertEquals("screen", holder.title)
        assertEquals("screen", implicit.flow().value)
        assertEquals(4, holder.other)
        holder.other = 5
        assertEquals("5", handle.get<String>("explicit"))
        assertTrue(handle.contains("title"))
    }

    @Test
    fun blankUnboundKeyAndCorruptRestoreFailExplicitly() {
        assertFailsWith<IllegalArgumentException> { SavedStateHandle().value<Int>().flow() }
        val handle = SavedStateHandle(mapOf("bad" to "{broken"))
        assertFailsWith<SerializationException> { handle.value<Profile>("bad").get() }
        assertEquals("{broken", handle.get<String>("bad"))
    }

    @Test
    fun explicitSerializerAndJsonConfigurationAreUsed() {
        val handle = SavedStateHandle(mapOf("profile" to "{\"name\":\"Ada\",\"extra\":true}"))
        val value = handle.value(
            "profile",
            json = Json {
                ignoreUnknownKeys = true
            },
            serializer = Profile.serializer()
        )
        assertEquals(Profile("Ada"), value.get())
        val encoded = handle.value("custom", serializer = NonNegativeSerializer)
        encoded.set(4)
        assertFailsWith<SerializationException> { encoded.set(-1) }
        assertEquals(4, encoded.get())
        assertEquals("4", handle.get<String>("custom"))
    }

    @Test
    fun structuredMapKeysRequireExplicitJsonConfiguration() {
        val handle = SavedStateHandle()
        val values = mapOf(Profile("key") to Profile("value"))
        val state = handle.value<Map<Profile, Profile>>(
            "map",
            json = Json {
                allowStructuredMapKeys =
                    true
            }
        )
        state.set(values)
        assertEquals(values, state.get())
        val restored = SavedStateHandle(mapOf("map" to handle.get<String>("map")))
        assertEquals(
            values,
            restored.value<Map<Profile, Profile>>(
                "map",
                json = Json {
                    allowStructuredMapKeys =
                        true
                }
            ).get()
        )
        val unsupported = handle.value<Map<Profile, Profile>>("other")
        assertFailsWith<SerializationException> { unsupported.set(values) }
        assertNull(unsupported.get())
    }

    @Test
    fun malformedExternalJsonIsNotSilentlyIgnored() {
        val handle = SavedStateHandle()
        val state = handle.value<Profile>("profile")
        state.set(Profile("Ada"))
        handle["profile"] = "broken"
        assertFailsWith<SerializationException> { state.flow().value }
        assertEquals("broken", handle.get<String>("profile"))
        state.set(Profile("Lin"))
        assertEquals(Profile("Lin"), state.get())
    }

    @Test
    fun restoredNullRawHandleValueIsAccepted() {
        assertNull(SavedStateHandle(mapOf("empty" to null)).value<Int>("empty", default = 1).get())
    }

    @Test
    fun fallbackDelegatesRemainConsistentWithoutPersistingFallback() {
        val handle = SavedStateHandle()
        val required = handle.value<Int>().required { 10 }
        val optional = handle.value<Int>().default { 20 }
        class Model {
            var count by required
            var optionalCount by optional
            var absent by handle.value<Int>().default { null }
        }
        val model = Model()
        assertEquals(10, model.count)
        assertEquals(10, required.flow().value)
        assertSame(required.flow(), required.flow())
        assertEquals("null", handle.get<String>("count"))
        model.count = 5
        assertEquals(5, model.count)
        assertEquals(5, required.flow().value)
        assertEquals(20, model.optionalCount)
        assertEquals(20, optional.flow().value)
        model.optionalCount = 4
        assertEquals(4, model.optionalCount)
        assertEquals(4, optional.flow().value)
        model.optionalCount = null
        assertEquals(20, optional.flow().value)
        assertNull(model.absent)
    }

    @Test
    fun viewModelDelegatesRestoreWithoutKoinAndKeepIdentity() = runTest {
        val handle = SavedStateHandle(mapOf("counter" to "7", "response" to "9", "named" to "11"))
        class Model(scope: CoroutineScope) : ViewModel(scope) {
            val counter by handle.saveState<Int>(default = 1)
            val response by handle.saveResponseState<Int>(default = 2)
            val explicit by handle.saveState<Int>("named")
            val explicitResponse by handle.saveResponseState<Int>("response")
        }
        val model = Model(backgroundScope)
        assertEquals(7, model.counter.get())
        assertSame(model.counter, model.counter)
        assertEquals(9, model.response.flow().value.data)
        assertSame(model.response, model.response)
        assertEquals(11, model.explicit.get())
        assertEquals(9, model.explicitResponse.get())
        assertNotSame(model.counter, Model(backgroundScope).counter)
    }
}

internal object NonNegativeSerializer : KSerializer<Int> {
    override val descriptor = PrimitiveSerialDescriptor("NonNegative", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: Int) {
        if (value < 0) throw SerializationException("Negative value")
        encoder.encodeInt(value)
    }

    override fun deserialize(decoder: Decoder): Int = decoder.decodeInt()
}
