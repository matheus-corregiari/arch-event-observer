# Event Observer State

Shared Kotlin state connecting Repository data to the View through `SavedStateHandle`.

## Installation

```kotlin
kotlin {
    sourceSets.commonMain.dependencies {
        implementation("io.github.matheus-corregiari:event-observer-state:<version>")
    }
}
```

Available since 2.3.0. Apply the Kotlin serialization plugin to the module containing complex models.
Package: `br.com.arch.toolkit.eventObserver.state`.

## Getting started

```kotlin
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import br.com.arch.toolkit.eventObserver.state.saveState
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable
data class User(val name: String)

class UsersViewModel(handle: SavedStateHandle) : ViewModel() {
    val users by handle.saveState<List<User>>(default = emptyList())
    val count = users.select { it?.size ?: 0 }

    fun connect(source: Flow<List<User>>) = users.bind(source)
    fun replace(users: List<User>) = this.users.set(users)
}
```

Observe `users.flow()` in the View. Finishing a source keeps the last value; connecting again replaces
its previous operation. Use `saveResponseState` and `load` for `Flow<DataResult<T>>`. Use `bindMapped`
or `loadMapped` to save a transformed payload, and `select` for cheap smaller states without duplicate storage.
For expensive direct updates use `setAsync`; for expensive projections use `selectAsync`. Operations
prepare data on `Dispatchers.Default` and commit on the owner scope. Capture UI inputs before starting
worker callbacks. See [performance tests and limits](../docs/state-performance.md), particularly for
browser targets and large saved-state payloads.

Objects, typed lists and typed maps are supported. Complex values need `@Serializable` or a supplied
serializer. The implementation is entirely in `commonMain`; use the main thread and a handle supplied
by a restoring owner. Only payloads are saved; transient loading and errors are not restored.

## Documentation

- [Module guide and recipes](../docs/modules/event-observer-state.md)
- [Migration guide](../docs/migration-state.md)
- [API overview](../docs/api/event-observer-state.md)
- [Compiled usage examples](src/commonTest/kotlin/br/com/arch/toolkit/eventObserver/state/UsageTest.kt)
