package br.com.arch.toolkit.eventObserver.state

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

/**
 * Computes an expensive projection on [workerDispatcher], retaining its last completed value.
 * Reading the returned StateFlow never invokes [transform]. New input cancels the old computation.
 * [scope] controls observation; [initialValue] is visible until the first computation completes.
 * Unlike [select], this projection updates asynchronously. Keep inputs immutable.
 * On JS/Wasm, a dispatcher does not provide a background thread: expensive work must cooperate
 * with the event loop or use an application-provided worker implementation.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun <A, B> StateFlow<A>.selectAsync(
    scope: CoroutineScope,
    initialValue: B,
    workerDispatcher: CoroutineDispatcher = Dispatchers.Default,
    transform: suspend (A) -> B
): StateFlow<B> = mapLatest { value ->
    withContext(workerDispatcher) { transform(value) }
}.stateIn(scope, SharingStarted.Eagerly, initialValue)
