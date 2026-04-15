@file:Suppress("Filename", "TooManyFunctions", "unused")

package br.com.arch.toolkit.util

import androidx.lifecycle.LiveData
import androidx.lifecycle.asFlow
import br.com.arch.toolkit.annotation.Experimental
import br.com.arch.toolkit.livedata.ResponseLiveData
import br.com.arch.toolkit.livedata.responseLiveData
import br.com.arch.toolkit.result.DataResult
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/* region Operator Functions -------------------------------------------------------------------- */
@Experimental
/** Combines a [LiveData] with a [ResponseLiveData]. */
operator fun <T, R> LiveData<T>.plus(other: ResponseLiveData<R>): ResponseLiveData<Pair<T?, R?>> =
    combine(context = EmptyCoroutineContext, response = other)

@Experimental
/** Combines two [ResponseLiveData] instances. */
operator fun <T, R> ResponseLiveData<T>.plus(source: LiveData<R>): ResponseLiveData<Pair<T?, R?>> =
    combine(context = EmptyCoroutineContext, liveData = source)

@Experimental
/** Combines two [ResponseLiveData] instances. */
operator fun <T, R> ResponseLiveData<T>.plus(
    source: ResponseLiveData<R>
): ResponseLiveData<Pair<T?, R?>> =
    combine(context = EmptyCoroutineContext, response = source)
/* endregion ------------------------------------------------------------------------------------ */

/* region LiveData + Response Functions --------------------------------------------------------- */
/* Nullable ------------------------------------------------------------------------------------- */
/** Combines a [LiveData] with a [ResponseLiveData]. */
@Experimental
fun <T, R> LiveData<T>.combine(
    context: CoroutineContext = EmptyCoroutineContext,
    response: ResponseLiveData<R>
): ResponseLiveData<Pair<T?, R?>> = responseLiveData(context = context) {
    toResponse().internalResponseCombine(response).collect(::emit)
}

/** Combines a [LiveData] with a [ResponseLiveData] and then transforms the pair. */
@Experimental
fun <T, R, X> LiveData<T>.combine(
    context: CoroutineContext = EmptyCoroutineContext,
    response: ResponseLiveData<R>,
    transform: ResponseTransform<T?, R?, X>
): ResponseLiveData<X> = responseLiveData(context = context) {
    toResponse().internalResponseCombine(response)
        .applyTransformation(context, transform)
        .collect(::emit)
}

/* Non Nullable --------------------------------------------------------------------------------- */
/** Combines a [LiveData] with a [ResponseLiveData] and drops null pair members. */
@Experimental
fun <T, R> LiveData<T>.combineNotNull(
    context: CoroutineContext = EmptyCoroutineContext,
    response: ResponseLiveData<R>
): ResponseLiveData<Pair<T, R>> = responseLiveData(context = context) {
    toResponse().internalResponseCombineNotNull(response)
        .collect(::emit)
}

/** Combines a [LiveData] with a [ResponseLiveData], then transforms non-null pairs. */
@Experimental
fun <T, R, X> LiveData<T>.combineNotNull(
    context: CoroutineContext = EmptyCoroutineContext,
    response: ResponseLiveData<R>,
    transform: ResponseTransform<T, R, X>
): ResponseLiveData<X> = responseLiveData(context = context) {
    toResponse().internalResponseCombineNotNull(response)
        .applyTransformation(context, transform)
        .collect(::emit)
}
/* endregion ------------------------------------------------------------------------------------ */

/* region Response + LiveData Functions ---------------------------------------------------------------- */
/* Nullable ------------------------------------------------------------------------------------- */
/** Combines a [ResponseLiveData] with a [LiveData]. */
@Experimental
fun <T, R> ResponseLiveData<T>.combine(
    context: CoroutineContext = EmptyCoroutineContext,
    liveData: LiveData<R>
): ResponseLiveData<Pair<T?, R?>> = responseLiveData(context = context) {
    internalResponseCombine(liveData.toResponse())
        .collect(::emit)
}

/** Combines a [ResponseLiveData] with a [LiveData] and then transforms the pair. */
@Experimental
fun <T, R, X> ResponseLiveData<T>.combine(
    context: CoroutineContext = EmptyCoroutineContext,
    liveData: LiveData<R>,
    transform: ResponseTransform<T?, R?, X>
): ResponseLiveData<X> = responseLiveData(context = context) {
    internalResponseCombine(liveData.toResponse())
        .applyTransformation(context, transform)
        .collect(::emit)
}

/* Non Nullable --------------------------------------------------------------------------------- */
/** Combines a [ResponseLiveData] with a [LiveData] and drops null pair members. */
@Experimental
fun <T, R> ResponseLiveData<T>.combineNotNull(
    context: CoroutineContext = EmptyCoroutineContext,
    liveData: LiveData<R>
): ResponseLiveData<Pair<T, R>> = responseLiveData(context = context) {
    internalResponseCombineNotNull(liveData.toResponse()).collect(::emit)
}

/** Combines a [ResponseLiveData] with a [LiveData], then transforms non-null pairs. */
@Experimental
fun <T, R, X> ResponseLiveData<T>.combineNotNull(
    context: CoroutineContext = EmptyCoroutineContext,
    liveData: LiveData<R>,
    transform: ResponseTransform<T, R, X>
): ResponseLiveData<X> = responseLiveData(context = context) {
    internalResponseCombineNotNull(liveData.toResponse())
        .applyTransformation(context, transform)
        .collect(::emit)
}
/* endregion ------------------------------------------------------------------------------------ */

/* region Response + Response Functions --------------------------------------------------------- */
/* Nullable ------------------------------------------------------------------------------------- */
/** Combines two [ResponseLiveData] instances. */
@Experimental
fun <T, R> ResponseLiveData<T>.combine(
    context: CoroutineContext = EmptyCoroutineContext,
    response: ResponseLiveData<R>
): ResponseLiveData<Pair<T?, R?>> = responseLiveData(context = context) {
    internalResponseCombine(response).collect(::emit)
}

/** Combines two [ResponseLiveData] instances and then transforms the pair. */
@Experimental
fun <T, R, X> ResponseLiveData<T>.combine(
    context: CoroutineContext = EmptyCoroutineContext,
    response: ResponseLiveData<R>,
    transform: ResponseTransform<T?, R?, X>
): ResponseLiveData<X> = responseLiveData(context = context) {
    internalResponseCombine(response)
        .applyTransformation(context, transform)
        .collect(::emit)
}

/* Non Nullable --------------------------------------------------------------------------------- */
/** Combines two [ResponseLiveData] instances and drops null pair members. */
@Experimental
fun <T, R> ResponseLiveData<T>.combineNotNull(
    context: CoroutineContext = EmptyCoroutineContext,
    response: ResponseLiveData<R>
): ResponseLiveData<Pair<T, R>> = responseLiveData(context = context) {
    internalResponseCombineNotNull(response).collect(::emit)
}

/** Combines two [ResponseLiveData] instances, then transforms non-null pairs. */
@Experimental
fun <T, R, X> ResponseLiveData<T>.combineNotNull(
    context: CoroutineContext = EmptyCoroutineContext,
    response: ResponseLiveData<R>,
    transform: ResponseTransform<T, R, X>
): ResponseLiveData<X> = responseLiveData(context = context) {
    internalResponseCombineNotNull(response)
        .applyTransformation(context, transform)
        .collect(::emit)
}
/* endregion ------------------------------------------------------------------------------------ */

/* region Auxiliary Functions ------------------------------------------------------------------- */
private suspend inline fun <T, R> ResponseLiveData<T>.internalResponseCombineNotNull(
    other: ResponseLiveData<R>
) =
    internalResponseCombine(other).mapNotNull { it.onlyWithValues() }

internal suspend inline fun <T, R> ResponseLiveData<T>.internalResponseCombine(
    other: ResponseLiveData<R>
) =
    channelFlow {
        val aFlow: Flow<DataResult<T>> = asFlow()
        val bFlow: Flow<DataResult<R>> = other.asFlow()
        val cFlow: Flow<DataResult<Pair<T?, R?>>> = aFlow.combine(bFlow) { a, b -> a + b }

        withContext(currentCoroutineContext()) {
            launch {
                aFlow.collect {
                    if (other.isInitialized.not()) {
                        trySend(
                            it + other.value
                        )
                    } else {
                        cancel()
                    }
                }
            }
            launch { bFlow.collect { if (isInitialized.not()) trySend(value + it) else cancel() } }
            cFlow.collect(::trySend)
        }
    }
/* endregion ------------------------------------------------------------------------------------ */
