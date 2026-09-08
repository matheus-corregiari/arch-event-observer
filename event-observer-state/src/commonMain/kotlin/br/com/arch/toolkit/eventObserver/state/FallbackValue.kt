package br.com.arch.toolkit.eventObserver.state

import kotlinx.coroutines.flow.StateFlow
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Writable saved property with a fallback applied only to reads and observation.
 * Assignments are saved normally; reading a fallback never writes it to the handle.
 * Created by [StateValue.Companion.required] or [StateValue.Companion.default].
 */
class FallbackValue<T : Any, R : T?> internal constructor(
    private val other: StateValue<T>,
    private val read: (T?) -> R
) : ReadWriteProperty<Any?, R> {
    private var projected: StateFlow<R>? = null

    /** Binds the underlying property name before observing an implicit key. */
    operator fun provideDelegate(ref: Any?, property: KProperty<*>): FallbackValue<T, R> = apply {
        other.provideDelegate(ref, property)
    }

    /** Reads the saved value, applying the fallback without persisting it. */
    override fun getValue(thisRef: Any?, property: KProperty<*>): R = read(other.get())

    /** Saves the assigned value through the underlying property. */
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: R) = other.set(value)

    /** Stable projection, reflecting defaults consistently with delegated reads. */
    fun flow(): StateFlow<R> = projected ?: other.flow().select(read).also { projected = it }
}
