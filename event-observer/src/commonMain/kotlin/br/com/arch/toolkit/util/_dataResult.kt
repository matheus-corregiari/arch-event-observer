package br.com.arch.toolkit.util

import br.com.arch.toolkit.result.DataResult
import br.com.arch.toolkit.result.DataResultStatus
import br.com.arch.toolkit.result.DataResultStatus.ERROR
import br.com.arch.toolkit.result.DataResultStatus.LOADING
import br.com.arch.toolkit.result.DataResultStatus.NONE
import br.com.arch.toolkit.result.DataResultStatus.SUCCESS
import kotlin.math.max

//region Data Result Creator Methods
/**
 * Creates a successful [DataResult] with the supplied data.
 */
fun <T> dataResultSuccess(data: T?) = DataResult(data, null, SUCCESS)

/**
 * Creates a loading [DataResult].
 */
fun <T> dataResultLoading(data: T? = null, error: Throwable? = null) =
    DataResult(data, error, LOADING)

/**
 * Creates an error [DataResult].
 */
fun <T> dataResultError(error: Throwable?, data: T? = null) =
    DataResult(data, error, ERROR)

/**
 * Creates a neutral [DataResult] with no data and no error.
 */
fun <T> dataResultNone() = DataResult<T>(null, null, NONE)
//endregion

//region Transformation Methods
/**
 * Returns this result or [dataResultNone] when the receiver is `null`.
 */
fun <T> DataResult<T>?.orNone() = this ?: dataResultNone()

/** Converts a nullable pair payload into a pair with non-null values when possible. */
fun <T, R> DataResult<Pair<T?, R?>>.onlyWithValues(): DataResult<Pair<T, R>>? =
    when (val data = data) {
        null -> DataResult(null, error, status)
        else -> data.runCatching { requireNotNull(first) to requireNotNull(second) }
            .mapCatching { DataResult<Pair<T, R>>(it, error, status) }
            .getOrNull()
    }

/**
 * Merges this result with [second], preserving the highest status and first error.
 */
fun <T, R> DataResult<T>?.merge(second: DataResult<R>?): DataResult<Pair<T?, R?>> = when {
    /* One of the results is null */
    this == null || second == null -> DataResult(
        data = Pair(this?.data, second?.data)
            .takeIf { it.first != null || it.second != null },
        error = this?.error ?: second?.error,
        status = this?.status ?: second?.status ?: NONE
    )

    /* Both results have NONE status */
    this.status == NONE && second.status == NONE -> dataResultNone()

    /* Both non-null results without any NONE status */
    else -> DataResult(
        data = (this.data to second.data)
            .takeIf { it.first != null || it.second != null },
        error = this.error ?: second.error,
        status = DataResultStatus.entries[max(this.status.ordinal, second.status.ordinal)]
    )
}

/**
 * Merges this result with [second] and drops the pair when either side is `null`.
 */
fun <T, R> DataResult<T>?.mergeNotNull(second: DataResult<R>?): DataResult<Pair<T, R>> {
    val mergeNullable = merge(second)

    val data = mergeNullable.data?.onlyWithValues()

    val error = mergeNullable.error
    val status = mergeNullable.status

    return DataResult(
        data = data,
        error = error,
        status = status
    )
}

/**
 * Merges a list of keyed results into a single map result.
 */
fun List<Pair<String, DataResult<*>?>>.mergeAll(): DataResult<Map<String, *>> {
    val resultWithMaxStatus = maxBy { it.second?.status?.ordinal ?: NONE.ordinal }
    if (resultWithMaxStatus.second?.status == NONE) return dataResultNone()

    val ordinal = resultWithMaxStatus.second?.status?.ordinal ?: NONE.ordinal
    val status = DataResultStatus.entries[ordinal]

    return DataResult(
        associate { (key, result) -> key to result?.data }
            .takeIf { it.values.filterNotNull().isNotEmpty() },
        firstOrNull { (_, result) -> result?.error != null }?.second?.error,
        status
    )
}
//endregion

//region Operator Methods
/**
 * Alias for [merge].
 */
operator fun <T, R> DataResult<T>?.plus(another: DataResult<R>?) = merge(another)
//endregion
