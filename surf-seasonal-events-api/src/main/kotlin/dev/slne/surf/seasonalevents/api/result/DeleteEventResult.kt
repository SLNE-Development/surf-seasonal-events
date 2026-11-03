package dev.slne.surf.seasonalevents.api.result

import kotlinx.serialization.Serializable

/** Outcome of deleting an event together with its markers, packages and progress. */
@Serializable
sealed interface DeleteEventResult {
    /** The event was deleted. */
    @Serializable
    data object Success : DeleteEventResult

    /** The event is active; stop it first. */
    @Serializable
    data object IsActive : DeleteEventResult

    /** No event with the given id exists. */
    @Serializable
    data object NotFound : DeleteEventResult
}
