package br.com.arch.toolkit.compose

import br.com.arch.toolkit.result.DataResult
import br.com.arch.toolkit.result.DataResultStatus

/**
 * Animation identity for a [DataResult].
 *
 * Payload changes that keep the same status and data/error presence should recompose
 * content without restarting the whole `AnimatedContent` transition.
 */
internal fun DataResult<*>.animationContentKey(): Any = DataResultAnimationKey(
    status = status,
    hasData = hasData,
    hasError = hasError,
    collectionShape = collectionShape()
)

private fun DataResult<*>.collectionShape(): CollectionShape = when (val value = data) {
    is Collection<*> -> when (value.size) {
        0 -> CollectionShape.Empty
        1 -> CollectionShape.Single
        else -> CollectionShape.Many
    }

    is Map<*, *> -> when (value.size) {
        0 -> CollectionShape.Empty
        1 -> CollectionShape.Single
        else -> CollectionShape.Many
    }

    is Sequence<*> -> CollectionShape.Sequence
    else -> CollectionShape.NotCollection
}

private data class DataResultAnimationKey(
    val status: DataResultStatus,
    val hasData: Boolean,
    val hasError: Boolean,
    val collectionShape: CollectionShape
)

private enum class CollectionShape {
    NotCollection,
    Sequence,
    Empty,
    Single,
    Many
}
