# Migrating State Handle

`state-handle` from Arch Toolkit is superseded by `event-observer-state` in Arch Event Observer 2.3.0.
The new artifact is published with the Event Observer release. The old artifact only carries a POM
notice; there is **no Maven relocation** and no automatic dependency or import rewrite.

| Before | After |
|---|---|
| `io.github.matheus-corregiari:state-handle:2.0.0-rc17` | `io.github.matheus-corregiari:event-observer-state:2.3.0` |
| `br.com.arch.toolkit.stateHandle` | `br.com.arch.toolkit.eventObserver.state` |
| Koin-provided `Json` | Default `Json`, or an explicit argument |
| Native value with JSON shadow fallback | One JSON value per key |
| `SavableObject` / Parcelable / Serializable markers | `@Serializable` or explicit `KSerializer<T>` |

## API changes

- `saveState<T>()`, `saveResponseState<T>()`, `value<T>()`, `get`, `set`, `flow` and `invalidate`
  retain their basic purpose. `flow()` now exposes stable `StateFlow` snapshots.
- `T` is a non-null payload type; a holder itself accepts null. Use `value<String>()`, not
  `value<String?>()`. Lists and maps can contain nullable values when their serializer supports them.
- `saveResponseState(default = ...)` takes a payload default. To deliver an actual operation result,
  call `state.set(dataResult)`. Defaults never replace restored values.
- Direct holder constructors require a non-null serializer and no longer accept silent fallback
  failure. The `json`, `scope` and `stateHandle` parameters remain explicit.
- `load` always starts another operation and returns `Job`. The old `evaluate` argument is removed;
  use `if (state.get() == null) state.load { ... }` for explicit reuse.
- `set(value, distinct)` remains available; observed state is also distinct.
- `.required` and `.default` remain read-only delegate fallbacks and now agree with their observed
  flows. Import them from `StateValue.Companion`. Use `default = value` for persisted initialization
  or `select { it ?: fallback }` for a derived fallback. Nullable states can still be cleared.
- Use `bind`, `bindMapped` and `bindReducing` for plain flows; use `load`, `loadMapped` and
  `loadReducing` for result flows. Use `select` for smaller states derived from one saved model.
- Clearing uses a saved JSON null rather than removing the key. Do not remove an active key directly.

## Existing saved snapshots

The old native/shadow representation is not decoded by this module. Choose a new key (for example
`users-v2`) when adopting it, or explicitly convert old saved values before creating the new holder.
Do not reuse a key containing a native object as if it were JSON. Invalid content fails explicitly.
This does not change the Toolkit module's code or the behavior of consumers staying on the old artifact.

## Restoration checklist

Use a handle provided by a restoring owner, serializable payloads and a main-thread scope. Restore
the payload as `Success`, reconnect continuous sources explicitly, and decide when to refresh.
Loading and errors are transient. `savedStateHandleCompat()` and `enableSavedStateHandleCompat()`
from the old README were not implemented APIs and are not provided here.

See the [module guide](modules/event-observer-state.md) for complete operation and platform semantics.
