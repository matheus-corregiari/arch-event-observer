package br.com.arch.toolkit.compose

import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import br.com.arch.toolkit.flow.ResponseFlow
import br.com.arch.toolkit.result.DataResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Wraps this synchronous [DataResult] into a [ComposableDataResult],
 * enabling declarative observation of `loading`, `success`, and `error` states
 * inside Jetpack Compose.
 *
 * ---
 *
 * ### Behavior
 * - Converts the current [DataResult] into a [MutableStateFlow].
 * - Exposes declarative Compose callbacks via [ComposableDataResult.Unwrap].
 * - Useful for cases where you already have a `DataResult` and want to render
 *   its state in a Composable tree.
 *
 * ---
 *
 * ### Example
 * ```kotlin
 * myDataResult.composable.Unwrap {
 *     OnShowLoading { CircularProgressIndicator() }
 *     OnData { data -> Text("Data: $data") }
 *     OnError { error -> Text("Error: ${error.message}") }
 * }
 * ```
 *
 * @receiver A synchronous [DataResult] instance.
 * @return A [ComposableDataResult] wrapping this [DataResult].
 *
 * @see ComposableDataResult
 * @see Flow.composable
 */
val <T> DataResult<T>.composable: ComposableDataResult<T> get() = MutableStateFlow(this).composable

/**
 * Wraps a [Flow] of [DataResult] (a [ResponseFlow]) into a [ComposableDataResult],
 * providing Compose-aware callbacks for loading, success, and error handling.
 *
 * ---
 *
 * ### Behavior
 * - Reactively listens to the upstream [Flow].
 * - Converts each [DataResult] emission into a Compose-friendly wrapper.
 * - Allows defining declarative UI blocks inside [ComposableDataResult.Unwrap].
 *
 * ---
 *
 * ### Example
 * ```kotlin
 * myFlow.composable.Unwrap {
 *   OnShowLoading { CircularProgressIndicator() }
 *   OnData { Text("Loaded: $it") }
 *   OnError { t -> Text("Error: ${t.message}") }
 * }
 * ```
 *
 * @receiver A [Flow] of [DataResult] (commonly a [ResponseFlow]).
 * @return A [ComposableDataResult] to bind Compose UI with reactive data.
 *
 * @see ComposableDataResult
 * @see DataResult.composable
 */
val <T> Flow<DataResult<T>>.composable: ComposableDataResult<T> get() = ComposableDataResult(this)

/**
 * Collects a [Flow] of [DataResult] (a [ResponseFlow]) into a Compose [State],
 * where the [State.value] is always a [ComposableDataResult].
 *
 * ---
 *
 * ### Behavior
 * - Uses [derivedStateOf] to recompose only when upstream data changes.
 * - Useful when you want to reuse the [ComposableDataResult] in multiple
 *   Composables without re-collecting the flow each time.
 *
 * ---
 *
 * ### Example
 * ```kotlin
 * val compState by myFlow.collectAsComposableState()
 *
 * compState.Unwrap {
 *   OnShowLoading { CircularProgressIndicator() }
 *   OnData { user -> Text("Hello ${user.name}") }
 *   OnError { e -> Text("Oops: ${e.message}") }
 * }
 * ```
 *
 * @receiver The [Flow] of [DataResult] to collect.
 * @return A Compose [State] whose value is the latest [ComposableDataResult].
 *
 * @see ComposableDataResult
 * @see Flow.composable
 */
fun <T> Flow<DataResult<T>>.collectAsComposableState() = derivedStateOf { composable }
