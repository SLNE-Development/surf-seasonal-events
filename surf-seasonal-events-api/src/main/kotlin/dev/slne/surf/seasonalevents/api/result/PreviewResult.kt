package dev.slne.surf.seasonalevents.api.result

import kotlinx.serialization.Serializable

/** Outcome of asking which package a click would give, without booking anything. */
@Serializable
sealed interface PreviewResult {
    /** Collecting is allowed; [items] is the serialized content of package [packageId]. */
    @Serializable
    class Preview(val packageId: ULong, val items: ByteArray) : PreviewResult {
        override fun equals(other: Any?) =
            other is Preview && other.packageId == packageId && other.items.contentEquals(items)

        override fun hashCode() = 31 * packageId.hashCode() + items.contentHashCode()
    }

    /** The marker belongs to another player's daily assignment. */
    @Serializable
    data object NotYours : PreviewResult

    /** The player already collected this marker (COLLECT) or today's marker (DAILY). */
    @Serializable
    data object AlreadyCollected : PreviewResult

    /** The player reached the event's daily limit. */
    @Serializable
    data object DailyLimitReached : PreviewResult

    /** The player already received every package. */
    @Serializable
    data object NothingLeft : PreviewResult

    /** No event is currently active. */
    @Serializable
    data object NoActiveEvent : PreviewResult
}
