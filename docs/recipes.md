# Recipes

## Render A Loading Screen

Use the loading callbacks directly from `DataResult` or, in the Compose module,
`ComposableDataResult`.

```kotlin
myFlow.composable.Unwrap {
    OnShowLoading { CircularProgressIndicator() }
    OnHideLoading { Text("Done") }
}
```

## Show Data Or Error

```kotlin
myFlow.composable.Unwrap {
    OnData { value -> Text(value.toString()) }
    OnError { error -> Text(error.message ?: "Unknown error") }
}
```

## React To List States

Use the list-aware callbacks when the payload is a collection, map, or sequence.

```kotlin
itemsFlow.composable.Unwrap {
    OnEmpty { Text("No items") }
    OnNotEmpty { items -> Text("Items: ${items.size}") }
    OnSingle { item -> Text("One item: $item") }
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
dataResultSuccess("Ready").composable.Unwrap {
    OnData { data -> Text(data) }
}
```

## Custom Animations in Compose

Animations are handled via the `contentTransform` property in `AnimationConfig`.
- If `contentTransform` is `null`, animations are disabled, and the content is rendered directly.
- If `contentTransform` is non-null, it uses `AnimatedContent` for smooth transitions.

```kotlin
myFlow.composable
    .animation {
        contentTransform = slideInVertically() + fadeIn() togetherWith (slideOutVertically() + fadeOut())
    }
    .Unwrap(modifier = Modifier.padding(16.dp)) {
        OnData { data -> Text(data) }
    }
```

To disable animations globally:

```kotlin
ComposableDataResult.AnimationConfig.defaultContentTransform = null
```

## Side Effects with Compose Observers

If you need to log errors or trigger analytics while using the Compose DSL, use `outsideComposable`:

```kotlin
myFlow.composable
    .outsideComposable {
        error { t -> Analytics.logError(t) }
        data { d -> Analytics.logView(d) }
    }
    .Unwrap {
        OnData { data -> Text(data) }
    }
```

## Practical Rule

Keep the smallest wrapper that matches the layer you are in. Use `DataResult` for the model,
`ResponseLiveData` or `ResponseFlow` for transport, and `ComposableDataResult` only where you
actually render Compose UI.

For module-specific API details, use the [API Reference](api/index.md).
