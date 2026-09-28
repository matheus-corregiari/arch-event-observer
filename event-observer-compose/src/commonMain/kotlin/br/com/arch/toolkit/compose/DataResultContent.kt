@file:Suppress("ComposableNaming", "FunctionName")

package br.com.arch.toolkit.compose

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import br.com.arch.toolkit.result.DataResult

/**
 * Declarative Compose component for rendering the state of a [DataResult].
 *
 * [DataResultContent] receives a static [DataResult] snapshot and dispatches
 * the configured callbacks inside the [ObserveComposableWrapper] scope according to the status:
 * - **Loading** → [ObserveComposableWrapper.OnShowLoading], [ObserveComposableWrapper.OnHideLoading]
 * - **Error** → [ObserveComposableWrapper.OnError]
 * - **Success/Data** → [ObserveComposableWrapper.OnSuccess], [ObserveComposableWrapper.OnData]
 * - **Collections** → [ObserveComposableWrapper.OnEmpty], [ObserveComposableWrapper.OnNotEmpty],
 *   [ObserveComposableWrapper.OnSingle], [ObserveComposableWrapper.OnMany]
 *
 * ---
 *
 * ### Example: Direct Usage in Screen/Preview
 * ```kotlin
 * val resultState by viewModel.flow.collectAsStateWithLifecycle()
 *
 * DataResultContent(result = resultState) {
 *     OnShowLoading { CircularProgressIndicator() }
 *     OnData { user -> Text("Hello ${user.name}") }
 *     OnError { e -> Text("Error: ${e.message}") }
 * }
 * ```
 *
 * @param T The type of data wrapped in [DataResult].
 * @param result The [DataResult] snapshot to render.
 * @param modifier Optional [Modifier] to apply to the layout root.
 * @param transitionSpec Optional transition animation spec for state changes. Defaults to `null` (no animation).
 * @param content DSL block on [ObserveComposableWrapper] defining state observers.
 *
 * @see DataResult
 * @see ObserveComposableWrapper
 */
@Composable
fun <T> DataResultContent(
    result: DataResult<T>,
    modifier: Modifier = Modifier,
    transitionSpec: (AnimatedContentTransitionScope<DataResult<T>>.() -> ContentTransform)? = null,
    content: ObserveComposableWrapper<T>.() -> Unit
) {
    val listToRender = remember(content) {
        ObserveComposableWrapper<T>().apply(content).list
    }

    if (transitionSpec != null) {
        AnimatedContent(
            targetState = result,
            modifier = modifier,
            transitionSpec = transitionSpec,
            content = { currentResult ->
                for (item in listToRender) {
                    if (item.hasVisibleContent(currentResult)) item.Content(currentResult)
                }
            }
        )
    } else {
        Box(modifier = modifier) {
            for (item in listToRender) {
                if (item.hasVisibleContent(result)) item.Content(result)
            }
        }
    }
}

/**
 * Declarative Compose extension for rendering this [DataResult] instance.
 *
 * ---
 *
 * ### Example: Extension Usage
 * ```kotlin
 * val resultState by viewModel.flow.collectAsStateWithLifecycle()
 *
 * resultState.Content {
 *     OnShowLoading { CircularProgressIndicator() }
 *     OnData { user -> Text("Hello ${user.name}") }
 *     OnError { e -> Text("Error: ${e.message}") }
 * }
 * ```
 *
 * @param T The type of data wrapped in [DataResult].
 * @param modifier Optional [Modifier] to apply to the layout root.
 * @param transitionSpec Optional transition animation spec for state changes. Defaults to `null` (no animation).
 * @param content DSL block on [ObserveComposableWrapper] defining state observers.
 *
 * @see DataResultContent
 */
@Composable
fun <T> DataResult<T>.Content(
    modifier: Modifier = Modifier,
    transitionSpec: (AnimatedContentTransitionScope<DataResult<T>>.() -> ContentTransform)? = null,
    content: ObserveComposableWrapper<T>.() -> Unit
) = DataResultContent(
    result = this,
    modifier = modifier,
    transitionSpec = transitionSpec,
    content = content
)
