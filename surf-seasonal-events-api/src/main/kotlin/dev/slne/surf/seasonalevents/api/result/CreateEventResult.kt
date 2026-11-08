package dev.slne.surf.seasonalevents.api.result

import dev.slne.surf.seasonalevents.api.event.SeasonalEvent
import kotlinx.serialization.Serializable

/** Outcome of creating a new seasonal event. */
@Serializable
sealed interface CreateEventResult {
    /** The event was created in state DRAFT. */
    @Serializable
    data class Success(val event: SeasonalEvent) : CreateEventResult

    /** Another event already uses this name. */
    @Serializable
    data object NameTaken : CreateEventResult

    /** The name is blank or longer than [dev.slne.surf.seasonalevents.api.event.SeasonalEvent.MAX_NAME_LENGTH]. */
    @Serializable
    data object InvalidName : CreateEventResult

    /** The marker limit is not positive or the daily limit is negative. */
    @Serializable
    data object InvalidLimit : CreateEventResult
}
