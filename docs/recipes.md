# Recipes

## Render A Loading Screen

Use the loading callbacks directly from `DataResult` or, in the Compose module,
`ComposableDataResult`.

```kotlin
myFlow.Unwrap {
    OnShowLoading { CircularProgressIndicator() }
    OnHideLoading { Text("Done") }
}
```

## Show Data Or Error

```kotlin
myFlow.Unwrap {
    OnData { value -> Text(value.toString()) }
    OnError { error -> Text(error.message ?: "Unknown error") }
}
```

## React To List States

Use the list-aware callbacks when the payload is a collection, map, or sequence.

```kotlin
itemsFlow.Unwrap {
    OnEmpty { -> Text("No items") }
    OnNotEmpty { items -> Text("Items: ${items.size}") }
    OnSingle<String> { item -> Text("One item: $item") }
    OnMany { items -> Text("Many items: ${items.size}") }
}
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
dataResultSuccess("Ready").Content {
    OnData { data -> Text(data) }
}
```

## Custom Animations in Compose

Pass an optional `transitionSpec` to animate structural state changes with `AnimatedContent`.
The default `null` renders content without animation.

```kotlin
myFlow.Unwrap(
    modifier = Modifier.padding(16.dp),
    transitionSpec = {
        slideInVertically() + fadeIn() togetherWith (slideOutVertically() + fadeOut())
    }
) {
    OnData { data -> Text(data) }
}
```

## Side Effects with Compose Observers

Handle side effects explicitly with `LaunchedEffect`. Collect the state once and share
the same snapshot between the effect and `DataResultContent`. Here,
`viewModel.state` is a `StateFlow<DataResult<T>>`:

```kotlin
val resultState by viewModel.state.collectAsStateWithLifecycle()

LaunchedEffect(resultState) {
    if (resultState.isError) {
        resultState.error?.let { Logger.log(it) }
    }
}

DataResultContent(result = resultState) {
    OnData { data -> Text(data.toString()) }
}
```

The effect restarts when its key changes and is cancelled when it leaves composition.

## Practical Rule

Keep the smallest wrapper that matches the layer you are in. Use `DataResult` for the model,
`ResponseLiveData` or `ResponseFlow` for transport, and `ComposableDataResult` only where you
actually render Compose UI.

For module-specific API details, use the [API Reference](api/index.md).
