package dev.slne.surf.seasonalevents.api.result

import kotlinx.serialization.Serializable

/** Outcome of deleting an event together with its packages and progress. */
@Serializable
sealed interface DeleteEventResult {
    /** The event was deleted. */
    @Serializable
    data object Success : DeleteEventResult

    /** The event is active; stop it first. */
    @Serializable
    data object IsActive : DeleteEventResult

    /**
     * The event still has markers whose real blocks are missing from the world; restore them first,
     * otherwise those blocks would be lost for good.
     */
    @Serializable
    data class HasMarkers(val markerCount: Int) : DeleteEventResult

    /** No event with the given id exists. */
    @Serializable
    data object NotFound : DeleteEventResult
}
