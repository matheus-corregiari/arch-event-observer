@file:JvmName("LiveDataUtils")
@file:Suppress("unused")

package br.com.arch.toolkit.util

import android.os.Looper
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.asFlow
import androidx.lifecycle.map
import br.com.arch.toolkit.livedata.ResponseLiveData
import br.com.arch.toolkit.livedata.responseLiveData
import br.com.arch.toolkit.result.DataResult

/**
 * Wraps this LiveData as a [ResponseLiveData] of [DataResult] values.
 */
fun <T> LiveData<DataResult<T>>.asResponse(): ResponseLiveData<T> = when {
    this is ResponseLiveData<*> -> this as ResponseLiveData<T>
    else -> responseLiveData { asFlow().collect(::emit) }
}

/**
 * Wraps this LiveData as a [ResponseLiveData] of raw values.
 */
fun <T> LiveData<T>.toResponse(): ResponseLiveData<T> =
    responseLiveData { asFlow().collect(::emitData) }

/** Observes only non-null values. */
inline fun <T> LiveData<T>.observeNotNull(
    owner: LifecycleOwner,
    crossinline observer: (T) -> Unit
) = observe(owner) { it?.let(observer) }

/** Observes only null values. */
inline fun <T> LiveData<T>.observeNull(owner: LifecycleOwner, crossinline observer: () -> Unit) =
    observe(owner) {
        if (it == null) {
            observer.invoke()
        }
    }

/** Observes the first non-null value. */
fun <T> LiveData<T>.observeSingle(owner: LifecycleOwner, observer: ((T) -> Unit)) =
    observeUntil(owner) {
        it?.let(observer)
        it != null
    }

/** Observes values until [observer] returns `true`. */
fun <T> LiveData<T>.observeUntil(owner: LifecycleOwner, observer: ((T?) -> Boolean)) =
    observe(
        owner,
        object : Observer<T> {
            override fun onChanged(value: T) {
                if (value.let(observer)) removeObserver(this)
            }
        }
    )

/** Maps a nullable list payload. */
fun <T, R> LiveData<List<T>?>.mapList(
    transformation: (T) -> R
): LiveData<List<R>?> = map { it?.map(transformation) }

/** Maps a LiveData and drops `null` results. */
fun <T, R> LiveData<T>.mapNotNull(
    transform: (T) -> R?
): LiveData<R> {
    val result = MediatorLiveData<R>()
    val applyTransform: (T) -> Unit = { value -> transform(value)?.let(result::setValue) }
    value.takeIf { isInitialized && it != null }?.let(applyTransform)
    result.addSource(this, applyTransform)
    return result
}

/** Sets a MutableLiveData value on the main thread when possible. */
fun <T> MutableLiveData<T>.safePostValue(value: T?) {
    if (Looper.getMainLooper()?.isCurrentThread == true) {
        this.value = value
    } else {
        postValue(value)
    }
}
