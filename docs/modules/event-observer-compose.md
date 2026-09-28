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
   Use `flow.Unwrap { ... }` or `ComposableDataResult(flow = ...) { ... }` to automatically collect and render a `Flow<DataResult<T>>` without intermediate wrapper object allocations.

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
    DataResult.success("Hello World").Content {
        OnData { text -> Text(text) }
    }
}
```

---

### Flow-Based Usage (`flow.Unwrap` & `ComposableDataResult`)

For self-contained components driven by a single `ResponseFlow` or `Flow<DataResult<T>>`, use `flow.Unwrap` directly:

```kotlin
val flow: ResponseFlow<String> = ...

flow.Unwrap {
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

When using `DataResultContent` or `flow.Unwrap`, you can pass a standard Jetpack Compose `transitionSpec`:

```kotlin
flow.Unwrap(
    transitionSpec = {
        slideInVertically() + fadeIn() togetherWith (slideOutVertically() + fadeOut())
    }
) {
    OnData { data -> Text(data) }
}
```

By default, `transitionSpec` is `null` in `DataResultContent` and `flow.Unwrap` (no animation overhead).

---

### Side Effects (Non-Compose)

For `flow.Unwrap`, pass `outsideComposable` parameter to trigger non-UI side effects:

```kotlin
flow.Unwrap(
    outsideComposable = {
        error { throwable -> Logger.log(throwable) }
    }
) {
    OnData { data -> Text(data) }
}
```

When using direct `DataResultContent`, handle non-UI side effects using standard Compose `LaunchedEffect`:

```kotlin
LaunchedEffect(resultState) {
    resultState.unwrap {
        error { throwable -> Logger.log(throwable) }
    }
}
```

---

## Available Observers

| Observer        | Triggered when...                                                |
|:----------------|:-----------------------------------------------------------------|
| `OnData`        | Data is present, regardless of status (Success, Error, Loading). |
| `OnSuccess`     | `DataResult` is Success.                                         |
| `OnShowLoading` | `DataResult` is Loading.                                         |
| `OnHideLoading` | `DataResult` transitions out of Loading.                         |
| `OnError`       | `DataResult` is Error.                                           |
| `OnEmpty`       | Data is a collection and it is empty.                            |
| `OnNotEmpty`    | Data is a collection and it is NOT empty.                        |
| `OnSingle`      | Data is a collection and has exactly one item.                   |
| `OnMany`        | Data is a collection and has multiple items.                     |
| `OnNone`        | `DataResult` is in the 'None' state.                             |
| `OnResult`      | On every emission.                                               |
| `OnStatus`      | Matches a specific `DataResultStatus`.                           |

---

## State Collection

`Unwrap` uses `collectAsStateWithLifecycle()` when a `LifecycleOwner` is available (defaulting to
`LocalLifecycleOwner.current`), ensuring efficient and lifecycle-aware collection.
