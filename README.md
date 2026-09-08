# Arch Event Observer

Kotlin-first event and result observation for Android and Compose Multiplatform.

The project is split into three public modules:

- `event-observer` for `DataResult`, `ResponseLiveData`, `ResponseFlow`, and the supporting
  utilities
- `event-observer-compose` for `ComposableDataResult` and the Compose-facing observation DSL
- `event-observer-state` for serializable SavedStateHandle state, repository operations and projections

[![License][badge-license]](/LICENSE)
[![Kotlin][badge-kotlin]](https://kotlinlang.org)
[![Latest release][badge-release]][link-release]
![Lint][badge-lint]
![Test][badge-test]
[![Coverage][badge-coverage]][link-coverage]

## Requirements

- Kotlin `2.4.10`
- Gradle wrapper `9.7.1`
- JDK `21` via the Gradle toolchain
- Android `minSdk 23` and `compileSdk 37`
- Compose Multiplatform `1.12.0` with iOS ARM targets
- Use the project wrapper instead of a local Gradle install

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

Use `event-observer-state` when you want:

- shared Repository-to-ViewModel state backed by `SavedStateHandle`
- objects, lists and maps, with or without `DataResult`
- refresh, filters, continuous streams and derived UI state

## Installation

Pick the module that matches your layer:

```kotlin
dependencies {
    implementation("io.github.matheus-corregiari:event-observer:<version>")
}
```

```kotlin
dependencies {
    implementation("io.github.matheus-corregiari:event-observer-compose:<version>")
}
```

`event-observer-compose` builds on top of `event-observer`, so use both only when you need
Compose rendering on top of the base result model.

For saved state in shared code:

```kotlin
kotlin {
    sourceSets.commonMain.dependencies {
        implementation("io.github.matheus-corregiari:event-observer-state:<version>")
    }
}
```

Apply the Kotlin serialization plugin in the module declaring your `@Serializable` models.
The state artifact includes the base result API; add the Compose artifact for its rendering DSL.

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

For saved state, declare a shared ViewModel and start a new operation on refresh:

```kotlin
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import br.com.arch.toolkit.eventObserver.state.saveResponseState
import br.com.arch.toolkit.result.DataResult
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable
data class User(val name: String)

class UsersViewModel(handle: SavedStateHandle) : ViewModel() {
    val users by handle.saveResponseState<List<User>>()
    val count = users.select { it?.size ?: 0 }

    fun refresh(source: () -> Flow<DataResult<List<User>>>) = users.load { source() }
}
```

Observe `users.flow()` from the View. Its last payload survives completed requests and is restored
through the handle's owner. See the [state guide](docs/modules/event-observer-state.md) for filters,
plain values, mapping and platform restoration limits.

## Documentation

Public docs live in `docs/`:

- [Home](docs/index.md)
- [Getting Started](docs/getting-started.md)
- [event-observer](docs/modules/event-observer.md)
- [event-observer-compose](docs/modules/event-observer-compose.md)
- [event-observer-state](docs/modules/event-observer-state.md)
- [State migration](docs/migration-state.md)
- [State performance and responsiveness](docs/state-performance.md)
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
[link-release]: https://github.com/matheus-corregiari/arch-event-observer/releases/latest

[badge-kotlin]: https://img.shields.io/badge/kotlin-2.4.10-blue.svg?logo=kotlin
[badge-release]: https://img.shields.io/github/v/release/matheus-corregiari/arch-event-observer

[badge-license]: https://img.shields.io/github/license/matheus-corregiari/arch-event-observer

[badge-coverage]: https://codecov.io/gh/matheus-corregiari/arch-event-observer/graph/badge.svg?token=146UU167K6

[badge-lint]: https://github.com/matheus-corregiari/arch-event-observer/actions/workflows/ci.yml/badge.svg

[badge-test]: https://github.com/matheus-corregiari/arch-event-observer/actions/workflows/ci.yml/badge.svg


## Saved screen state

See [event-observer-state](docs/modules/event-observer-state.md) for one-shot requests, refresh, filters, continuous streams and transformations. Existing Toolkit consumers should follow the [migration guide](docs/migration-state.md).
