# `event-observer-compose`

`event-observer-compose` is the UI-facing module for Jetpack Compose Multiplatform. It layers a
declarative rendering DSL on top of `event-observer`.

## Install

```kotlin
dependencies {
    implementation("io.github.matheus-corregiari:arch-event-observer-compose:<version>")
}
```

This module depends on `event-observer`, so the base result model comes with it.

## What Lives Here

- `ComposableDataResult`
- `collectAsComposableState()`
- `OnData`
- `OnError`
- `OnShowLoading`
- `OnHideLoading`
- `OnEmpty`
- `OnNotEmpty`
- `OnSingle`
- `OnMany`

## Use It When

- your final observation point is a Compose screen
- you want to replace manual `when` branches with state callbacks
- you need Compose-specific rendering and side-effect hooks

## Example

```kotlin
userFlow.composable
    .OnShowLoading { CircularProgressIndicator() }
    .OnData { value -> Text(value) }
    .OnError { error -> Text(error.message ?: "Unknown error") }
    .Unwrap()
```

## API Reference

- [Compose module API](../api/event-observer-compose.md)
