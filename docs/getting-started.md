# Getting Started

## Install The Modules

Use the module you need:

```kotlin
dependencies {
    implementation("io.github.matheus-corregiari:event-observer:<version>")
    implementation("io.github.matheus-corregiari:event-observer-compose:<version>")
}
```

Inside this repository, use project dependencies:

```kotlin
dependencies {
    implementation(project(":event-observer"))
    implementation(project(":event-observer-compose"))
}
```

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

Wrap a `Flow<DataResult<T>>` or `LiveData<DataResult<T>>` with `composable` and render the states
you need.

```kotlin
myFlow.composable
    .OnShowLoading { CircularProgressIndicator() }
    .OnData { value -> Text(value.toString()) }
    .OnError { error -> Text(error.message ?: "Unknown error") }
    .Unwrap()
```

## Recommended Next Steps

- Read [Core Concepts](core-concepts.md) to understand the state model.
- Use [Recipes](recipes.md) for common patterns.
- Check [Changelog](changelog/index.md) when you need release history.
