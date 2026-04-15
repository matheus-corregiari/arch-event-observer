package br.com.arch.toolkit.util

import br.com.arch.toolkit.result.DataResult
import br.com.arch.toolkit.result.ObserveWrapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Collects a flow of [DataResult] values through an [ObserveWrapper].
 */
suspend fun <T> Flow<DataResult<T>>.unwrap(
    config: ObserveWrapper<T>.() -> Unit
) {
    val wrapper = ObserveWrapper<T>().apply(config)
    collect { wrapper.suspendFunc { handleResult(it) } }
}

/**
 * Returns the current value for state or shared flows, or `null` otherwise.
 */
fun <T> Flow<T>.valueOrNull(): T? = when (this) {
    is StateFlow<T> -> value
    is SharedFlow<T> -> replayCache.lastOrNull()
    else -> null
}
