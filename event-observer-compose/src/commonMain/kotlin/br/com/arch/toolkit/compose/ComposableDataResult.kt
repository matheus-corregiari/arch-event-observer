@file:Suppress(
    "ComposableNaming",
    "MagicNumber",
    "FunctionNaming",
    "TooManyFunctions",
    "FunctionName"
)

package br.com.arch.toolkit.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.arch.toolkit.compose.ComposableDataResult.AnimationConfig.Defaults.defaultEnterDuration
import br.com.arch.toolkit.compose.ComposableDataResult.AnimationConfig.Defaults.defaultExitDuration
import br.com.arch.toolkit.compose.ComposableDataResult.AnimationConfig.Defaults.enabledByDefault
import br.com.arch.toolkit.compose.observable.ComposeObservable
import br.com.arch.toolkit.flow.ResponseFlow
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
 *   .animation { enabled = true }
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
 *   enabled = true
 *   defaultEnterDuration = 300.milliseconds
 *   defaultExitDuration = 200.milliseconds
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
    private val wrapper = ObserveComposableWrapper<T>()
    private var notComposableBlock: (ObserveWrapper<T>.() -> Unit)? = null

    /**
     * Configures animation parameters for all subsequent composable callbacks that are
     * managed by this [ComposableDataResult] instance.
     *
     * Example:
     *
     * ```kotlin
     * comp.animation {
     *   enabled = true
     *   defaultEnterDuration = 300.milliseconds
     * }
     * ```
     *
     * By default, animations are enabled with predefined fade-in and fade-out transitions.
     * You can disable animations or customize the enter/exit transitions and their durations.
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
     * @param owner Optional [LifecycleOwner] for lifecycle-aware collection.
     * @param config DSL block on this [ComposableDataResult].
     */
    @Composable
    fun Unwrap(
        owner: LifecycleOwner? = LocalLifecycleOwner.current,
        config: ObserveComposableWrapper<T>.() -> Unit
    ) {
        val animationConfig = remember { animationConfig }
        val state: DataResult<T>? by if (owner != null) {
            result.collectAsStateWithLifecycle(result.valueOrNull(), owner)
        } else {
            result.collectAsState(result.valueOrNull())
        }
        val resultState = state ?: return

        LaunchedEffect(resultState) { resultState.unwrap { notComposableBlock?.invoke(this) } }
        wrapper.apply(config).list.forEachIndexed { index, observable ->
            if (animationConfig.enabled) {
                AnimatedVisibility(
                    label = "observable - ${index.toString().padStart(3, '0')}",
                    visible = observable.hasVisibleContent(resultState),
                    modifier = animationConfig.animationModifier,
                    enter = animationConfig.enterAnimation,
                    exit = animationConfig.exitAnimation,
                    content = { observable.Content(resultState) }
                )
            } else {
                if (observable.hasVisibleContent(resultState)) observable.Content(resultState)
            }
        }
        wrapper.clear()
    }
    //endregion

    /**
     * Holds animation configuration for the composable states managed by [ComposableDataResult].
     *
     * ---
     *
     * ### Behavior
     * - Controls whether animations are applied to [AnimatedVisibility] when rendering
     *   success, error, loading, or data states.
     * - Defines the default `Modifier`, enter, and exit transitions.
     * - Provides global defaults via [Defaults], which can be overridden before use.
     *
     * ---
     *
     * ### Example
     * ```kotlin
     * comp.animation {
     *   enabled = true
     *   enterAnimation = fadeIn(tween(300))
     *   exitAnimation = fadeOut(tween(200))
     * }
     * ```
     *
     * Or set global defaults once:
     * ```kotlin
     * ComposableDataResult.AnimationConfig.enabledByDefault = false
     * ComposableDataResult.AnimationConfig.defaultEnterDuration = 200.milliseconds
     * ComposableDataResult.AnimationConfig.defaultExitDuration = 200.milliseconds
     * ```
     *
     * ---
     *
     * @property enabled Whether animations are active for the composable blocks.
     *           Defaults to [Defaults.enabledByDefault].
     * @property animationModifier A [Modifier] applied to the `AnimatedVisibility` wrapper.
     * @property enterAnimation The [EnterTransition] for showing content.
     *           Defaults to a fade-in with a delay equal to the exit duration.
     * @property exitAnimation The [ExitTransition] for hiding content.
     *           Defaults to a simple fade-out.
     *
     * @see ComposableDataResult.animation
     * @see AnimatedVisibility
     */
    class AnimationConfig internal constructor() {
        var enabled: Boolean = enabledByDefault
        var animationModifier = Modifier
        var enterAnimation: EnterTransition = fadeIn(
            animationSpec = tween(
                durationMillis = defaultEnterDuration.toInt(DurationUnit.MILLISECONDS),
                // Delays enter to potentially run after a preceding exit animation completes
                delayMillis = defaultExitDuration.toInt(DurationUnit.MILLISECONDS)
            )
        )
        var exitAnimation: ExitTransition =
            fadeOut(
                animationSpec = tween(
                    durationMillis = defaultExitDuration.toInt(DurationUnit.MILLISECONDS)
                )
            )

        /**
         * Provides global defaults for [AnimationConfig].
         *
         * ---
         *
         * ### Behavior
         * - These values are applied whenever a new [AnimationConfig] is created.
         * - Can be changed globally to affect all instances.
         *
         * ---
         *
         * ### Example
         * ```kotlin
         * // Disable animations everywhere
         * ComposableDataResult.AnimationConfig.enabledByDefault = false
         *
         * // Faster transitions
         * ComposableDataResult.AnimationConfig.defaultEnterDuration = 150.milliseconds
         * ComposableDataResult.AnimationConfig.defaultExitDuration = 150.milliseconds
         * ```
         *
         * ---
         *
         * @property enabledByDefault Global flag to enable/disable animations.
         *           Default = `true`.
         * @property defaultEnterDuration Default duration for [enterAnimation].
         *           Default = `450.milliseconds`.
         * @property defaultExitDuration Default duration for [exitAnimation].
         *           Default = `450.milliseconds`.
         */
        companion object Defaults {
            var enabledByDefault = true
            var defaultEnterDuration: Duration = 450.milliseconds
            var defaultExitDuration: Duration = 450.milliseconds
        }
    }
}
