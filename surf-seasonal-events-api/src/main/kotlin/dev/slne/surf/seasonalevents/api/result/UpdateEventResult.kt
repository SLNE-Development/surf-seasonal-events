package dev.slne.surf.seasonalevents.api.result

import dev.slne.surf.seasonalevents.api.event.SeasonalEvent
import kotlinx.serialization.Serializable

/** Outcome of changing an event's display name or end date. */
@Serializable
sealed interface UpdateEventResult {
    /** The event was updated; contains its new state. */
    @Serializable
    data class Success(val event: SeasonalEvent) : UpdateEventResult

    /** No event with the given id exists. */
    @Serializable
    data object NotFound : UpdateEventResult

    /** The event has ended and can no longer be changed. */
    @Serializable
    data object AlreadyEnded : UpdateEventResult

    /**
     * The new end date would give the running `DAILY` event more days than filled packages
     * ([StartEventResult.NotEnoughPackages] explains the rule); nothing was changed.
     */
    @Serializable
    data class NotEnoughPackages(val filled: Int, val needed: Int) : UpdateEventResult

    /** The display name is blank or longer than [dev.slne.surf.seasonalevents.api.event.SeasonalEvent.MAX_DISPLAY_NAME_LENGTH]. */
    @Serializable
    data object InvalidDisplayName : UpdateEventResult
}
