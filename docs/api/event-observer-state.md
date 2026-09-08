# event-observer-state API

Package: `br.com.arch.toolkit.eventObserver.state`.

- `SavedStateHandle.saveState` and `saveResponseState`: ViewModel delegates.
- `SavedStateHandle.value` / `StateValue`: directly saved property values.
- `ViewModelState.Regular`: plain operations and saved payloads.
- `ViewModelState.Result`: result operations with transient status.
- `StateFlow.select`: synchronous derived state without another saved copy.
- `StateFlow.selectAsync` / `ViewModelState.selectAsync`: worker-computed, retained projections.
- `ViewModelState.setAsync`: prepares a direct saved update on the worker dispatcher.

[Generated reference](event-observer-state/html/index.html)

See the [module guide](../modules/event-observer-state.md) and [migration guide](../migration-state.md).
