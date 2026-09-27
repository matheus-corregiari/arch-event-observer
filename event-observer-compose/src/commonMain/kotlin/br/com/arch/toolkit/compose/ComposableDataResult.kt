@file:Suppress(
    "ComposableNaming",
    "MagicNumber",
    "FunctionNaming",
    "TooManyFunctions",
    "FunctionName"
)

package br.com.arch.toolkit.compose

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.arch.toolkit.compose.observable.ComposeObservable
import br.com.arch.toolkit.result.DataResult
import br.com.arch.toolkit.result.ObserveWrapper
import br.com.arch.toolkit.util.valueOrNull
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit

/**
 * Declarative Compose wrapper for observing a [ResponseFlow] of [DataResult].
 *
 * [ComposableDataResult] provides a **DSL-based** rendering logic to react to common states:
 * - **Loading** → [ObserveComposableWrapper.OnShowLoading], [ObserveComposableWrapper.OnHideLoading]
 * - **Error** → [ObserveComposableWrapper.OnError]
 * - **Success/Data** → [ObserveComposableWrapper.OnSuccess], [ObserveComposableWrapper.OnData]
 * - **Collections** → [ObserveComposableWrapper.OnEmpty], [ObserveComposableWrapper.OnNotEmpty], [ObserveComposableWrapper.OnSingle], [ObserveComposableWrapper.OnMany]
 *
 * The final rendering is triggered by [Unwrap], which collects the underlying flow
 * and dispatches the configured callbacks inside the [ObserveComposableWrapper] scope.
 *
 * ---
 *
 * ### Behavior
 * - Works with [Flow]s of [DataResult] (commonly [ResponseFlow]).
 * - Each state observer added in [Unwrap] adds a [ComposeObservable] to the pipeline.
 * - Supports optional **non-Compose side effects** via [outsideComposable].
 * - Provides **animations** for showing/hiding state blocks via [AnimationConfig].
 * - Uses [collectAsStateWithLifecycle] if a [LifecycleOwner] is available,
 *   falling back to [collectAsState].
 *
 * ---
 *
 * ### Example: Typical Usage
 * ```kotlin
 * myFlow.composable
 *   .animation { contentTransform = fadeIn() togetherWith fadeOut() }
 *   .outsideComposable { error { t -> log(t) } }
 *   .Unwrap {
 *     OnShowLoading { CircularProgressIndicator() }
 *     OnData { user -> Text("Hello ${user.name}") }
 *     OnError { e -> Text("Error: ${e.message}") }
 *   }
 * ```
 *
 * ### Example: Animations
 * ```kotlin
 * comp.animation {
 *   contentTransform = fadeIn() togetherWith fadeOut()
 * }
 * ```
 *
 * ---
 *
 * @param T The type of data wrapped in [DataResult].
 * @property result The [Flow] emitting [DataResult] values.
 *
 * @see ResponseFlow
 * @see DataResult
 * @see Unwrap
 * @see AnimationConfig
 */
@Stable
@ConsistentCopyVisibility
data class ComposableDataResult<T> internal constructor(val result: Flow<DataResult<T>>) {

    private val animationConfig = AnimationConfig()
    private var notComposableBlock: (ObserveWrapper<T>.() -> Unit)? = null

    /**
     * Configures animation parameters for all subsequent composable callbacks that are
     * managed by this [ComposableDataResult] instance.
     *
     * Example:
     *
     * ```kotlin
     * comp.animation {
     *   contentTransform = fadeIn() togetherWith fadeOut()
     * }
     * ```
     *
     * By default, animations are enabled with predefined fade-in and fade-out transitions.
     * You can disable animations by setting `contentTransform = null` or customize the enter/exit transitions.
     *
     * @param config A DSL block to customize the [AnimationConfig] for this instance.
     * @return This [ComposableDataResult] instance for chaining further configurations.
     * @see AnimationConfig
     */
    fun animation(config: AnimationConfig.() -> Unit) = apply { animationConfig.config() }

    /**
     * Attaches non-@Composable observation logic that runs outside the Compose scope.
     *
     * Use this to add side effects or loggers via an [ObserveWrapper].
     *
     * Example:
     *
     * ```kotlin
     * comp.outsideComposable {
     *   error { throwable -> logError(throwable) }
     * }
     * ```
     *
     * @param config receiver lambda on an [ObserveWrapper]<T> for traditional callbacks
     * @return this [ComposableDataResult] for chaining
     * @see ObserveWrapper
     */
    fun outsideComposable(config: ObserveWrapper<T>.() -> Unit) =
        apply { notComposableBlock = config }

    // region Unwrap

    /**
     * Configures the DSL before collection starts.
     *
     * ---
     *
     * ### Example
     * ```kotlin
     * comp.Unwrap {
     *   OnShowLoading { CircularProgressIndicator() }
     *   OnData { Text("Done!") }
     *   OnError { Text("Oops!") }
     * }
     * ```
     *
     * @param modifier Optional [Modifier] to apply to the layout root.
     * @param owner Optional [LifecycleOwner] for lifecycle-aware collection.
     * @param config DSL block on this [ComposableDataResult].
     */
    @Composable
    fun Unwrap(
        modifier: Modifier = Modifier,
        owner: LifecycleOwner? = LocalLifecycleOwner.current,
        config: ObserveComposableWrapper<T>.() -> Unit
    ) {
        val animationConfig = remember { animationConfig }
        val currentNotComposableBlock by rememberUpdatedState(notComposableBlock)
        val state: DataResult<T>? by if (owner != null) {
            result.collectAsStateWithLifecycle(result.valueOrNull(), owner)
        } else {
            result.collectAsState(result.valueOrNull())
        }
        val resultState = state ?: return

        LaunchedEffect(resultState) {
            resultState.unwrap { currentNotComposableBlock?.invoke(this) }
        }

        val listToRender = remember(config) {
            ObserveComposableWrapper<T>().apply(config).list
        }

        val transform = animationConfig.contentTransform
        if (transform != null) {
            AnimatedContent(
                targetState = resultState,
                modifier = modifier,
                transitionSpec = { transform },
                content = { result ->
                    for (item in listToRender) {
                        if (item.hasVisibleContent(result)) item.Content(result)
                    }
                }
            )
        } else {
            Box(modifier = modifier) {
                for (item in listToRender) {
                    if (item.hasVisibleContent(resultState)) item.Content(resultState)
                }
            }
        }
    }
    //endregion

    /**
     * Holds animation configuration for the composable states managed by [ComposableDataResult].
     *
     * ---
     *
     * ### Behavior
     * - Controls whether animations are applied to state transitions.
     * - Defines the default `contentTransform`, which can be customized.
     * - Provides global defaults via [Defaults], which can be overridden before use.
     *
     * ---
     *
     * ### Example
     * ```kotlin
     * comp.animation {
     *   contentTransform = slideInVertically() + fadeIn() togetherWith (slideOutVertically() + fadeOut())
     * }
     * ```
     *
     * Or set global defaults once:
     * ```kotlin
     * ComposableDataResult.AnimationConfig.defaultEnterDuration = 300.milliseconds
     * ComposableDataResult.AnimationConfig.defaultExitDuration = 200.milliseconds
     * ```
     *
     * ---
     *
     * @property contentTransform The [ContentTransform] for state transitions.
     *           If `null`, animations are disabled.
     *           Defaults to a fade-in/out transition.
     *
     * @see ComposableDataResult.animation
     */
    class AnimationConfig {
        var contentTransform: ContentTransform? = defaultContentTransform

        companion object Defaults {
            var defaultEnterDuration: Duration = 300.milliseconds
            var defaultExitDuration: Duration = 200.milliseconds

            var defaultContentTransform: ContentTransform? = fadeIn(
                animationSpec = tween(
                    durationMillis = defaultEnterDuration.toInt(DurationUnit.MILLISECONDS),
                    delayMillis = defaultExitDuration.toInt(DurationUnit.MILLISECONDS)
                )
            ) togetherWith fadeOut(
                animationSpec = tween(
                    durationMillis = defaultExitDuration.toInt(DurationUnit.MILLISECONDS)
                )
            )
        }
    }
}
