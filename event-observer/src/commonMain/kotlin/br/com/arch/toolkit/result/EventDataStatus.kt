package br.com.arch.toolkit.result

/**
 * Filter that controls whether an event should react to data presence.
 */
enum class EventDataStatus {

    /**
     * Run only when [DataResult.data] is not `null`.
     */
    WithData,

    /**
     * Run only when [DataResult.data] is `null`.
     */
    WithoutData,

    /**
     * Ignore data presence and evaluate the event normally.
     */
    DoesNotMatter;

    /**
     * Returns `true` when this filter allows the supplied [DataResult].
     */
    fun considerEvent(result: DataResult<*>) = when (this) {
        WithData -> result.hasData
        WithoutData -> !result.hasData
        DoesNotMatter -> true
    }
}
