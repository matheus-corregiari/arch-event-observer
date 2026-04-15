package br.com.arch.toolkit.flow

import br.com.arch.toolkit.result.DataResult
import br.com.arch.toolkit.result.DataResultStatus
import br.com.arch.toolkit.util.dataResultNone
import br.com.arch.toolkit.util.dataResultSuccess
import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalForInheritanceCoroutinesApi::class)
open class ResponseStateFlow<T> internal constructor(
    private val innerFlow: StateFlow<DataResult<T>>
) : ResponseSharedFlow<T>(innerFlow), StateFlow<DataResult<T>> by innerFlow {

    /** Current status of the latest [DataResult]. */
    val status: DataResultStatus get() = value.status

    /** Current error of the latest [DataResult]. */
    val error: Throwable? get() = value.error

    /** Current payload of the latest [DataResult]. */
    val data: T? get() = value.data

    override val replayCache get() = innerFlow.replayCache

    override suspend fun collect(
        collector: FlowCollector<DataResult<T>>
    ) = innerFlow.collect(collector)

    companion object {
        /** Creates a state flow with [DataResultStatus.NONE]. */
        operator fun <T> invoke(): ResponseStateFlow<T> = ResponseStateFlow(
            innerFlow = MutableStateFlow<DataResult<T>>(dataResultNone())
        )

        /** Creates a state flow from a single [DataResult]. */
        operator fun <T> invoke(data: DataResult<T>): ResponseStateFlow<T> = ResponseStateFlow(
            innerFlow = MutableStateFlow<DataResult<T>>(data)
        )

        /** Converts a plain flow into a [ResponseStateFlow]. */
        fun <T> fromFlow(
            flow: Flow<T>,
            started: SharingStarted = SharingStarted.WhileSubscribed(),
            initial: DataResult<T> =
                (flow as? StateFlow<T>)?.value?.let(::dataResultSuccess) ?: dataResultNone()
        ): ResponseStateFlow<T> = ResponseFlow.fromFlow(flow)
            .state(started = started, initial = initial)

        /** Converts a flow of [DataResult] values into a [ResponseStateFlow]. */
        fun <T> from(
            flow: Flow<DataResult<T>>,
            started: SharingStarted = SharingStarted.WhileSubscribed(),
            initial: DataResult<T> = (flow as? StateFlow<DataResult<T>>)?.value ?: dataResultNone()
        ): ResponseStateFlow<T> = ResponseFlow.from(flow)
            .state(started = started, initial = initial)

        /** Maps a flow of [DataResult] values and converts it to a state flow. */
        fun <T, R> from(
            flow: Flow<DataResult<R>>,
            transform: (R) -> T,
            started: SharingStarted = SharingStarted.WhileSubscribed(),
            initial: DataResult<T> =
                (flow as? StateFlow<DataResult<R>>)?.value?.transform(transform) ?: dataResultNone()
        ): ResponseStateFlow<T> = ResponseFlow.from(flow = flow, transform = transform)
            .state(started = started, initial = initial)

        /** Transforms a plain flow and converts it to a state flow. */
        fun <T, R> fromFlow(
            flow: Flow<R>,
            transform: (R) -> T,
            started: SharingStarted = SharingStarted.WhileSubscribed(),
            initial: DataResult<T> = (flow as? StateFlow<R>)?.value?.let(transform)
                ?.let(::dataResultSuccess) ?: dataResultNone()
        ): ResponseStateFlow<T> = ResponseFlow.fromFlow(flow = flow, transform = transform)
            .state(started = started, initial = initial)
    }
}
