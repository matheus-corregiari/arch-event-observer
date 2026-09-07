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

    override fun getValue(thisRef: ViewModel, property: KProperty<*>): S {
        val current = holder ?: lazy { create(thisRef, property.name) }.also { holder = it }
        return current.value
    }
}

/** Plain saved state. Default only applies when the handle has no value for the resolved key. */
inline fun <reified T : Any> SavedStateHandle.saveState(
    name: String = "",
    default: T? = null,
    json: Json = Json,
    serializer: KSerializer<T> = serializer<T>()
): StateDelegate<ViewModelState.Regular<T>> = StateDelegate { model, property ->
    ViewModelState.Regular(
        name.ifBlank {
            property
        },
        serializer,
        this,
        model.viewModelScope,
        json,
        default
    )
}

/** Result state. Restores payload as Success; defaults are payloads, never transient statuses. */
inline fun <reified T : Any> SavedStateHandle.saveResponseState(
    name: String = "",
    default: T? = null,
    json: Json = Json,
    serializer: KSerializer<T> = serializer<T>()
): StateDelegate<ViewModelState.Result<T>> = StateDelegate { model, property ->
    ViewModelState.Result(
        name.ifBlank {
            property
        },
        serializer,
        this,
        model.viewModelScope,
        json,
        default
    )
}
