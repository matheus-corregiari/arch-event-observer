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

The entry point for Compose integration is the `.composable` extension property, which converts a
`Flow<DataResult<T>>` (or a single `DataResult<T>`) into a `ComposableDataResult<T>`.

You then use `Unwrap` to define how each state should be rendered.

## Usage

### Basic Example

```kotlin
val flow: ResponseFlow<String> = ...

flow.composable.Unwrap {
    OnShowLoading { CircularProgressIndicator() }
    OnData { data -> Text("Content: $data") }
    OnError { error -> Text("Error: ${error.message}") }
}
```

### Animations

Animations are handled via the `contentTransform` property in `AnimationConfig`.
- If `contentTransform` is `null`, animations are disabled, and the content is rendered directly.
- If `contentTransform` is non-null, it uses `AnimatedContent` for smooth transitions.

You can customize transitions or disable them:

```kotlin
flow.composable
    .animation {
        contentTransform = slideInVertically() + fadeIn() togetherWith (slideOutVertically() + fadeOut())
    }
    .Unwrap(modifier = Modifier.padding(16.dp)) {
        OnData { data -> Text(data) }
    }
```

To disable animations globally:

```kotlin
ComposableDataResult.AnimationConfig.defaultContentTransform = null
```

### Side Effects (Non-Compose)

If you need to trigger non-UI side effects (like logging) when a state changes, use
`outsideComposable`:

```kotlin
flow.composable
    .outsideComposable {
        error { throwable -> Logger.log(throwable) }
    }
    .Unwrap {
        OnData { data -> Text(data) }
    }
```

## Available Observers (inside `Unwrap`)

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

## State Collection

`Unwrap` uses `collectAsStateWithLifecycle()` when a `LifecycleOwner` is available (defaulting to
`LocalLifecycleOwner.current`), ensuring efficient and lifecycle-aware collection.

For manual control, you can use `collectAsComposableState()`:

```kotlin
val compState by flow.collectAsComposableState()
compState.Unwrap {
    OnData { data -> Text(data) }
}
```
