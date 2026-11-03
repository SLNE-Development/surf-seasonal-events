package dev.slne.surf.seasonalevents.api.progress

import dev.slne.surf.seasonalevents.api.event.SeasonalEvent
import dev.slne.surf.seasonalevents.api.marker.MarkerPosition
import kotlinx.serialization.Serializable

/** A player's progress in the active event, shown by `/seasonalevents`. */
@Serializable
sealed interface PlayerStatus {
    /** No event is currently running. */
    @Serializable
    data object NoActiveEvent : PlayerStatus

    /** Progress in a [dev.slne.surf.seasonalevents.api.event.EventMode.DAILY] event. */
    @Serializable
    data class Daily(
        val event: SeasonalEvent,
        /** Where today's marker is; null if the player has no assignment today. */
        val position: MarkerPosition?,
        /** Whether the player already collected today's marker. */
        val collectedToday: Boolean,
        /** Whether the player already received every package of this event. */
        val nothingLeft: Boolean,
    ) : PlayerStatus

    /** Progress in a [dev.slne.surf.seasonalevents.api.event.EventMode.COLLECT] event. */
    @Serializable
    data class Collect(
        val event: SeasonalEvent,
        /** Number of markers the player has found. */
        val found: Int,
        /** Number of markers in the event. */
        val total: Int,
        /** Remaining collections today; null when the event has no daily limit. */
        val remainingToday: Int?,
    ) : PlayerStatus
}
