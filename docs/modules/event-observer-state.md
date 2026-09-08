# event-observer-state

Typed screen state connecting repository operations to the View through `SavedStateHandle`.
The state outlives individual requests; the handle's owner controls restoration.

```kotlin
kotlin {
    sourceSets.commonMain.dependencies {
        implementation("io.github.matheus-corregiari:event-observer-state:2.3.0")
    }
}
```

The module supports Android (API 23+), JVM, iOS ARM64/simulator ARM64, JS and WasmJS.

## Minimal ViewModel

```kotlin
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import br.com.arch.toolkit.eventObserver.state.saveResponseState
import br.com.arch.toolkit.eventObserver.state.saveState
import kotlinx.serialization.Serializable

@Serializable
data class User(val name: String)

class UsersViewModel(handle: SavedStateHandle) : ViewModel() {
    val users by handle.saveResponseState<List<User>>()
    val filter by handle.saveState<String>(default = "")

    fun refresh(repository: UsersRepository) = users.load {
        repository.users(filter.get().orEmpty()) // Flow<DataResult<List<User>>>
    }

    fun changeFilter(value: String, repository: UsersRepository) {
        filter.set(value)
        refresh(repository)
    }
}
```

Observe `users.flow()` as state from the View. It is the same `StateFlow` before, during and after
every request. A completed producer keeps its last value; refresh always starts a new operation.
Calling `load` again cancels the previous operation and rejects its late results.

For a continuous repository stream, use the same `load` call. Call `cancel()` to disconnect it;
the current saved data remains observable. The ViewModel scope also cancels collection on disposal.
No loading event is invented: the source supplies `DataResult` loading/error/success statuses.

## Without DataResult

`saveState<T>()` exposes `Regular<T>`. Use `set(value)` for an object, list or map, or
`bind(repository.flow)` to collect a one-shot or continuous `Flow<T>`. `bind` returns a `Job`.
On failure the old data remains; pass `onError = { ... }` to handle failure explicitly. By default,
the exception reaches the supplied scope's exception handler. Cancellation is never an error result.

You can construct `ViewModelState.Regular` or `ViewModelState.Result` directly with a handle,
serializer and main-thread scope. The delegates simply supply the property key and ViewModel scope.
No Koin setup is required.

For property syntax, `var query by handle.value<String>()` infers the key from the property name.
`StateValue.Companion.required` and `default` provide fallbacks for property reads and
their flows; they do not persist the fallback. Assignments still save the assigned value. `default = value` initializes an absent saved entry.

## Transform a repository payload

```kotlin
users.loadMapped(transform = { dtos: List<UserDto> -> dtos.map { User(it.displayName) } }) {
    repository.usersDto()
}
```

For plain values use `state.bindMapped(repository.flow) { input -> viewValue(input) }`.
The transformed value is what gets serialized and restored. Transformations may suspend;
operation replacement is checked again before committing the transformed value.

For pagination or accumulation, define the rule yourself:

```kotlin
users.loadReducing(reduce = { current, page: List<User> -> current.orEmpty() + page }) {
    repository.nextPage()
}
```

Plain values have the equivalent `bindReducing(source) { current, input -> next }`.
Reducers run for non-null result payloads only. Each new operation replaces the previous one;
there is no parallel queue or implicit merge policy. For filters, use replacement rather than append.

## A response feeding several smaller states

Persist a serializable screen model when fields must update together:

```kotlin
@Serializable
data class Screen(val users: List<User>, val total: Int)

class ScreenViewModel(handle: SavedStateHandle) : ViewModel() {
    val screen by handle.saveState<Screen>()
    val users = screen.select { it?.users.orEmpty() }
    val total = screen.select { it?.total ?: 0 }
}
```

Map one repository response into `Screen` with `bindMapped`, or use `saveResponseState<Screen>()`
and `loadMapped` when it carries `DataResult`. `select` returns distinct read-through state;
it does not serialize another copy. Observe the full screen state when an atomic snapshot of all
fields is required. A separate editable filter or selection can have its own saved entry.

## Persistence contract

- Keys contain JSON strings. A stored `"null"` is different from an absent key: defaults initialize
  only absent keys. `invalidate()` and `set(null)` clear data without removing the key or its observers.
- `get()`, `flow().value` and `select(...).value` read the handle-backed representation. External
  updates to the owned key must be valid JSON for the same serializer and configuration.
- Do not call `SavedStateHandle.remove()` on an active key: AndroidX detaches its existing flows.
  Use the holder's clearing API. Use one logical state per key and matching serializers.
- Primitive types and typed lists/maps use built-in serializers. Complex objects require
  `@Serializable` or an explicit `KSerializer<T>`. Nested collection elements and map keys must
  also be serializable. Complex map keys require the appropriate `Json` configuration, such as
  `allowStructuredMapKeys`; arbitrary `Any` values are not automatically serializable.
- Apply the Kotlin serialization compiler plugin in the module declaring `@Serializable` models,
  using the same version as that module's Kotlin plugin.
- Supply `json = Json { ... }` and/or `serializer = ...` when needed. Encoding completes before
  writing; an exception leaves the old value intact. Invalid restored JSON fails explicitly
  during holder creation (or first delegate access), without replacing the stored content.
- Saving a `DataResult` persists its non-null payload. Loading/error/none results without data
  retain the last payload; use `invalidate()` to clear it. On restoration, non-null data becomes
  `Success`, while saved null becomes `None`. Throwable and in-progress work are not persisted.
- Run access and operations on the main thread with a main-thread scope. Replace collections
  instead of mutating them in place. Projections must be pure and inexpensive.

## Shared code and tests

All production code lives in `commonMain`. Repository binding, serialization, restoration snapshots,
refresh, filters, cancellation and projection tests live in `commonTest` and run on every test target.
A separate Android integration test verifies `SavedStateRegistry` and a real `Bundle`/`Parcel` round
trip; those Android APIs cannot run in `commonTest`.

See the [compiled usage examples](https://github.com/matheus-corregiari/arch-event-observer/blob/master/event-observer-state/src/commonTest/kotlin/br/com/arch/toolkit/eventObserver/state/UsageTest.kt).

## Platform boundaries

This module uses only the supplied `SavedStateHandle`; it creates no disk or browser storage.
Recreation works to the extent the owner saves and restores that handle. Merely constructing
`SavedStateHandle()` does not install restoration into an application's lifecycle.
Android saved state follows the owner's save/stop lifecycle and is not a guarantee after force-stop,
task dismissal or arbitrary application termination. Other platforms require their own supported
handle owner; this module does not promise persistence after closing the application.

Keep serialized screen state small. Large repository datasets belong in a database/cache; retain
IDs, filters or a compact screen snapshot when the platform's saved-state budget would be exceeded.

See [migration](../migration-state.md) and [API reference](../api/event-observer-state.md).
