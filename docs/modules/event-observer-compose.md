# `event-observer-compose`

`event-observer-compose` is the UI-facing module for Jetpack Compose Multiplatform. It provides a
declarative rendering DSL to handle the states of a `DataResult` or `ResponseFlow`.

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
   Use `flow.Content { ... }` or `ComposableDataResult(flow = ...) { ... }` to automatically collect and render a `Flow<DataResult<T>>` without intermediate wrapper object allocations.

---

## Usage

### Stateless / Direct Usage (`DataResultContent` & `@Preview`)

When following Compose State Hoisting and Unidirectional Data Flow (UDF), collect state at the screen level and pass the `DataResult<T>` snapshot directly to `DataResultContent` or `result.Content`:

```kotlin
val resultState by viewModel.flow.collectAsStateWithLifecycle()

DataResultContent(result = resultState) {
    OnShowLoading { CircularProgressIndicator() }
    OnData { data -> Text("Content: $data") }
    OnError { error -> Text("Error: ${error.message}") }
}
```

Or using the extension function:

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

---

## State Collection

`Content` uses `collectAsStateWithLifecycle()` when a `LifecycleOwner` is available (defaulting to
`LocalLifecycleOwner.current`), and collecting only while that owner is at least `STARTED`. A custom owner lets a nested
screen or navigation entry control collection independently of its parent.
Pass `owner = null` to use `collectAsState()` until the composable leaves composition;
this is useful in tests or hosts where lifecycle gating is not desired. Previews can
use `DataResultContent(result)` without a flow or lifecycle owner.

## Content scope

The content receiver `DataResultContentScope<T>` carries `@DataResultContentDsl`,
a Kotlin `@DslMarker`. Nested receivers resolve
implicit observer registrations against the nearest content scope.

## Migration from the builder API

Replace `flow.composable.Unwrap { ... }`
with `flow.Content { ... }`. Handle side effects explicitly with `LaunchedEffect`. Pass custom animations
through `transitionSpec`; the former `AnimationConfig` and its global defaults
are removed. The default is now no animation. Explicit references to
`ObserveComposableWrapper<T>` must become `DataResultContentScope<T>`.

These are source and binary incompatible changes for existing Compose consumers.
