package dev.slne.surf.seasonalevents.api.result

import dev.slne.surf.seasonalevents.api.event.SeasonalEvent
import kotlinx.serialization.Serializable

/** Outcome of stopping the active event. */
@Serializable
sealed interface StopEventResult {
    /** The active event has ended; contains its final state. */
    @Serializable
    data class Success(val event: SeasonalEvent) : StopEventResult

    /** No event is currently active. */
    @Serializable
    data object NoActiveEvent : StopEventResult
}
