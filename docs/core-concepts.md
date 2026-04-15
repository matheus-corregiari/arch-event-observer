# Core Concepts

## `DataResult`

`DataResult<T>` carries three things at once:

- `data`
- `error`
- `status`

The status values are:

- `NONE` for a neutral, empty result
- `LOADING` while work is in progress
- `SUCCESS` when data is ready
- `ERROR` when the operation failed

The class also exposes convenience flags such as `isSuccess`, `isError`, `isLoading`, `isNone`,
`hasData`, `hasError`, and list-oriented checks like `isEmpty`, `isNotEmpty`, `hasOneItem`, and
`hasManyItems`.

## Event Filters

`EventDataStatus` controls whether a callback should fire based on data presence.

- `WithData`
- `WithoutData`
- `DoesNotMatter`

Use it when a callback should only react to a value-bearing result, a value-less result, or both.

## Wrapper DSL

`ObserveWrapper<T>` is the callback DSL behind `unwrap { ... }` and the LiveData observation
helpers.

It lets you react to:

- `data`
- `loading`
- `showLoading`
- `hideLoading`
- `error`
- `success`
- `result`
- `status`
- `empty`
- `notEmpty`
- `oneItem`
- `manyItems`
- `none`

The wrapper keeps the control flow small: define what you care about, then attach it to a
`DataResult`.

## LiveData And Flow

`ResponseLiveData<T>` and `ResponseFlow<T>` wrap reactive sources that emit `DataResult<T>`.

They exist for different layers:

- `ResponseLiveData` fits Android UI code that still uses LiveData
- `ResponseFlow` fits coroutine-driven state pipelines and shared state

Both expose helpers for:

- mapping data
- mapping errors
- converting from plain `Flow`
- keeping derived state in sync

`ResponseStateFlow<T>` and `ResponseSharedFlow<T>` give you stateful or shared flow behavior when
you need it.

## Compose

`ComposableDataResult<T>` turns a `Flow<DataResult<T>>` into a declarative Compose DSL.

It renders common blocks such as:

- `OnData`
- `OnError`
- `OnShowLoading`
- `OnHideLoading`
- `OnSuccess`
- `OnStatus`
- `OnEmpty`
- `OnNotEmpty`
- `OnSingle`
- `OnMany`

It also supports optional animation configuration and non-Compose side effects through
`outsideComposable`.

## Design Rule

The library stays opinionated about result shape, but it stays out of your app structure. It gives
you the primitives and leaves the screen architecture to you.
