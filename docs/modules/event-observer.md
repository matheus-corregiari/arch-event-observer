# `event-observer`

`event-observer` is the base module. It owns the result model, status handling, and the reactive
wrappers that move `DataResult<T>` through Android and coroutine layers.

## Install

```kotlin
dependencies {
    implementation("io.github.matheus-corregiari:arch-event-observer:<version>")
}
```

## What Lives Here

- `DataResult<T>`
- `DataResultStatus`
- `EventDataStatus`
- `ResponseLiveData`
- `MutableResponseLiveData`
- `SwapResponseLiveData`
- `ResponseFlow`
- `ResponseStateFlow`
- `ResponseSharedFlow`

## Use It When

- you need the base observation model without Compose
- you want LiveData support in Android UI layers
- you want Flow-based state transport with the same result semantics

## Example

```kotlin
val userState = responseLiveData<String> {
    emitLoading()
    emitData("Ready")
}

userState.observe(this) {
    loading { println("loading") }
    data { value -> println(value) }
    error { throwable -> println(throwable.message) }
}
```

## API Reference

- [Base module API](../api/event-observer.md)
