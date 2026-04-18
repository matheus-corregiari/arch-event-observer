# Arch Event Observer

Kotlin-first event and result observation for Android and Compose Multiplatform.

The project is split into two public modules:

- `event-observer` for `DataResult`, `ResponseLiveData`, `ResponseFlow`, and the supporting
  utilities
- `event-observer-compose` for `ComposableDataResult` and the Compose-facing observation DSL

[![License][badge-license]](/LICENSE)
[![Kotlin][badge-kotlin]](https://kotlinlang.org)
![Lint][badge-lint]
![Test][badge-test]
[![Coverage][badge-coverage]][link-coverage]

## Overview

The library centers on `DataResult<T>` and a small set of wrappers that keep loading, success,
error, and list-state handling consistent across Android and Compose layers.

Use `event-observer` when you want:

- `DataResult` helpers and status handling
- `ResponseLiveData`, `MutableResponseLiveData`, and `SwapResponseLiveData`
- `ResponseFlow`, `ResponseStateFlow`, and `ResponseSharedFlow`
- chaining, mapping, and merge helpers for reactive state

Use `event-observer-compose` when you want:

- `ComposableDataResult` for declarative state rendering
- `collectAsComposableState()` for `Flow<DataResult<T>>` and `LiveData<DataResult<T>>`
- Compose observables such as `OnData`, `OnError`, `OnShowLoading`, `OnEmpty`, `OnNotEmpty`,
  `OnSingle`, and `OnMany`
- a Compose-first API on top of `event-observer`

## Installation

Pick the module that matches your layer:

```kotlin
dependencies {
    implementation("io.github.matheus-corregiari:arch-event-observer:<version>")
}
```

```kotlin
dependencies {
    implementation("io.github.matheus-corregiari:arch-event-observer-compose:<version>")
}
```

`event-observer-compose` builds on top of `event-observer`, so use both only when you need
Compose rendering on top of the base result model.

## Module Guide

`event-observer`

- base result model and status helpers
- LiveData and Flow wrappers
- transformations, chaining, and merge helpers

`event-observer-compose`

- Compose observation DSL
- `ComposableDataResult`
- Compose rendering hooks for loading, data, error, and collection states

Use only `event-observer` when your UI layer is not Compose. Add `event-observer-compose` when the
final observation point lives inside Jetpack Compose Multiplatform.

## Getting Started

The fastest path is to start with `DataResult` and render it where you need it.

```kotlin
val result = dataResultSuccess("Hello")

result.unwrap {
    data { value -> println(value) }
    error { throwable -> println(throwable.message) }
    loading { isLoading -> println("loading=$isLoading") }
}
```

For Compose, convert the upstream state into a `ComposableDataResult` and render the blocks you care
about.

```kotlin
myFlow.composable
    .OnShowLoading { CircularProgressIndicator() }
    .OnData { value -> Text(value.toString()) }
    .OnError { error -> Text(error.message ?: "Unknown error") }
    .Unwrap()
```

## Documentation

Public docs live in `docs/`:

- [Home](docs/index.md)
- [Getting Started](docs/getting-started.md)
- [event-observer](docs/modules/event-observer.md)
- [event-observer-compose](docs/modules/event-observer-compose.md)
- [Core Concepts](docs/core-concepts.md)
- [Recipes](docs/recipes.md)
- [Changelog](docs/changelog/index.md)
- [Contributing](docs/contributing.md)
- [API Reference](docs/api/index.md)

The published MkDocs site is built from the same content and mirrors these pages.

## Platform Notes

- `event-observer` is Android-facing and integrates with LiveData.
- `event-observer-compose` builds on top of `event-observer`, Flow, and Compose state.
- The API is designed to stay predictable in shared KMP-oriented architecture layers.

## Contributing

Read [CONTRIBUTING.md](CONTRIBUTING.md) before sending changes.

## License

```text
Copyright 2025 Matheus Corregiari

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

[link-coverage]: https://codecov.io/gh/matheus-corregiari/arch-event-observer

[badge-kotlin]: https://img.shields.io/badge/kotlin-2.3.20-blue.svg?logo=kotlin

[badge-license]: https://img.shields.io/github/license/matheus-corregiari/arch-event-observer

[badge-coverage]: https://codecov.io/gh/matheus-corregiari/arch-event-observer/graph/badge.svg?token=146UU167K6

[badge-lint]: https://github.com/matheus-corregiari/arch-event-observer/actions/workflows/lint.yml/badge.svg

[badge-test]: https://github.com/matheus-corregiari/arch-event-observer/actions/workflows/coverage.yml/badge.svg
