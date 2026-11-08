package dev.slne.surf.seasonalevents.api.result

import dev.slne.surf.seasonalevents.api.marker.EventMarker
import kotlinx.serialization.Serializable

/** Outcome of marking a block as an event object. */
@Serializable
sealed interface MarkResult {
    /**
     * The block is now a marker; the caller removes the real block.
     *
     * @param markerCount markers of the event including this one
     */
    @Serializable
    data class Success(val marker: EventMarker, val markerCount: Int, val markerLimit: Int) : MarkResult

    /** The event already has [limit] markers. */
    @Serializable
    data class LimitReached(val limit: Int) : MarkResult

    /** This position is already marked, in this or another event. */
    @Serializable
    data object AlreadyMarked : MarkResult

    /**
     * The `COLLECT` event is running and would then have more markers than filled packages;
     * fill another package first.
     */
    @Serializable
    data class NotEnoughPackages(val filled: Int, val needed: Int) : MarkResult

    /** The event has ended; markers can no longer be added. */
    @Serializable
    data object EventEnded : MarkResult

    /** No event with the given id exists. */
    @Serializable
    data object NotFound : MarkResult
}
