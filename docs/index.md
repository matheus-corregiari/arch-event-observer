# Arch Event Observer

Arch Event Observer is a Kotlin-first event and result observation toolkit for Android and Compose
Multiplatform.

The repository is split into three public modules:

- `event-observer` for `DataResult`, `ResponseLiveData`, `ResponseFlow`, and reactive helpers
- `event-observer-compose` for snapshot and Flow-driven Compose rendering
- `event-observer-state` for saved screen state and repository operations

This branch documents the **3.0.0 API**. See [GitHub Releases](https://github.com/matheus-corregiari/arch-event-observer/releases)
for publication status and use the documentation from the Git tag matching your installed version. See the [Compose migration guide](modules/event-observer-compose.md#migration-from-the-builder-api).

Version **3.0.0** includes Compose API changes and privately owned State keys.
See [state migration](migration-state.md#300-privately-owned-state) and
[release notes](changelog/3.0.0.md).

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
- [`event-observer-state`](modules/event-observer-state.md) for restorable shared screen state

## Start Here

- [Getting Started](getting-started.md)
- [event-observer](modules/event-observer.md)
- [event-observer-compose](modules/event-observer-compose.md)
- [event-observer-state](modules/event-observer-state.md)
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
myFlow.Content {
    OnShowLoading { CircularProgressIndicator() }
    OnData { value -> Text(value.toString()) }
    OnError { error -> Text(error.message ?: "Unknown error") }
}
```

## Scope

This project keeps the API focused on observation and rendering. It does not try to replace your
repository, state holder, or UI architecture.

## Saved screen state

[event-observer-state](modules/event-observer-state.md) connects repository operations to restorable View state. See the [Toolkit migration guide](migration-state.md).
