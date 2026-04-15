@file:SuppressLint("KotlinNullnessAnnotation")
@file:Suppress("TooManyFunctions")

package br.com.arch.toolkit.livedata

import android.annotation.SuppressLint
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import br.com.arch.toolkit.result.DataResult
import br.com.arch.toolkit.util.dataResultError
import br.com.arch.toolkit.util.dataResultLoading
import br.com.arch.toolkit.util.dataResultNone
import br.com.arch.toolkit.util.dataResultSuccess
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope

/** Mutable [ResponseLiveData] with direct setters and posting helpers. */
class MutableResponseLiveData<T> : ResponseLiveData<T> {

    /** Creates an empty mutable response LiveData. */
    constructor() : super()

    /** Creates a mutable response LiveData with an initial [DataResult]. */
    constructor(value: DataResult<T>) : super(value)

    // region Post Methods
    /** Posts a loading result. */
    fun postLoading(@Nullable data: T? = null) {
        postValue(dataResultLoading(data))
    }

    /** Posts an error result. */
    fun postError(@NonNull error: Throwable, @Nullable data: T? = null) {
        postValue(dataResultError(error, data))
    }

    /** Posts a success result with data. */
    fun postData(@NonNull data: T) {
        postValue(dataResultSuccess(data))
    }

    /** Posts a success result without data. */
    fun postSuccess() {
        postValue(dataResultSuccess(null))
    }

    /** Posts the neutral none state. */
    fun postNone() {
        postValue(dataResultNone())
    }
    // endregion

    // region Set methods
    /** Sets a loading result. */
    fun setLoading(@Nullable data: T? = null) {
        value = dataResultLoading(data)
    }

    /** Sets an error result. */
    fun setError(@NonNull error: Throwable, @Nullable data: T? = null) {
        value = dataResultError(error, data)
    }

    /** Sets a success result with data. */
    fun setData(@NonNull data: T) {
        value = dataResultSuccess(data)
    }

    /** Sets a success result without data. */
    fun setSuccess() {
        value = dataResultSuccess(null)
    }

    /** Sets the neutral none state. */
    fun setNone() {
        value = dataResultNone()
    }
    // endregion

    /** Narrows the return type of [ResponseLiveData.scope]. */
    override fun scope(scope: CoroutineScope): MutableResponseLiveData<T> =
        super.scope(scope) as MutableResponseLiveData<T>

    /** Narrows the return type of [ResponseLiveData.transformDispatcher]. */
    override fun transformDispatcher(dispatcher: CoroutineDispatcher): MutableResponseLiveData<T> =
        super.transformDispatcher(dispatcher) as MutableResponseLiveData<T>

    /** Exposes the protected setter. */
    public override fun setValue(value: DataResult<T>?) = super.setValue(value)

    /** Exposes the protected poster. */
    public override fun postValue(value: DataResult<T>?) = super.postValue(value)

    /** Exposes the protected main-thread-safe setter. */
    public override fun safePostValue(value: DataResult<T>?) = super.safePostValue(value)
}
