@file:Suppress("TooManyFunctions")
@file:OptIn(ExperimentalForInheritanceCoroutinesApi::class)

package br.com.arch.toolkit.flow

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import br.com.arch.toolkit.result.DataResult
import br.com.arch.toolkit.result.ObserveWrapper
import br.com.arch.toolkit.util.dataResultError
import br.com.arch.toolkit.util.dataResultNone
import br.com.arch.toolkit.util.dataResultSuccess
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn

open class ResponseFlow<T> internal constructor(
    private val innerFlow: Flow<DataResult<T>>
) : Flow<DataResult<T>> by innerFlow {

    private var scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var transformDispatcher: CoroutineDispatcher = Dispatchers.Default

    /** Sets the scope used by observation helpers. */
    fun scope(scope: CoroutineScope) = apply { this.scope = scope }

    /** Sets the dispatcher used for transformation callbacks. */
    fun transformDispatcher(dispatcher: CoroutineDispatcher) =
        apply { transformDispatcher = dispatcher }

    /** Collects the flow with the supplied [ObserveWrapper] DSL. */
    suspend fun collect(wrapper: ObserveWrapper<T>.() -> Unit) =
        ObserveWrapper<T>().scope(scope).transformDispatcher(transformDispatcher).apply(wrapper)
            .suspendFunc { collect { data -> handleResult(data) } }

    /** Observes this flow from the supplied [LifecycleOwner]. */
    fun observe(
        owner: LifecycleOwner,
        wrapper: ObserveWrapper<T>.() -> Unit
    ) = ObserveWrapper<T>().scope(owner.lifecycleScope).transformDispatcher(transformDispatcher)
        .apply(wrapper).suspendFunc {
            owner.lifecycle.repeatOnLifecycle(
                state = Lifecycle.State.STARTED,
                block = { collect(::handleResult) }
            )
        }

    /** Maps the payload while keeping the [DataResult] envelope. */
    fun <R> map(transform: (T) -> R) = from(flow = innerFlow, transform = transform)

    /** Converts this flow into a [ResponseStateFlow]. */
    fun state(
        scope: CoroutineScope = this.scope,
        started: SharingStarted = SharingStarted.WhileSubscribed(),
        initial: DataResult<T> = (innerFlow as? StateFlow<DataResult<T>>)?.value
            ?: (innerFlow as? SharedFlow<DataResult<T>>)?.replayCache?.lastOrNull()
            ?: dataResultNone()
    ): ResponseStateFlow<T> = ResponseStateFlow(
        innerFlow = innerFlow.stateIn(scope = scope, started = started, initialValue = initial)
    )

    /** Converts this flow into a [ResponseSharedFlow]. */
    fun shared(
        scope: CoroutineScope = this.scope,
        started: SharingStarted = SharingStarted.WhileSubscribed(),
        replay: Int = (innerFlow as? SharedFlow<DataResult<T>>)?.replayCache?.size
            ?: (innerFlow as? StateFlow<DataResult<T>>)?.replayCache?.size ?: 0
    ): ResponseSharedFlow<T> = ResponseSharedFlow(
        innerFlow = innerFlow.shareIn(scope = scope, started = started, replay = replay)
    )

    override fun equals(other: Any?) = innerFlow == other

    override fun hashCode() = innerFlow.hashCode()

    override fun toString() = innerFlow.toString()

    companion object {

        /** Creates an empty response flow. */
        operator fun <T> invoke(): ResponseFlow<T> = ResponseFlow(emptyFlow())

        /** Creates a flow that emits a single [DataResult]. */
        operator fun <T> invoke(data: DataResult<T>): ResponseFlow<T> = ResponseFlow(flowOf(data))

        /** Creates a flow from a fixed set of [DataResult] values. */
        operator fun <T> invoke(vararg data: DataResult<T>): ResponseFlow<T> =
            ResponseFlow(flowOf(*data))

        /** Creates a flow from a list of [DataResult] values. */
        operator fun <T> invoke(dataList: List<DataResult<T>>): ResponseFlow<T> =
            ResponseFlow(dataList.asFlow())

        /** Wraps an existing flow of [DataResult] values. */
        fun <T> from(flow: Flow<DataResult<T>>): ResponseFlow<T> = ResponseFlow(innerFlow = flow)

        /** Wraps and maps an existing flow of [DataResult] values. */
        fun <T, R> from(
            flow: Flow<DataResult<R>>,
            transform: (R) -> T
        ): ResponseFlow<T> = ResponseFlow(
            innerFlow = flow
                .map { it.transform(transform) }
                .catch { emit(dataResultError(it)) }
        )

        /** Wraps a plain flow and turns each value into a successful [DataResult]. */
        fun <T> fromFlow(flow: Flow<T>): ResponseFlow<T> = from(
            flow = flow.map(::dataResultSuccess)
        )

        /** Wraps a plain flow and transforms each value before wrapping it. */
        fun <T, R> fromFlow(
            flow: Flow<R>,
            transform: suspend (R) -> T
        ): ResponseFlow<T> = ResponseFlow<T>(
            innerFlow = flow
                .map { dataResultSuccess<T>(transform(it)) }
                .catch { emit(dataResultError<T>(it)) }
        )
    }
}
