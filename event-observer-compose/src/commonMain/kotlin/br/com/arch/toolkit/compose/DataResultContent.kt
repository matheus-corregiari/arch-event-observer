@file:Suppress("ComposableNaming", "FunctionName")

package br.com.arch.toolkit.compose

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import br.com.arch.toolkit.compose.observable.ComposeObservable
import br.com.arch.toolkit.result.DataResult

/**
 * Declarative Compose component for rendering the state of a [DataResult].
 *
 * [DataResultContent] receives a static [DataResult] snapshot and dispatches
 * the configured callbacks inside the [DataResultContentScope] scope according to the status:
 * - **Loading** → [DataResultContentScope.OnShowLoading], [DataResultContentScope.OnHideLoading]
 * - **Error** → [DataResultContentScope.OnError]
 * - **Success/Data** → [DataResultContentScope.OnSuccess], [DataResultContentScope.OnData]
 * - **Collections** → [DataResultContentScope.OnEmpty], [DataResultContentScope.OnNotEmpty],
 *   [DataResultContentScope.OnSingle], [DataResultContentScope.OnMany]
 *
 * ---
 *
 * ### Example: Direct Usage in Screen/Preview
 * ```kotlin
 * // viewModel.state is a StateFlow<DataResult<User>>.
 * val resultState by viewModel.state.collectAsStateWithLifecycle()
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
 * @param content DSL block on [DataResultContentScope] defining state observers.
 *
 * @see DataResult
 * @see DataResultContentScope
 */
@Composable
fun <T> DataResultContent(
    result: DataResult<T>,
    modifier: Modifier = Modifier,
    transitionSpec: (AnimatedContentTransitionScope<DataResult<T>>.() -> ContentTransform)? = null,
    content: DataResultContentScope<T>.() -> Unit
) {
    val listToRender = remember(content) {
        DataResultContentScope<T>().apply(content).list
    }

    if (transitionSpec != null) {
        AnimatedContent(
            targetState = result,
            modifier = modifier,
            transitionSpec = transitionSpec,
            contentKey = { it.animationContentKey() },
            content = { currentResult ->
                RenderVisibleContent(listToRender, currentResult)
            }
        )
    } else {
        Box(modifier = modifier) {
            RenderVisibleContent(listToRender, result)
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
 * // viewModel.state is a StateFlow<DataResult<User>>.
 * val resultState by viewModel.state.collectAsStateWithLifecycle()
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
 * @param content DSL block on [DataResultContentScope] defining state observers.
 *
 * @see DataResultContent
 */
@Composable
fun <T> DataResult<T>.Content(
    modifier: Modifier = Modifier,
    transitionSpec: (AnimatedContentTransitionScope<DataResult<T>>.() -> ContentTransform)? = null,
    content: DataResultContentScope<T>.() -> Unit
) = DataResultContent(
    result = this,
    modifier = modifier,
    transitionSpec = transitionSpec,
    content = content
)

@Composable
@Suppress("FunctionNaming")
private fun <T> RenderVisibleContent(
    observables: List<ComposeObservable<T, *>>,
    result: DataResult<T>
) {
    val visibleContent = remember(result, observables) {
        observables.withIndex().filter { it.value.hasVisibleContent(result) }
    }
    visibleContent.forEach { (index, observable) ->
        // Keep registration identity when preceding observers become hidden.
        key(index) {
            observable.Content(result)
        }
    }
}
