@file:JvmName("ResponseLiveDataUtils")
@file:Suppress("TooManyFunctions")

package br.com.arch.toolkit.util

import br.com.arch.toolkit.livedata.MutableResponseLiveData
import br.com.arch.toolkit.livedata.ResponseLiveData
import br.com.arch.toolkit.livedata.SwapResponseLiveData
import br.com.arch.toolkit.result.DataResult
import br.com.arch.toolkit.result.DataResultStatus

/** Creates a [ResponseLiveData] with the supplied value and status. */
fun <T> responseLiveDataOf(value: T, status: DataResultStatus = DataResultStatus.SUCCESS) =
    ResponseLiveData(DataResult(value, null, status))

/** Creates a [ResponseLiveData] in the error state. */
fun <T> responseLiveDataOf(error: Throwable) =
    ResponseLiveData<T>(DataResult(null, error, DataResultStatus.ERROR))

/** Creates a [MutableResponseLiveData] with the supplied value and status. */
fun <T> mutableResponseLiveDataOf(value: T, status: DataResultStatus = DataResultStatus.SUCCESS) =
    MutableResponseLiveData(DataResult(value, null, status))

/** Creates a [MutableResponseLiveData] in the error state. */
fun <T> mutableResponseLiveDataOf(error: Throwable) =
    MutableResponseLiveData<T>(DataResult(null, error, DataResultStatus.ERROR))

/** Creates a [SwapResponseLiveData] with the supplied value and status. */
fun <T> swapResponseLiveDataOf(value: T, status: DataResultStatus = DataResultStatus.SUCCESS) =
    SwapResponseLiveData(DataResult(value, null, status))

/** Creates a [SwapResponseLiveData] in the error state. */
fun <T> swapResponseLiveDataOf(error: Throwable) =
    SwapResponseLiveData<T>(DataResult(null, error, DataResultStatus.ERROR))

/** Maps a list payload while keeping the LiveData wrapper. */
fun <T, R> ResponseLiveData<List<T>>.mapList(
    transformation: (T) -> R
): ResponseLiveData<List<R>> = map { it.map(transformation) }
