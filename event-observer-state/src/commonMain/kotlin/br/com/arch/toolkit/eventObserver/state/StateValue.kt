package br.com.arch.toolkit.eventObserver.state

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/** Serialized property delegate. Bind a property or supply a key before calling [flow]. */
class StateValue<T : Any>(
    private val handle: SavedStateHandle,
    private val key: String,
    private val serializer: KSerializer<T>,
    private val json: Json = Json,
    private val default: T? = null
) : ReadWriteProperty<Any?, T?> {
    private var stored: StoredState<T>? = null

    /** Resolves an implicit key once, when the property is declared. */
    operator fun provideDelegate(ref: Any?, property: KProperty<*>): StateValue<T> = apply {
        state(key.ifBlank { property.name })
    }

    private fun state(name: String = key): StoredState<T> = stored ?: StoredState(
        handle,
        name,
        serializer,
        json,
        default
    ).also { stored = it }

    /** Reads the payload saved under the resolved property key. */
    override fun getValue(thisRef: Any?, property: KProperty<*>): T? = state().flow.value

    /** Serializes the assignment before replacing the saved payload. */
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) = state().set(value)

    /** Current value; defaults only initialize absent keys. */
    fun get(): T? = state().flow.value

    /** Saves synchronously. Encoding failures leave the old value intact. Null does not remove the key. */
    fun set(value: T?) = state().set(value)

    /** Stable, read-through observable state. Do not call SavedStateHandle.remove on its key. */
    fun flow(): StateFlow<T?> = state().flow

    /** Read fallbacks do not persist another value; assignments still update the saved property. */
    companion object {
        /** Supplies a non-null fallback on reads and observation, without writing it to the handle. */
        fun <T : Any> StateValue<T>.required(default: () -> T): FallbackValue<T, T> =
            FallbackValue(this) { it ?: default() }

        /** Supplies a nullable fallback on reads and observation, without writing it to the handle. */
        fun <T : Any> StateValue<T>.default(default: () -> T?): FallbackValue<T, T?> =
            FallbackValue(this) { it ?: default() }
    }
}

/**
 * Creates a saved property for use in common code, with no ViewModel requirement.
 *
 * [key] defaults to the delegated property name. Supply it explicitly when using [StateValue.get],
 * [StateValue.set] or [StateValue.flow] without a delegated property.
 * [default] initializes an absent entry only; saved null stays null.
 * Complex models require a generated or explicit [serializer], using the supplied [json].
 * Access on the main thread. Encoding failures preserve the previous saved value.
 */
inline fun <reified T : Any> SavedStateHandle.value(
    key: String = "",
    default: T? = null,
    json: Json = Json,
    serializer: KSerializer<T> = serializer<T>()
): StateValue<T> = StateValue(this, key, serializer, json, default)
