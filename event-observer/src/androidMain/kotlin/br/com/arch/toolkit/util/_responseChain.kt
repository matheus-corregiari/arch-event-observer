@file:Suppress("Filename", "TooManyFunctions", "LongParameterList", "unused")

package br.com.arch.toolkit.util

import androidx.lifecycle.LiveData
import androidx.lifecycle.asFlow
import br.com.arch.toolkit.annotation.Experimental
import br.com.arch.toolkit.livedata.ResponseLiveData
import br.com.arch.toolkit.livedata.responseLiveData
import br.com.arch.toolkit.result.DataResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/* region LiveData + Response Functions --------------------------------------------------------- */
/** Produces a [ResponseLiveData] from a nullable source value. */
@FunctionalInterface
fun interface WithResponse<T, R> {
    suspend fun invoke(result: T?): ResponseLiveData<R>
}

/* Nullable ------------------------------------------------------------------------------------- */

/** Chains a nullable [LiveData] with a response factory. */
@Experimental
fun <T, R> LiveData<T>.chainWith(
    context: CoroutineContext = EmptyCoroutineContext,
    other: WithResponse<T, R>,
    condition: suspend (T?) -> Boolean
): ResponseLiveData<Pair<T?, R?>> = responseLiveData(context = context) {
    toResponse().internalResponseChainWith(
        other = { value -> value.data?.let { other.invoke(it) } ?: error("Null other Response") },
        condition = { value -> value.data?.let { condition.invoke(it) } ?: false }
    ).collect(::emit)
}

/** Chains a nullable [LiveData] and then applies a transformation. */
@Experimental
fun <T, R, X> LiveData<T>.chainWith(
    context: CoroutineContext = EmptyCoroutineContext,
    other: WithResponse<T, R>,
    condition: suspend (T?) -> Boolean,
    transform: ResponseTransform<T?, R?, X>
): ResponseLiveData<X> = responseLiveData(context = context) {
    toResponse().internalResponseChainWith(
        other = { value -> value.data?.let { other.invoke(it) } ?: error("Null other Response") },
        condition = { value -> value.data?.let { condition.invoke(it) } ?: false }
    ).applyTransformation(context, transform).collect(::emit)
}

/* Non Nullable --------------------------------------------------------------------------------- */
/** Chains a non-null [LiveData] with a response factory. */
@Experimental
fun <T, R> LiveData<T>.chainNotNullWith(
    context: CoroutineContext = EmptyCoroutineContext,
    other: WithResponse<T, R>,
    condition: suspend (T) -> Boolean
): ResponseLiveData<Pair<T, R>> = responseLiveData(context = context) {
    toResponse().internalResponseChainNotNullWith(
        other = { value -> value.data?.let { other.invoke(it) } ?: error("Null other Response") },
        condition = { value -> value.data?.let { condition.invoke(it) } ?: false }
    ).collect(::emit)
}

/** Chains a non-null [LiveData] and then applies a transformation. */
@Experimental
fun <T, R, X> LiveData<T>.chainNotNullWith(
    context: CoroutineContext = EmptyCoroutineContext,
    other: WithResponse<T, R>,
    condition: suspend (T) -> Boolean,
    transform: ResponseTransform<T, R, X>
): ResponseLiveData<X> = responseLiveData(context = context) {
    toResponse().internalResponseChainNotNullWith(
        other = { value -> value.data?.let { other.invoke(it) } ?: error("Null other Response") },
        condition = { value -> value.data?.let { condition.invoke(it) } ?: false }
    ).applyTransformation(context, transform).collect(::emit)
}
/* endregion ------------------------------------------------------------------------------------ */

/* region Response + LiveData Functions ---------------------------------------------------------------- */
/** Produces a [LiveData] from a [DataResult]. */
@FunctionalInterface
interface ResponseWith<T, R> {
    suspend fun invoke(result: DataResult<T>): LiveData<R>
}

/* Nullable ------------------------------------------------------------------------------------- */
/** Chains a [ResponseLiveData] with a LiveData-producing factory. */
@Experimental
fun <T, R> ResponseLiveData<T>.chainWith(
    context: CoroutineContext,
    other: ResponseWith<T, R>,
    condition: suspend (DataResult<T>) -> Boolean
): ResponseLiveData<Pair<T?, R?>> = responseLiveData(context = context) {
    internalResponseChainWith(
        condition = condition,
        other = { result -> other.invoke(result).toResponse() }
    ).collect(::emit)
}

/** Chains a [ResponseLiveData] and then applies a transformation. */
@Experimental
fun <T, R, X> ResponseLiveData<T>.chainWith(
    context: CoroutineContext,
    other: ResponseWith<T, R>,
    condition: suspend (DataResult<T>) -> Boolean,
    transform: ResponseTransform<T?, R?, X>
): ResponseLiveData<X> = responseLiveData(context = context) {
    internalResponseChainWith(
        condition = condition,
        other = { result -> other.invoke(result).toResponse() }
    ).applyTransformation(context, transform).collect(::emit)
}

/* Non Nullable --------------------------------------------------------------------------------- */
/** Chains a [ResponseLiveData] with a LiveData-producing factory using non-null pairs. */
@Experimental
fun <T, R> ResponseLiveData<T>.chainNotNullWith(
    context: CoroutineContext,
    other: ResponseWith<T, R>,
    condition: suspend (DataResult<T>) -> Boolean
): ResponseLiveData<Pair<T, R>> = responseLiveData(context = context) {
    internalResponseChainNotNullWith(
        condition = condition,
        other = { result -> other.invoke(result).toResponse() }
    ).collect(::emit)
}

/** Chains a [ResponseLiveData] and then applies a transformation using non-null pairs. */
@Experimental
fun <T, R, X> ResponseLiveData<T>.chainNotNullWith(
    context: CoroutineContext,
    other: ResponseWith<T, R>,
    condition: suspend (DataResult<T>) -> Boolean,
    transform: ResponseTransform<T, R, X>
): ResponseLiveData<X> = responseLiveData(context = context) {
    internalResponseChainNotNullWith(
        condition = condition,
        other = { result -> other.invoke(result).toResponse() }
    ).applyTransformation(context, transform).collect(::emit)
}
/* endregion ------------------------------------------------------------------------------------ */

/* region Response + Response Functions --------------------------------------------------------- */
/** Produces a [ResponseLiveData] from a [DataResult]. */
@FunctionalInterface
interface ResponseWithResponse<T, R> {
    suspend fun invoke(result: DataResult<T>): ResponseLiveData<R>
}

/* Nullable ------------------------------------------------------------------------------------- */
/** Chains a [ResponseLiveData] with another [ResponseLiveData] factory. */
@Experimental
fun <T, R> ResponseLiveData<T>.chainWith(
    context: CoroutineContext,
    other: ResponseWithResponse<T, R>,
    condition: suspend (DataResult<T>) -> Boolean
): ResponseLiveData<Pair<T?, R?>> = responseLiveData(context = context) {
    internalResponseChainWith(
        condition = condition,
        other = other::invoke
    ).collect(::emit)
}

/** Chains a [ResponseLiveData] and then applies a transformation. */
@Experimental
fun <T, R, X> ResponseLiveData<T>.chainWith(
    context: CoroutineContext,
    other: ResponseWithResponse<T, R>,
    condition: suspend (DataResult<T>) -> Boolean,
    transform: ResponseTransform<T?, R?, X>
): ResponseLiveData<X> = responseLiveData(context = context) {
    internalResponseChainWith(
        condition = condition,
        other = other::invoke
    ).applyTransformation(context, transform).collect(::emit)
}

/* Non Nullable --------------------------------------------------------------------------------- */
/** Chains a [ResponseLiveData] with another [ResponseLiveData] factory using non-null pairs. */
@Experimental
fun <T, R> ResponseLiveData<T>.chainNotNullWith(
    context: CoroutineContext,
    other: ResponseWithResponse<T, R>,
    condition: suspend (DataResult<T>) -> Boolean
): ResponseLiveData<Pair<T, R>> = responseLiveData(context = context) {
    internalResponseChainNotNullWith(
        condition = condition,
        other = other::invoke
    ).collect(::emit)
}

/** Chains a [ResponseLiveData] and then applies a transformation using non-null pairs. */
@Experimental
fun <T, R, X> ResponseLiveData<T>.chainNotNullWith(
    context: CoroutineContext,
    other: ResponseWithResponse<T, R>,
    condition: suspend (DataResult<T>) -> Boolean,
    transform: ResponseTransform<T, R, X>
): ResponseLiveData<X> = responseLiveData(context = context) {
    internalResponseChainNotNullWith(
        condition = condition,
        other = other::invoke
    ).applyTransformation(context, transform).collect(::emit)
}
/* endregion ------------------------------------------------------------------------------------ */

/* region Auxiliary Functions ------------------------------------------------------------------- */
private suspend inline fun <T, R> ResponseLiveData<T>.internalResponseChainNotNullWith(
    noinline other: suspend (DataResult<T>) -> ResponseLiveData<R>,
    noinline condition: suspend (DataResult<T>) -> Boolean
) = internalResponseChainWith(
    other = other,
    condition = condition
).mapNotNull { it.onlyWithValues() }

private suspend inline fun <T, R> ResponseLiveData<T>.internalResponseChainWith(
    noinline other: suspend (DataResult<T>) -> ResponseLiveData<R>,
    noinline condition: suspend (DataResult<T>) -> Boolean
) = channelFlow<DataResult<Pair<T?, R?>>> {
    val aFlow: Flow<DataResult<T>> = asFlow()
    var bJob: Job? = null

    aFlow.collect { aValue: DataResult<T> ->
        bJob?.cancel("New Data Arrived, so, should cancel previous job")

        /* */
        val isConditionMet = condition.runCatching { invoke(aValue) }
            .getOrDefault(false)

        /* */
        val liveData = other.takeIf { isConditionMet }?.runCatching { invoke(aValue) }
            ?.getOrNull()

        /* */
        when {
            /* */
            isConditionMet.not() && aValue.isError ->
                trySend(dataResultError(aValue.error))

            /* Do nothing */
            liveData == null -> Unit

            /* Here the magic becomes alive! */
            else -> {
                if (liveData.isInitialized.not()) trySend(aValue + null)
                bJob = launch { internalResponseCombine(liveData).collect(::trySend) }
            }
        }
    }
}
/* endregion ------------------------------------------------------------------------------------ */
