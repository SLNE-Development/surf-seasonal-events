package dev.slne.surf.seasonalevents.api.progress

import dev.slne.surf.seasonalevents.api.event.SeasonalEvent
import kotlinx.serialization.Serializable

/** A player's situation on the current day of a daily event. */
@Serializable
enum class DailyToday {
    /** Today's block is waiting to be found. */
    HIDDEN,

    /** The player already found today's block. */
    COLLECTED,

    /** The player already received every package of the event. */
    NOTHING_LEFT,

    /** The player has no block today. */
    NO_BLOCK,
}

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
        /** How today looks for the player. */
        val today: DailyToday,
        /** Number of packages the player has received. */
        val found: Int,
        /** Number of packages the player can receive in total: those received plus those still open. */
        val total: Int,
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
