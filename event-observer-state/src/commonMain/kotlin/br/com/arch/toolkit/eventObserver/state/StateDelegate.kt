package br.com.arch.toolkit.eventObserver.state

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/** Lazily creates one holder using the ViewModel scope and the property name when no key is given. */
class StateDelegate<S>(
    private val create: (ViewModel, String) -> S
) : ReadOnlyProperty<ViewModel, S> {
    private var holder: Lazy<S>? = null

    /** Creates the holder on first access and returns that same holder on subsequent reads. */
    override fun getValue(thisRef: ViewModel, property: KProperty<*>): S {
        val current = holder ?: lazy { create(thisRef, property.name) }.also { holder = it }
        return current.value
    }
}

/**
 * Creates plain saved state as a ViewModel property, for example `val users by handle.saveState<List<User>>()`.
 *
 * [name] defaults to the property name. [default] initializes only an absent saved entry.
 * [serializer] and [json] encode the payload; complex models require a serializable type.
 * The holder uses [viewModelScope]; access it on the main thread.
 * Use [ViewModelState.Regular.bind] for flows or [ViewModelState.set] for direct values.
 */
inline fun <reified T : Any> SavedStateHandle.saveState(
    name: String = "",
    default: T? = null,
    json: Json = Json,
    serializer: KSerializer<T> = serializer<T>()
): StateDelegate<ViewModelState.Regular<T>> = StateDelegate { model, property ->
    ViewModelState.Regular(
        name.ifBlank { property },
        serializer,
        this,
        model.viewModelScope,
        json,
        default
    )
}

/**
 * Creates result state as a ViewModel property, for example `val users by handle.saveResponseState<List<User>>()`.
 *
 * [name] defaults to the property name. [default] is a payload and initializes only an absent entry.
 * [serializer] and [json] encode only the payload. Restored data becomes Success; errors and loading
 * are transient. The holder uses [viewModelScope]; access it on the main thread.
 * Each [ViewModelState.Result.load] replaces the preceding operation, while keeping the same state flow.
 */
inline fun <reified T : Any> SavedStateHandle.saveResponseState(
    name: String = "",
    default: T? = null,
    json: Json = Json,
    serializer: KSerializer<T> = serializer<T>()
): StateDelegate<ViewModelState.Result<T>> = StateDelegate { model, property ->
    ViewModelState.Result(
        name.ifBlank { property },
        serializer,
        this,
        model.viewModelScope,
        json,
        default
    )
}
