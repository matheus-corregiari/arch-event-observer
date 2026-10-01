package br.com.arch.toolkit.eventObserver.state

import androidx.lifecycle.SavedStateHandle
import br.com.arch.toolkit.result.DataResult
import br.com.arch.toolkit.result.DataResultStatus
import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.json.Json

/**
 * Immediate, pure projection. Public selectors deduplicate by value; holder projections use
 * precomputed revision identities so collection never compares large payloads on the owner thread.
 * This is the only custom StateFlow adapter; it owns no job, cache, or mutable state.
 */
@OptIn(ExperimentalForInheritanceCoroutinesApi::class)
internal class SelectedState<A, B>(
    private val source: StateFlow<A>,
    private val revision: ((A) -> Any)? = null,
    private val transform: (A) -> B
) : StateFlow<B> {
    override val value: B get() = transform(source.value)
    override val replayCache: List<B> get() = listOf(value)

    override suspend fun collect(collector: FlowCollector<B>): Nothing {
        val selector = revision
        val projected = if (selector == null) {
            source.map(transform).distinctUntilChanged()
        } else {
            source.distinctUntilChangedBy(selector).map(transform)
        }
        projected.collect(collector)
        error("StateFlow collection unexpectedly completed")
    }
}

/** Identity equality keeps MutableStateFlow publication independent of payload size. */
internal class StateSnapshot<T>(
    val encoded: String?,
    val result: DataResult<T>,
    val dataRevision: Any = Any(),
    val resultRevision: Any = Any()
)

internal class PreparedState<T>(
    val encoded: String,
    val value: T?,
    val base: StateSnapshot<T>,
    val unchanged: Boolean
)

internal class StoredState<T : Any>(
    private val handle: SavedStateHandle,
    private val key: String,
    serializer: KSerializer<T>,
    private val json: Json,
    default: T?
) {
    private val nullableSerializer = serializer.nullable
    private val snapshots: MutableStateFlow<StateSnapshot<T>>

    init {
        require(key.isNotBlank()) { "State key must not be blank" }
        val encoded: String? = if (handle.contains(key)) {
            handle[key]
        } else {
            json.encodeToString(nullableSerializer, default).also { handle[key] = it }
        }
        val value = encoded?.let { json.decodeFromString(nullableSerializer, it) }
        snapshots = MutableStateFlow(StateSnapshot(encoded, DataResult(value, null, status(value))))
    }

    val current: StateSnapshot<T> get() = snapshots.value
    val flow: StateFlow<T?> = SelectedState(snapshots, { it.dataRevision }) { it.result.data }
    val results: StateFlow<DataResult<T>> =
        SelectedState(snapshots, { it.resultRevision }) { it.result }

    fun set(value: T?) = commit(prepare(value))

    // Encoding, detached decoding and potentially deep equality all happen on the worker for async APIs.
    fun prepare(value: T?): PreparedState<T> {
        val encoded = json.encodeToString(nullableSerializer, value)
        val detached = json.decodeFromString(nullableSerializer, encoded)
        return compare(encoded, detached, current)
    }

    fun rebase(prepared: PreparedState<T>, base: StateSnapshot<T>): PreparedState<T> =
        compare(prepared.encoded, prepared.value, base)

    private fun compare(encoded: String, value: T?, base: StateSnapshot<T>): PreparedState<T> =
        PreparedState(encoded, value, base, value == base.result.data)

    fun commit(
        prepared: PreparedState<T>,
        status: DataResultStatus = status(prepared.value),
        error: Throwable? = null
    ) {
        check(prepared.base === current) { "Prepared state must be rebased before committing" }
        publish(prepared, status, error)
    }

    fun updateStatus(status: DataResultStatus, error: Throwable?) = publish(null, status, error)

    private fun publish(prepared: PreparedState<T>?, status: DataResultStatus, error: Throwable?) {
        val previous = current
        val sameData = prepared == null || prepared.unchanged
        val payload = if (sameData) previous.result.data else prepared.value
        val dataRevision = if (sameData) previous.dataRevision else Any()
        val sameResult =
            sameData && status == previous.result.status && error === previous.result.error
        val result = if (sameResult) previous.result else DataResult(payload, error, status)
        val next = StateSnapshot(
            prepared?.encoded ?: previous.encoded,
            result,
            dataRevision,
            if (sameResult) previous.resultRevision else Any()
        )
        // The key is private: no external handle flows may participate in this transaction.
        prepared?.let { handle[key] = it.encoded }
        // Last action: an immediate collector may synchronously publish a newer snapshot.
        snapshots.value = next
    }

    private fun status(value: T?): DataResultStatus =
        if (value == null) DataResultStatus.NONE else DataResultStatus.SUCCESS
}

/** Derives distinct immediate state. Keep transform pure and cheap; use selectAsync for heavy work. */
fun <A, B> StateFlow<A>.select(
    transform: (A) -> B
): StateFlow<B> = SelectedState(this, transform = transform)
