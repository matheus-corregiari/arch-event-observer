@file:Suppress(
    "KotlinNullnessAnnotation",
    "TooManyFunctions",
    "MemberVisibilityCanBePrivate",
    "DeprecatedCallableAddReplaceWith",
    "unused"
)

package br.com.arch.toolkit.livedata

import android.os.Looper
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import br.com.arch.toolkit.result.DataResult
import br.com.arch.toolkit.result.DataResultStatus
import br.com.arch.toolkit.result.ObserveWrapper
import br.com.arch.toolkit.result.attachTo
import br.com.arch.toolkit.util.mapNotNull
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow

/**
 * LiveData wrapper that carries [DataResult] values.
 *
 * The class exposes convenience views for the current data, status, and error,
 * plus mapping helpers that keep the same response model.
 */
open class ResponseLiveData<T> : LiveData<DataResult<T>> {

    private var mergeLock = Any()

    protected var scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        private set

    /** Sets the scope used by observation helpers. */
    open fun scope(scope: CoroutineScope): ResponseLiveData<T> {
        this.scope = scope
        return this
    }

    protected var transformDispatcher: CoroutineDispatcher = Dispatchers.IO
        private set

    /** Sets the dispatcher used for mapping callbacks. */
    open fun transformDispatcher(dispatcher: CoroutineDispatcher): ResponseLiveData<T> {
        transformDispatcher = dispatcher
        return this
    }

    /**
     * Current error value, if any.
     */
    val error: Throwable?
        @Nullable get() = value?.error

    /**
     * Current status value, if any.
     */
    val status: DataResultStatus?
        @Nullable get() = value?.status

    /**
     * Current data value, if any.
     */
    val data: T?
        @Nullable get() = value?.data

    val liveData: LiveData<DataResult<T>> get() = this
    val dataLiveData: LiveData<T> get() = liveData.mapNotNull { it.data }
    val statusLiveData: LiveData<DataResultStatus> get() = liveData.mapNotNull { it.status }
    val errorLiveData: LiveData<Throwable> get() = liveData.mapNotNull { it.error }

    /**
     * Creates an empty response LiveData.
     */
    constructor() : super()

    /**
     * Creates a response LiveData with an initial [DataResult].
     */
    constructor(value: DataResult<T>) : super(value)

    //region Mappers

    /**
     * Maps the payload to a new [ResponseLiveData].
     */
    @NonNull
    fun <R> map(@NonNull transformation: ((T) -> R)): ResponseLiveData<R> {
        val liveData = SwapResponseLiveData<R>()
            .scope(scope)
            .transformDispatcher(transformDispatcher)
        liveData.swapSource(this, transformation)
        return liveData
    }

    /**
     * Maps the current error to a new [ResponseLiveData].
     */
    @NonNull
    fun mapError(@NonNull transformation: (Throwable) -> Throwable): ResponseLiveData<T> {
        val liveData = SwapResponseLiveData<T>()
            .scope(scope)
            .transformDispatcher(transformDispatcher)
        liveData.swapSource(this, { it }, transformation)
        return liveData
    }

    /**
     * Replaces an error with a successful data value.
     */
    @NonNull
    fun onErrorReturn(@NonNull onErrorReturn: ((Throwable) -> T)): ResponseLiveData<T> {
        val liveData = SwapResponseLiveData<T>()
            .scope(scope)
            .transformDispatcher(transformDispatcher)
        liveData.swapSource(this, { it }, null, onErrorReturn)
        return liveData
    }
    //endregion

    //region Observability

    /**
     * Runs [onNext] before forwarding the current data value.
     */
    @NonNull
    fun onNext(@NonNull onNext: ((T) -> Unit)): ResponseLiveData<T> = map {
        onNext(it)
        it
    }

    /**
     * Runs [onError] before forwarding the current error value.
     */
    @NonNull
    fun onError(@NonNull onError: ((Throwable) -> Unit)): ResponseLiveData<T> = mapError {
        onError(it)
        it
    }

    /**
     * Transforms the full [DataResult] before forwarding it.
     */
    @NonNull
    fun <R> transform(
        @NonNull transformation: (DataResult<T>) -> DataResult<R>
    ): ResponseLiveData<R> {
        val liveData = SwapResponseLiveData<R>()
            .scope(scope)
            .transformDispatcher(transformDispatcher)
        liveData.swapSource(this, transformation)
        return liveData
    }
    //endregion

    /**
     * Creates an [ObserveWrapper], applies [wrapperConfig], and observes it with [owner].
     */
    @NonNull
    fun observe(
        @NonNull owner: LifecycleOwner,
        @NonNull wrapperConfig: ObserveWrapper<T>.() -> Unit
    ) = ObserveWrapper<T>()
        .scope(scope)
        .transformDispatcher(transformDispatcher)
        .apply(wrapperConfig)
        .attachTo(liveData = this, owner = owner)

    /**
     * Posts on the main thread when possible, otherwise falls back to [postValue].
     */
    protected open fun safePostValue(value: DataResult<T>?) {
        if (Looper.getMainLooper()?.isCurrentThread == true) {
            this.value = value
        } else {
            postValue(value)
        }
    }

    companion object {
        /** Creates a response LiveData from a flow of [DataResult] values. */
        fun <T> from(flow: Flow<DataResult<T>>) = responseLiveData {
            flow.collect { emit(it) }
        }

        /** Creates a response LiveData from a plain flow. */
        fun <T> fromFlow(flow: Flow<T>) = responseLiveData {
            flow.collect { emitData(it) }
        }
    }
}
