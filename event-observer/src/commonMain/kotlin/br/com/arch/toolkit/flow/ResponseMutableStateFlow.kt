package br.com.arch.toolkit.flow

import br.com.arch.toolkit.result.DataResult
import br.com.arch.toolkit.util.dataResultError
import br.com.arch.toolkit.util.dataResultLoading
import br.com.arch.toolkit.util.dataResultNone
import br.com.arch.toolkit.util.dataResultSuccess
import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow

@OptIn(ExperimentalForInheritanceCoroutinesApi::class)
@Suppress("TooManyFunctions")
class ResponseMutableStateFlow<T> private constructor(
    private val innerFlow: MutableStateFlow<DataResult<T>>
) : ResponseStateFlow<T>(innerFlow), MutableStateFlow<DataResult<T>> by innerFlow {

    override var value: DataResult<T>
        get() = innerFlow.value
        set(value) {
            innerFlow.value = value
        }

    override val replayCache get() = innerFlow.replayCache

    override suspend fun collect(collector: FlowCollector<DataResult<T>>) =
        innerFlow.collect(collector)

    override suspend fun emit(value: DataResult<T>) = innerFlow.emit(value)

    override fun tryEmit(value: DataResult<T>) = innerFlow.tryEmit(value)

    // region Custom Emitters
    /** Emits a success state without data. */
    suspend fun emitSuccess() = emit(dataResultSuccess(null))

    /** Tries to emit a success state without data. */
    fun tryEmitSuccess() = tryEmit(dataResultSuccess(null))

    /** Emits a success state with [value]. */
    suspend fun emitData(value: T) = emit(dataResultSuccess(value))

    /** Tries to emit a success state with [value]. */
    fun tryEmitData(value: T) = tryEmit(dataResultSuccess(value))

    /** Emits a loading state with optional data and error. */
    suspend fun emitLoading(value: T? = null, throwable: Throwable? = null) =
        emit(dataResultLoading(value, throwable))

    /** Tries to emit a loading state with optional data and error. */
    fun tryEmitLoading(value: T? = null, throwable: Throwable? = null) =
        tryEmit(dataResultLoading(value, throwable))

    /** Emits an error state with optional data. */
    suspend fun emitError(throwable: Throwable, value: T? = null) =
        emit(dataResultError(throwable, value))

    /** Tries to emit an error state with optional data. */
    fun tryEmitError(throwable: Throwable, value: T? = null) =
        tryEmit(dataResultError(throwable, value))

    /** Emits the neutral none state. */
    suspend fun emitNone() = emit(dataResultNone())

    /** Tries to emit the neutral none state. */
    fun tryEmitNone() = tryEmit(dataResultNone())
    // endregion

    companion object Companion {
        /** Creates a mutable state flow initialized with [DataResultStatus.NONE]. */
        operator fun <T> invoke(): ResponseMutableStateFlow<T> =
            ResponseMutableStateFlow(MutableStateFlow(dataResultNone()))

        /** Creates a mutable state flow from a single [DataResult]. */
        operator fun <T> invoke(data: DataResult<T>): ResponseMutableStateFlow<T> =
            ResponseMutableStateFlow(MutableStateFlow(data))
    }
}
