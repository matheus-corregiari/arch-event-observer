package br.com.arch.toolkit.livedata

import androidx.lifecycle.MediatorLiveData
import br.com.arch.toolkit.exception.DataResultTransformationException
import br.com.arch.toolkit.result.DataResult
import br.com.arch.toolkit.result.DataResultStatus
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** [ResponseLiveData] that mirrors another source and can transform values. */
class SwapResponseLiveData<T> : ResponseLiveData<T> {

    private val sourceLiveData = MediatorLiveData<Any>()
    private val sourceObserver: (Any?) -> Unit = {}
    private var lastSource: ResponseLiveData<*>? = null

    /** Creates an empty swap LiveData. */
    constructor() : super()

    /** Creates a swap LiveData with an initial [DataResult]. */
    constructor(value: DataResult<T>) : super(value)

    /** `true` when a source has been attached. */
    val hasDataSource: Boolean
        get() = lastSource != null

    /** Enables or disables duplicate suppression. */
    private var notifyOnlyOnDistinct: Boolean = false
    fun notifyOnlyOnDistinct(notifyOnlyOnDistinct: Boolean) = apply {
        this.notifyOnlyOnDistinct = notifyOnlyOnDistinct
    }

    /** Mirrors [source] and forwards the same [DataResult] values. */
    fun swapSource(source: ResponseLiveData<T>, discardAfterLoading: Boolean = false) =
        executeSwap(source, discardAfterLoading) { it }

    /** Mirrors [source] after transforming each emitted [DataResult]. */
    fun <R> swapSource(
        source: ResponseLiveData<R>,
        transformation: (DataResult<R>) -> DataResult<T>
    ) = executeSwap(source, false, transformation)

    /** Mirrors [source] while transforming data and error payloads separately. */
    fun <R> swapSource(
        source: ResponseLiveData<R>,
        dataTransformer: (R) -> T,
        errorTransformer: ((Throwable) -> Throwable)? = null,
        onErrorReturn: ((Throwable) -> T)? = null
    ) = executeSwap(source, false) { result ->

        var status = result.status
        val error = result.error?.let { errorTransformer?.invoke(it) ?: result.error }
        var data = result.data?.let(dataTransformer)

        if (data == null && onErrorReturn != null && error != null) {
            data = error.let(onErrorReturn)
        }
        if (onErrorReturn != null && status == DataResultStatus.ERROR) {
            status = DataResultStatus.SUCCESS
        }
        val newValue = DataResult<T>(data, error, status)
        newValue.takeIf { value != newValue }
    }

    /** Detaches the current source. */
    fun clearSource() {
        lastSource?.let {
            scope.launch(Dispatchers.Main) { sourceLiveData.removeSource(it) }
        }
        lastSource = null
    }

    /** `true` when the current source should be refreshed. */
    fun needsRefresh() = hasDataSource.not() || status == DataResultStatus.ERROR

    override fun scope(scope: CoroutineScope) =
        super.scope(scope) as SwapResponseLiveData<T>

    override fun transformDispatcher(dispatcher: CoroutineDispatcher) =
        super.transformDispatcher(dispatcher) as SwapResponseLiveData<T>

    override fun onActive() {
        super.onActive()
        scope.launch(Dispatchers.Main) {
            if (!sourceLiveData.hasObservers()) sourceLiveData.observeForever(sourceObserver)
        }
    }

    override fun onInactive() {
        super.onInactive()
        scope.launch(Dispatchers.Main) { sourceLiveData.removeObserver(sourceObserver) }
    }

    private fun <R> executeSwap(
        source: ResponseLiveData<R>,
        discardAfterLoading: Boolean,
        transformation: (DataResult<R>) -> DataResult<T>?
    ) {
        clearSource()
        lastSource = source
        scope.launch(Dispatchers.Main) {
            sourceLiveData.addSource(source) { data ->
                onChanged(data, discardAfterLoading, transformation)
            }
        }
    }

    private fun <R> onChanged(
        data: DataResult<R>?,
        discardAfterLoading: Boolean,
        transformation: (DataResult<R>) -> DataResult<T>?
    ) = scope.launch {
        withContext(transformDispatcher) {
            transformation.runCatching { data?.let(::invoke) }
        }.onFailure {
            val error = DataResultTransformationException(
                "Error performing swapSource, please check your transformations",
                it
            )

            val result = DataResult<T>(null, error, DataResultStatus.ERROR)
            if (value == result && notifyOnlyOnDistinct) return@onFailure

            safePostValue(result)
        }.getOrNull().let {
            if (value == it && notifyOnlyOnDistinct) return@let
            safePostValue(it)

            if (it?.status != DataResultStatus.LOADING && discardAfterLoading) value = null
        }
    }
}
