package br.com.arch.toolkit.eventObserver.state

import android.os.Bundle
import android.os.Parcel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.SAVED_STATE_REGISTRY_OWNER_KEY
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.enableSavedStateHandles
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import br.com.arch.toolkit.util.dataResultError
import br.com.arch.toolkit.util.dataResultSuccess
import kotlinx.serialization.KSerializer
import kotlinx.serialization.encoding.Encoder
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame

/** Exercises SavedStateRegistry and a real parcel round trip, not a reused in-memory handle. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE)
class AndroidRestoreTest {
    @Test
    fun complexPayloadsRestoreIntoNewOwnerAndViewModel() {
        val first = Owner(null)
        val model = first.model()
        val screen = Screen(listOf(Profile("Ada", listOf(null, "admin"))), mapOf("total" to 1))
        model.screen.set(screen)
        model.profiles.set(listOf(Profile("Lin")))
        model.map.set(mapOf("user" to Profile("Grace")))
        model.response.set(dataResultSuccess(screen))
        model.response.set(dataResultError(IllegalStateException("transient")))
        model.cleared.set(null)
        val saved = parcelCopy(first.save())
        first.destroy()

        val second = Owner(saved)
        val restored = second.model()
        try {
            assertNotSame(model, restored)
            assertEquals(screen, restored.screen.flow().value)
            assertEquals(listOf(Profile("Lin")), restored.profiles.get())
            assertEquals(mapOf("user" to Profile("Grace")), restored.map.get())
            assertEquals(dataResultSuccess(screen), restored.response.flow().value)
            assertNull(restored.cleared.get())
            restored.cleared.set(2)
            assertEquals(2, restored.cleared.flow().value)
        } finally {
            second.destroy()
        }
    }

    @Test
    fun configurationChangeRetainsViewModelAndStateFlow() {
        val first = Owner(null)
        val model = first.model()
        val stream = model.response.flow()
        model.response.set(dataResultSuccess(Screen(emptyList(), mapOf("total" to 7))))
        val saved = first.save()
        first.destroy(clearStore = false)
        val second = Owner(saved, first.viewModelStore)
        try {
            assertSame(model, second.model())
            assertSame(stream, second.model().response.flow())
            assertEquals(7, second.model().response.get()?.counts?.get("total"))
        } finally {
            second.destroy()
        }
    }

    @Test
    fun savingPreparedJsonDoesNotRunTheModelCodecAgain() {
        val owner = Owner(null)
        try {
            val model = owner.model()
            model.screen.set(Screen(listOf(Profile("Ada")), emptyMap()))
            val beforeSave = model.screenEncodes
            parcelCopy(owner.save())
            assertEquals(1, beforeSave)
            assertEquals(beforeSave, model.screenEncodes)
        } finally {
            owner.destroy()
        }
    }

    private fun parcelCopy(bundle: Bundle): Bundle {
        val parcel = Parcel.obtain()
        return try {
            parcel.writeBundle(bundle)
            val bytes = parcel.marshall()
            parcel.unmarshall(bytes, 0, bytes.size)
            parcel.setDataPosition(0)
            requireNotNull(parcel.readBundle(javaClass.classLoader))
        } finally {
            parcel.recycle()
        }
    }

    private class Model(handle: SavedStateHandle) : ViewModel() {
        var screenEncodes = 0
            private set
        private val screenCodec = object : KSerializer<Screen> by Screen.serializer() {
            override fun serialize(encoder: Encoder, value: Screen) {
                screenEncodes++
                Screen.serializer().serialize(encoder, value)
            }
        }
        val screen by handle.saveState(serializer = screenCodec)
        val profiles by handle.saveState<List<Profile>>()
        val map by handle.saveState<Map<String, Profile>>()
        val response by handle.saveResponseState<Screen>()
        val cleared by handle.saveState<Int>(default = 99)
    }

    private class Owner(
        restored: Bundle?,
        override val viewModelStore: ViewModelStore = ViewModelStore()
    ) : SavedStateRegistryOwner, ViewModelStoreOwner {
        private val registry = LifecycleRegistry(this)
        private val controller = SavedStateRegistryController.create(this)
        override val lifecycle: Lifecycle get() = registry
        override val savedStateRegistry: SavedStateRegistry get() = controller.savedStateRegistry

        init {
            controller.performAttach()
            controller.performRestore(restored)
            enableSavedStateHandles()
            registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        }

        fun model(): Model {
            val extras = MutableCreationExtras().apply {
                set(SAVED_STATE_REGISTRY_OWNER_KEY, this@Owner)
                set(VIEW_MODEL_STORE_OWNER_KEY, this@Owner)
            }
            val factory = viewModelFactory { initializer { Model(createSavedStateHandle()) } }
            return ViewModelProvider.create(this, factory, extras)[Model::class]
        }

        fun save(): Bundle {
            registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            return Bundle().also(controller::performSave)
        }

        fun destroy(clearStore: Boolean = true) {
            registry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            if (clearStore) viewModelStore.clear()
        }
    }
}
