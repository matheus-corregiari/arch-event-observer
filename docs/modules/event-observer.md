# `event-observer`

`event-observer` is the base module. It owns the result model, status handling, and the reactive
wrappers that move `DataResult<T>` through Android and coroutine layers.

## Install

```kotlin
dependencies {
    implementation("io.github.matheus-corregiari:event-observer:<version>")
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

## Typed error observers

Filter errors by exception type using a reified type or `KClass`:

```kotlin
result.unwrap {
    error<IllegalStateException> { error -> Logger.log(error) }
    error(IllegalArgumentException::class) { error -> Logger.log(error) }
    error { error: Throwable -> Logger.log(error) }
}
```

Handlers are additive: the untyped handler still runs when a typed handler matches.
Subclasses match their parent exception type. A typed handler skips null errors and
non-error states; `single` and `dataStatus` filters still apply.

The same typed overloads are available on `DataResult.error`. Use explicit lambda
parameters (`{ error -> ... }`) or an explicit empty parameter list (`{ -> ... }`)
to distinguish callbacks with and without an argument.

## API Reference

- [Base module API](../api/event-observer.md)

### Failures inside observer callbacks

If a callback throws, recovery requires an error observer whose exception type and
`dataStatus` match the replayed error result. Recovery uses an error result without
data, so `WithData` handlers cannot handle it. A nonmatching typed handler does not
silently consume the exception: the wrapper reports `DataResultException` with the
original failure in `error`. An untyped handler can serve as a fallback.
