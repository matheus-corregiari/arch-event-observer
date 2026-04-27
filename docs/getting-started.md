# Getting Started

## Install The Right Module

Start with the base module:

```kotlin
dependencies {
    implementation("io.github.matheus-corregiari:event-observer:<version>")
}
```

Add the Compose module when your rendering layer is Compose Multiplatform:

```kotlin
dependencies {
    implementation("io.github.matheus-corregiari:event-observer-compose:<version>")
}
```

See the dedicated module pages for the split:

- [`event-observer`](modules/event-observer.md)
- [`event-observer-compose`](modules/event-observer-compose.md)

## Create A Result

`DataResult<T>` is the core model. Use the helpers to build the state you need.

```kotlin
val loading = dataResultLoading<String>()
val success = dataResultSuccess("Loaded")
val error = dataResultError<String>(RuntimeException("Boom"))
val none = dataResultNone<String>()
```

## Observe It In LiveData

`ResponseLiveData<T>` lets you emit `DataResult` values from coroutine code and observe them from
Android UI.

```kotlin
val userLiveData = responseLiveData<String> {
    emitLoading()
    emitData("Hello")
}

userLiveData.observe(this) {
    loading { isLoading -> println("loading=$isLoading") }
    data { value -> println(value) }
    error { throwable -> println(throwable.message) }
}
```

## Render It In Compose

Add `event-observer-compose`, then wrap a `Flow<DataResult<T>>` or `LiveData<DataResult<T>>` with
`composable` and render the states you need.

```kotlin
myFlow.composable.Unwrap {
    OnShowLoading { CircularProgressIndicator() }
    OnData { value -> Text(value.toString()) }
    OnError { error -> Text(error.message ?: "Unknown error") }
}
```

## Recommended Next Steps

- Read the module pages to choose the right dependency surface.
- Read [Core Concepts](core-concepts.md) to understand the state model.
- Use [Recipes](recipes.md) for common patterns.
- Check [API Reference](api/index.md) for per-module API docs.
- Check [Changelog](changelog/index.md) when you need release history.
