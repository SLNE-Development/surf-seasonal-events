package dev.slne.surf.seasonalevents.api.result

import dev.slne.surf.seasonalevents.api.marker.EventMarker
import kotlinx.serialization.Serializable

/** Outcome of removing a marker. */
@Serializable
sealed interface UnmarkResult {
    /**
     * The marker was removed; contains its block data so the caller can restore the real block.
     *
     * @param markerCount markers the event has left
     */
    @Serializable
    data class Success(val marker: EventMarker, val markerCount: Int, val markerLimit: Int) : UnmarkResult

    /** There is no marker at this position. */
    @Serializable
    data object NotMarked : UnmarkResult
}
