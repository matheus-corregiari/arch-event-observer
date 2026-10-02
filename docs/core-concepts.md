# Core Concepts

## `DataResult`

`DataResult<T>` carries three things at once:

- `data`
- `error`
- `status`

The status values are:

- `NONE` for a neutral, empty result
- `LOADING` while work is in progress
- `SUCCESS` when data is ready
- `ERROR` when the operation failed

The class also exposes convenience flags such as `isSuccess`, `isError`, `isLoading`, `isNone`,
`hasData`, `hasError`, and list-oriented checks like `isEmpty`, `isNotEmpty`, `hasOneItem`, and
`hasManyItems`.

## Event Filters

`EventDataStatus` controls whether a callback should fire based on data presence.

- `WithData`
- `WithoutData`
- `DoesNotMatter`

Use it when a callback should only react to a value-bearing result, a value-less result, or both.

## Wrapper DSL

`ObserveWrapper<T>` is the callback DSL behind `unwrap { ... }` and the LiveData observation
helpers.

It lets you react to:

- `data`
- `loading`
- `showLoading`
- `hideLoading`
- `error`
- `success`
- `result`
- `status`
- `empty`
- `notEmpty`
- `oneItem`
- `manyItems`
- `none`

The wrapper keeps the control flow small: define what you care about, then attach it to a
`DataResult`.

## LiveData And Flow

`ResponseLiveData<T>` and `ResponseFlow<T>` wrap reactive sources that emit `DataResult<T>`.

They exist for different layers:

- `ResponseLiveData` fits Android UI code that still uses LiveData
- `ResponseFlow` fits coroutine-driven state pipelines and shared state

Both expose helpers for:

- mapping data
- mapping errors
- converting from plain `Flow`
- keeping derived state in sync

`ResponseStateFlow<T>` and `ResponseSharedFlow<T>` give you stateful or shared flow behavior when
you need it.

These APIs live in [`event-observer`](modules/event-observer.md).

## Compose

`ComposableDataResult(flow)` turns a `Flow<DataResult<T>>` into a declarative Compose DSL.

It renders common blocks such as:

- `OnData`
- `OnError`
- `OnShowLoading`
- `OnHideLoading`
- `OnSuccess`
- `OnStatus`
- `OnEmpty`
- `OnNotEmpty`
- `OnSingle`
- `OnMany`

It supports optional animations through `transitionSpec`. Handle side effects explicitly
with Compose `LaunchedEffect`.

These APIs live in [`event-observer-compose`](modules/event-observer-compose.md).

## Design Rule

The library stays opinionated about result shape, but it stays out of your app structure. It gives
you the primitives and leaves the screen architecture to you.

## Result, event, or persisted state?

| Need | Use | Example |
| --- | --- | --- |
| Describe an operation now | Core `DataResult<T>` | Loading with cached users, then success or error |
| Perform an action for a new occurrence | An application event stream | Navigate after a new save, or show a snackbar once |
| Retain a screen value across requests and owner recreation | State `saveState` / `saveResponseState` | Search filter, selected ID, compact screen payload |

A result is a snapshot, not a delivery guarantee. `StateFlow` keeps the latest value,
conflates equal values and can skip intermediate values. Wrapper callbacks describe
matching results; they do not create a durable application event queue.

### Keep observation execution separate from the result

`DataResult` equality, hashing, destructuring and `copy()` use only `data`, `error`
and `status`. Existing `.scope(...)` and `.transformDispatcher(...)` setters mutate
only that instance. Generated copies and `transform` results do **not** inherit
those settings. This legacy behavior is retained for compatibility.

Prefer execution settings on each observation:

```kotlin
val result = dataResultSuccess("Ready")
val copy = result.copy()
copy.unwrap(scope = viewModelScope, transformDispatcher = Dispatchers.Default) {
    data(transformer = { it.uppercase() }) { prepared -> render(prepared) }
}
```

This overload ignores legacy instance settings; its DSL can override the supplied
settings. It stores no configuration on either result. The owner cancels the scope;
transformations run on the supplied dispatcher and callbacks return to the owner
scope. The existing `unwrap { scope(viewModelScope); data { ... } }` is also supported.
Avoid the dispatcher-only legacy setter when you already have an owned scope.

### Render state; collect actions explicitly in Compose

```kotlin
val result by viewModel.users.flow().collectAsStateWithLifecycle()
DataResultContent(result = result) {
    OnData { users -> Text("Users: ${users.size}") }
    OnError { error -> Text(error.message.orEmpty()) }
}
LaunchedEffect(viewModel.events) {
    viewModel.events.collect { event -> handleEvent(event) }
}
```

`events` and `handleEvent` are application-defined. Choose buffering, replay and
acknowledgement for the application's delivery requirements. This collector is
cancelled when it leaves composition. `LaunchedEffect(result)` instead reruns when
its result key changes; replayed/restored state should not automatically repeat a
navigation or snackbar action. Use lifecycle-aware collection if actions should
only be handled while the screen is started.

### Persist compact screen values

```kotlin
val filter by handle.saveState<String>(default = "")
val users by handle.saveResponseState<List<User>>()
val query = filter.get().orEmpty() // owner thread, before producer work
users.load { repository.users(query) } // Flow<DataResult<List<User>>>
```

State producers, mapping and JSON preparation run on the worker dispatcher;
commits run on the owner scope. A replacement operation cancels its predecessor
and rejects late results. Cancellation is not an error. Stopping UI collection
alone does not stop the ViewModel operation. Use `setAsync` for expensive direct
writes; synchronous writes and initial restoration still execute on the caller.

Restoration saves the payload, not exceptions, in-progress work or events. A
restored non-null result payload becomes success; saved null becomes none.
Guarantees depend on the `SavedStateHandle` owner; this module creates no disk
storage. See the [state contract](modules/event-observer-state.md).

### Sequence inspection

`isEmpty` and `isNotEmpty` obtain an iterator and call `hasNext`; sequence builders
may produce one element during that call. `hasOneItem` and `hasManyItems` inspect
at most two elements. Unbounded sequences can therefore be checked directly,
but work or failures beyond that prefix are not evaluated.

Each check obtains a new iterator. A constrained-once sequence cannot be checked
again, and inspection may advance an underlying iterator. No caching or replay is
introduced. Materialize a **finite** sequence outside the UI thread before using
multiple collection observers or rendering its contents.
