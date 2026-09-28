@file:Suppress(
    "ComposableNaming",
    "MagicNumber",
    "FunctionNaming",
    "TooManyFunctions",
    "FunctionName",
    "LongParameterList"
)

package br.com.arch.toolkit.compose

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.arch.toolkit.result.DataResult
import br.com.arch.toolkit.result.ObserveWrapper
import br.com.arch.toolkit.util.valueOrNull
import kotlinx.coroutines.flow.Flow

/**
 * Top-level Composable function for collecting and rendering a [Flow] of [DataResult].
 *
 * ---
 *
 * ### Example
 * ```kotlin
 * ComposableDataResult(flow = viewModel.flow) {
 *     OnShowLoading { CircularProgressIndicator() }
 *     OnData { user -> Text("Hello ${user.name}") }
 *     OnError { e -> Text("Error: ${e.message}") }
 * }
 * ```
 *
 * @param T The type of data wrapped in [DataResult].
 * @param flow The [Flow] emitting [DataResult] values.
 * @param modifier Optional [Modifier] to apply to the layout root.
 * @param owner Optional [LifecycleOwner] for lifecycle-aware collection.
 * @param transitionSpec Optional transition animation spec for state changes. Defaults to `null`.
 * @param outsideComposable Optional non-composable side effects block.
 * @param content DSL block on [ObserveComposableWrapper] defining state observers.
 */
@Composable
fun <T> ComposableDataResult(
    flow: Flow<DataResult<T>>,
    modifier: Modifier = Modifier,
    owner: LifecycleOwner? = LocalLifecycleOwner.current,
    transitionSpec: (AnimatedContentTransitionScope<DataResult<T>>.() -> ContentTransform)? = null,
    outsideComposable: (ObserveWrapper<T>.() -> Unit)? = null,
    content: ObserveComposableWrapper<T>.() -> Unit
) {
    val currentOutsideComposable by rememberUpdatedState(outsideComposable)
    val state: DataResult<T>? by if (owner != null) {
        flow.collectAsStateWithLifecycle(flow.valueOrNull(), owner)
    } else {
        flow.collectAsState(flow.valueOrNull())
    }
    val resultState = state ?: return

    LaunchedEffect(resultState) {
        resultState.unwrap { currentOutsideComposable?.invoke(this) }
    }

    DataResultContent(
        result = resultState,
        modifier = modifier,
        transitionSpec = transitionSpec,
        content = content
    )
}

/**
 * Extension function for collecting a [Flow] of [DataResult] and declaratively
 * rendering its state using Compose observers.
 *
 * ---
 *
 * ### Example
 * ```kotlin
 * viewModel.flow.Unwrap {
 *     OnShowLoading { CircularProgressIndicator() }
 *     OnData { user -> Text("Hello ${user.name}") }
 *     OnError { e -> Text("Error: ${e.message}") }
 * }
 * ```
 *
 * @param T The type of data wrapped in [DataResult].
 * @param modifier Optional [Modifier] to apply to the layout root.
 * @param owner Optional [LifecycleOwner] for lifecycle-aware collection.
 * @param transitionSpec Optional transition animation spec for state changes. Defaults to `null`.
 * @param outsideComposable Optional non-composable side effects block.
 * @param content DSL block on [ObserveComposableWrapper] defining state observers.
 */
@Composable
fun <T> Flow<DataResult<T>>.Unwrap(
    modifier: Modifier = Modifier,
    owner: LifecycleOwner? = LocalLifecycleOwner.current,
    transitionSpec: (AnimatedContentTransitionScope<DataResult<T>>.() -> ContentTransform)? = null,
    outsideComposable: (ObserveWrapper<T>.() -> Unit)? = null,
    content: ObserveComposableWrapper<T>.() -> Unit
) = ComposableDataResult(
    flow = this,
    modifier = modifier,
    owner = owner,
    transitionSpec = transitionSpec,
    outsideComposable = outsideComposable,
    content = content
)
