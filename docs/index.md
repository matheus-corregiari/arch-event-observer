# Arch Event Observer

Arch Event Observer is a Kotlin-first event and result observation toolkit for Android and Compose
Multiplatform.

The repository is split into two public modules:

- `event-observer` for `DataResult`, `ResponseLiveData`, `ResponseFlow`, and reactive helpers
- `event-observer-compose` for `ComposableDataResult` and Compose-driven state rendering

## What You Get

- A single `DataResult<T>` model for success, loading, error, and neutral states
- LiveData wrappers for Android UI layers
- Flow wrappers for shared state and coroutine-based pipelines
- Compose helpers for declarative rendering without manual `when` blocks
- published docs that stay aligned with the shipped public API

## Choose Your Module

- [`event-observer`](modules/event-observer.md) for the base model, LiveData, and Flow support
- [`event-observer-compose`](modules/event-observer-compose.md) for Compose rendering on top of the
  base module

## Start Here

- [Getting Started](getting-started.md)
- [event-observer](modules/event-observer.md)
- [event-observer-compose](modules/event-observer-compose.md)
- [Core Concepts](core-concepts.md)
- [Recipes](recipes.md)
- [API Reference](api/index.md)
- [Changelog](changelog/index.md)
- [Contributing](contributing.md)

## Quick Example

```kotlin
val result = dataResultSuccess("Hello")

result.unwrap {
    data { value -> println(value) }
    error { throwable -> println(throwable.message) }
}
```

```kotlin
myFlow.composable.Unwrap {
    OnShowLoading { CircularProgressIndicator() }
    OnData { value -> Text(value.toString()) }
    OnError { error -> Text(error.message ?: "Unknown error") }
}
```

## Scope

This project keeps the API focused on observation and rendering. It does not try to replace your
repository, state holder, or UI architecture.
