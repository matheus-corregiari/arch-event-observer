package br.com.arch.toolkit.eventObserver.state

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.json.Json

/** A read-through projection: values always reflect the backing state, without another cache. */
@OptIn(ExperimentalForInheritanceCoroutinesApi::class)
internal class SelectedState<A, B>(
    private val source: StateFlow<A>,
    private val transform: (A) -> B
) : StateFlow<B> {
    override val value: B get() = transform(source.value)
    override val replayCache: List<B> get() = listOf(value)

    override suspend fun collect(collector: FlowCollector<B>): Nothing {
        source.map(transform).distinctUntilChanged().collect(collector)
        error("StateFlow collection unexpectedly completed")
    }
}

internal class StoredState<T : Any>(
    private val handle: SavedStateHandle,
    private val key: String,
    serializer: KSerializer<T>,
    private val json: Json,
    default: T?
) {
    private val nullableSerializer = serializer.nullable

    init {
        require(key.isNotBlank()) { "State key must not be blank" }
        if (!handle.contains(key)) set(default)
        // Validate restored content now, before exposing a holder to the caller.
        decode(handle[key])
    }

    val flow: StateFlow<T?> = SelectedState(handle.getStateFlow<String?>(key, null), ::decode)

    fun set(value: T?) {
        val encoded = json.encodeToString(nullableSerializer, value)
        handle[key] = encoded
    }

    private fun decode(encoded: String?): T? = encoded?.let {
        json.decodeFromString(nullableSerializer, it)
    }
}

/** Derives a distinct, read-through state without persisting a duplicate copy. Keep [transform] pure. */
fun <A, B> StateFlow<A>.select(transform: (A) -> B): StateFlow<B> = SelectedState(this, transform)
