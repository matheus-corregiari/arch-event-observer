# Recipes

## Render A Loading Screen

Use the loading callbacks directly from `DataResult` or `ComposableDataResult`.

```kotlin
myFlow.composable
    .OnShowLoading { CircularProgressIndicator() }
    .OnHideLoading { Text("Done") }
    .Unwrap()
```

## Show Data Or Error

```kotlin
myFlow.composable
    .OnData { value -> Text(value.toString()) }
    .OnError { error -> Text(error.message ?: "Unknown error") }
    .Unwrap()
```

## React To List States

Use the list-aware callbacks when the payload is a collection, map, or sequence.

```kotlin
itemsFlow.composable
    .OnEmpty { Text("No items") }
    .OnNotEmpty { items -> Text("Items: ${items.size}") }
    .OnSingle { item -> Text("One item: $item") }
    .OnMany { items -> Text("Many items: ${items.size}") }
    .Unwrap()
```

## Convert A Coroutine Pipeline To LiveData

```kotlin
val liveData = responseLiveData<String> {
    emitLoading()
    val value = repository.loadValue()
    emitData(value)
}
```

You can then observe it with the wrapper DSL:

```kotlin
liveData.observe(this) {
    loading { isLoading -> println("loading=$isLoading") }
    data { value -> println(value) }
    error { throwable -> println(throwable.message) }
}
```

## Transform Existing Results

```kotlin
val reshaped = result.transform { value -> value.uppercase() }
val safe = result.orNone()
```

For `ResponseLiveData`, use `map`, `mapError`, `onNext`, and `onErrorReturn` when you need to
reshape state before rendering.

## Combine Multiple Sources

```kotlin
val combined = firstResponse.combine(secondResponse)
```

Use `combineNotNull` when both sides must have data, and `chainWith` when the second
`ResponseLiveData` depends on the first result.

## Wrap A Plain Result Into Compose

```kotlin
val state = dataResultSuccess("Ready").composable

state
    .OnData { Text(it) }
    .Unwrap()
```

## Practical Rule

Keep the smallest wrapper that matches the layer you are in. Use `DataResult` for the model,
`ResponseLiveData` or `ResponseFlow` for transport, and `ComposableDataResult` only where you
actually render Compose UI.
