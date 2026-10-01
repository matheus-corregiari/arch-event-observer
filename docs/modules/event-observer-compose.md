# `event-observer-compose`

`event-observer-compose` is the UI-facing module for Jetpack Compose Multiplatform. It provides a
declarative rendering DSL to handle the states of a `DataResult` or `ResponseFlow`.

This page describes the 3.0.0 API. See [GitHub Releases](https://github.com/matheus-corregiari/arch-event-observer/releases) for publication status. The [migration section](#migration-from-the-builder-api) lists APIs removed since 2.3.0.

## Install

```kotlin
dependencies {
    implementation("io.github.matheus-corregiari:event-observer-compose:<version>")
}
```

## Core Concept

There are two primary ways to render `DataResult` states in Jetpack Compose:

1. **Stateless / Direct Rendering (Recommended for UDF and Previews)**:
   Use `DataResultContent(result = ...)` or `result.Content { ... }` when you observe state at the screen level (e.g., via `collectAsStateWithLifecycle()`).
2. **Flow-Based Collection**:
   Use `flow.Content { ... }` or `ComposableDataResult(flow = ...) { ... }` to automatically collect and render a `Flow<DataResult<T>>` without creating a builder object.

---

## Usage

### Stateless / Direct Usage (`DataResultContent` & `@Preview`)

When following Compose State Hoisting and Unidirectional Data Flow (UDF), collect state at the screen level and pass the `DataResult<T>` snapshot directly to `DataResultContent` or `result.Content`:

Here `viewModel.state` is a `StateFlow<DataResult<User>>`.

```kotlin
val resultState by viewModel.state.collectAsStateWithLifecycle()

DataResultContent(result = resultState) {
    OnShowLoading { CircularProgressIndicator() }
    OnData { data -> Text("Content: $data") }
    OnError { error -> Text("Error: ${error.message}") }
}
```

For a plain `Flow<DataResult<T>>`, supply an initial snapshot when collecting:

```kotlin
val resultState by viewModel.flow.collectAsStateWithLifecycle(initialValue = dataResultNone())
```

Or render the collected snapshot using the extension function:

```kotlin
resultState.Content {
    OnShowLoading { CircularProgressIndicator() }
    OnData { data -> Text("Content: $data") }
    OnError { error -> Text("Error: ${error.message}") }
}
```

This approach is **@Preview friendly** and allows instant rendering in Android Studio previews without requiring mock flows:

```kotlin
@Preview
@Composable
fun SuccessPreview() {
    dataResultSuccess("Hello World").Content {
        OnData { text -> Text(text) }
    }
}
```

---

### Flow-Based Usage (`flow.Content` & `ComposableDataResult`)

For self-contained components driven by a single `ResponseFlow` or `Flow<DataResult<T>>`, use `flow.Content` directly:

```kotlin
val flow: ResponseFlow<String> = ...

flow.Content {
    OnShowLoading { CircularProgressIndicator() }
    OnData { data -> Text("Content: $data") }
    OnError { error -> Text("Error: ${error.message}") }
}
```

Or using the top-level Composable function:

```kotlin
ComposableDataResult(flow = viewModel.flow) {
    OnShowLoading { CircularProgressIndicator() }
    OnData { data -> Text("Content: $data") }
    OnError { error -> Text("Error: ${error.message}") }
}
```

---

### Animations

When using `DataResultContent` or `flow.Content`, you can pass a standard Jetpack Compose `transitionSpec`:

```kotlin
flow.Content(
    transitionSpec = {
        slideInVertically() + fadeIn() togetherWith (slideOutVertically() + fadeOut())
    }
) {
    OnData { data -> Text(data) }
}
```

By default, `transitionSpec` is `null`, so content renders without animation.

Observer registrations are remembered by the content lambda. Stable lambdas reuse the
registrations; changed captures rebuild them so rendered callbacks stay current.
Visibility is filtered once per result and observer list inside each animated content
slot, preserving outgoing snapshots during transitions. Registration keys preserve
remembered state when an earlier observer becomes hidden. This trades a filtered-list
allocation on changes for avoiding repeated visibility checks on unchanged inputs.

All matching observers render together, in registration order: for example, `OnSuccess`,
`OnData` and `OnStatus` can render the same snapshot. Selecting only the first match
would discard content. The animated container stays composed even when no observer
matches so outgoing content can finish its exit transition.

With animation enabled, payload updates preserve the content identity when status, data/error presence and collection shape stay the same. Empty, single and multiple-item collections have distinct keys. Sequence keys do not traverse their elements.

---

### Side Effects (Non-Compose)

Handle side effects explicitly with `LaunchedEffect`. Collect the state once and share
the same snapshot between the effect and `DataResultContent`. In this example,
`viewModel.state` is a `StateFlow<DataResult<T>>`:

```kotlin
val resultState by viewModel.state.collectAsStateWithLifecycle()

LaunchedEffect(resultState) {
    if (resultState.isError) {
        resultState.error?.let { Logger.log(it) }
    }
}

DataResultContent(result = resultState) {
    OnData { data -> Text(data.toString()) }
}
```

The effect restarts when its key changes and is cancelled when it leaves composition.

---

## Available Observers

| Observer        | Triggered when...                                                     |
| :-------------- | :-------------------------------------------------------------------- |
| `OnData`        | Data is present, regardless of status (Success, Error, Loading).      |
| `OnSuccess`     | `DataResult` is Success.                                              |
| `OnShowLoading` | `DataResult` is Loading.                                              |
| `OnHideLoading` | `DataResult` is Success or Error; no prior Loading state is required. |
| `OnError`       | `DataResult` is Error.                                                |
| `OnEmpty`       | Data is a collection and it is empty.                                 |
| `OnNotEmpty`    | Data is a collection and it is NOT empty.                             |
| `OnSingle`      | Data is a collection and has exactly one item.                        |
| `OnMany`        | Data is a collection and has multiple items.                          |
| `OnNone`        | `DataResult` is in the 'None' state.                                  |
| `OnResult`      | For the current snapshot, including None.                             |
| `OnStatus`      | Receives the current status, optionally filtered by data presence.    |

Collection observers also accept maps and sequences. Unlike animation keys, visibility
checks for sequences count their elements, and `OnSingle` reads the first element again.
Materialize finite sequences as lists before rendering; do not use constrained-once or
unbounded sequences with these observers. For maps, `OnSingle<Pair<K, V>>` receives the
single key/value pair.

Treat `DataResult` payloads as snapshots: emit a new result and a new collection instead
of mutating a collection in place. Visibility caching and Compose state equality rely
on observable input changes.

---

## State Collection

`Flow.Content` and `ComposableDataResult` use `collectAsStateWithLifecycle()` by default
with `LocalLifecycleOwner.current`, collecting only while that owner is at least `STARTED`. A custom owner lets a nested
screen or navigation entry control collection independently of its parent.
Pass `owner = null` to use `collectAsState()` until the composable leaves composition;
this is useful in tests or hosts where lifecycle gating is not desired. Previews can
use `DataResultContent(result)` without a flow or lifecycle owner.

A plain flow with no initial/replayed value renders nothing until its first emission.
`StateFlow` and replaying `SharedFlow` provide their current snapshot immediately.
Snapshot `DataResult.Content` does not collect a flow or access a lifecycle owner.

## Content scope

The content receiver `DataResultContentScope<T>` carries `@DataResultContentDsl`,
a Kotlin `@DslMarker`. Nested receivers resolve
implicit observer registrations against the nearest content scope.

## Migration from the builder API

| Removed API                                                   | Replacement                                                                                             |
| ------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------- |
| `result.composable.Unwrap { ... }`                            | `result.Content { ... }` or `DataResultContent(result) { ... }`                                         |
| `flow.composable.Unwrap { ... }`                              | `flow.Content { ... }` or `ComposableDataResult(flow) { ... }`                                          |
| `flow.collectAsComposableState()`                             | Collect with `collectAsStateWithLifecycle` and render the snapshot; use `initialValue` for a plain Flow |
| `liveData.composable` / `liveData.collectAsComposableState()` | Convert with AndroidX `asFlow()`, remember that Flow, and render with `Content`                         |
| Builder `.outsideComposable { ... }`                          | Explicit `LaunchedEffect` using the rendered snapshot                                                   |

For Android LiveData, keep the converted Flow stable across recompositions:

```kotlin
import androidx.compose.runtime.remember
import androidx.lifecycle.asFlow
import br.com.arch.toolkit.compose.Content

val flow = remember(userLiveData) { userLiveData.asFlow() }
flow.Content {
    OnData { user -> Text(user.toString()) }
}
```

Replace `flow.composable.Unwrap { ... }`
with `flow.Content { ... }`. Handle side effects explicitly with `LaunchedEffect`. Pass custom animations
through `transitionSpec`; the former `AnimationConfig` and its global defaults
are removed. The default is now no animation. Explicit references to
`ObserveComposableWrapper<T>` must become `DataResultContentScope<T>`.

These are source and binary incompatible changes for existing Compose consumers.
