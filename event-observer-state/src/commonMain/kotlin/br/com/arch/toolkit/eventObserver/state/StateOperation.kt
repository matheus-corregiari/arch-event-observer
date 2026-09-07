package br.com.arch.toolkit.eventObserver.state

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

/** Latest operation wins, including when an old producer swallows cancellation. Main-thread confined. */
internal class StateOperation(private val scope: CoroutineScope) {
    private var generation = 0L
    private var job: Job? = null

    fun cancel() {
        generation++
        val previous = job
        job = null
        previous?.cancel()
    }

    // User producers and transformations may throw any exception; cancellation is rethrown separately.
    @Suppress("TooGenericExceptionCaught")
    fun start(onError: (Throwable) -> Unit, block: suspend (suspend () -> Unit) -> Unit): Job {
        generation++
        val current = generation
        val previous = job
        val launched = scope.launch(start = CoroutineStart.LAZY) {
            val checkCurrent: suspend () -> Unit = {
                currentCoroutineContext().ensureActive()
                if (current != generation) throw CancellationException("Operation replaced")
            }
            try {
                checkCurrent()
                block(checkCurrent)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                checkCurrent()
                onError(failure)
            }
        }
        // Install before starting: an immediate error callback may synchronously start a retry.
        job = launched
        // Completion callbacks can start another operation while cancellation is delivered.
        previous?.cancel()
        launched.start()
        return launched
    }
}
