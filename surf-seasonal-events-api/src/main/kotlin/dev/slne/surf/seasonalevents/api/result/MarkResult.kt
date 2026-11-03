package dev.slne.surf.seasonalevents.api.result

import dev.slne.surf.seasonalevents.api.marker.EventMarker
import kotlinx.serialization.Serializable

/** Outcome of marking a block as an event object. */
@Serializable
sealed interface MarkResult {
    /** The block is now a marker; the caller removes the real block. */
    @Serializable
    data class Success(val marker: EventMarker) : MarkResult

    /** The event already has [limit] markers. */
    @Serializable
    data class LimitReached(val limit: Int) : MarkResult

    /** This position is already marked, in this or another event. */
    @Serializable
    data object AlreadyMarked : MarkResult

    /** The event has ended; markers can no longer be added. */
    @Serializable
    data object EventEnded : MarkResult

    /** No event with the given id exists. */
    @Serializable
    data object NotFound : MarkResult
}
