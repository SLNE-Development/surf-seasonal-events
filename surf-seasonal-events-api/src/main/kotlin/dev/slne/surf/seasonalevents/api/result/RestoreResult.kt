package dev.slne.surf.seasonalevents.api.result

import dev.slne.surf.seasonalevents.api.marker.EventMarker
import kotlinx.serialization.Serializable

/** Outcome of restoring all real blocks of an event. */
@Serializable
sealed interface RestoreResult {
    /** All markers were removed; the caller places these blocks back into the world. */
    @Serializable
    data class Success(val markers: List<EventMarker>) : RestoreResult

    /** The event is active; stop it first. */
    @Serializable
    data object IsActive : RestoreResult

    /** No event with the given id exists. */
    @Serializable
    data object NotFound : RestoreResult
}
