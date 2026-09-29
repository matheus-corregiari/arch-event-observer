package br.com.arch.toolkit.compose.observable

import androidx.compose.runtime.Composable
import br.com.arch.toolkit.result.DataResult

/**
 * Selects and renders content from the current [DataResult] snapshot.
 *
 * Implementations evaluate visibility without collecting a flow and pass the
 * snapshot values to their registered composable content.
 *
 * @param T the type of the data carried by the source [DataResult]
 * @param R the type of the content passed to Compose views
 */
internal sealed class ComposeObservable<T, R> {
    /**
     * Determines whether this observable currently has content to display.
     *
     * @param result the [DataResult] being evaluated
     */
    abstract fun hasVisibleContent(result: DataResult<T>): Boolean

    /**
     * Invoked inside a @Composable context to render UI based on [result].
     *
     * Implementations render values directly from this snapshot.
     */
    @Composable
    @Suppress("FunctionNaming")
    abstract fun Content(result: DataResult<T>)
}
