package dev.slne.surf.seasonalevents.api.progress

import dev.slne.surf.seasonalevents.api.event.SeasonalEvent
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.LocalDate

/** Whether today's pinned marker of a `DAILY` event is still waiting for the player. */
enum class NpcToday {
    /** Today has a pinned marker the player has not found yet. */
    GIFT_HIDDEN,

    /** The player already found today's pinned marker. */
    GIFT_FOUND,

    /** No marker is pinned to today. */
    NO_GIFT,
}

/** Where the player stands with the reward the event's NPC hands out. */
@Serializable
sealed interface NpcReward {
    /** The event has no filled package marked as NPC reward. */
    @Serializable
    data object None : NpcReward

    /**
     * The reward is not available yet: either [day] has not come, or [missing] pinned markers of the days
     * before it are still to be found.
     */
    @Serializable
    data class Waiting(val day: @Contextual LocalDate, val missing: Int) : NpcReward

    /** The player can collect the reward now. */
    @Serializable
    data object Ready : NpcReward

    /** The player already received the reward. */
    @Serializable
    data object Claimed : NpcReward
}

/** What the event's NPC knows about a player when they talk to it. */
@Serializable
sealed interface NpcStatus {
    /** No event is currently running. */
    @Serializable
    data object NoActiveEvent : NpcStatus

    @Serializable
    data class Active(
        val event: SeasonalEvent,
        /** Whether this is the player's first talk to the NPC in this event. */
        val firstVisit: Boolean,
        val today: NpcToday,
        /** Pinned markers of earlier days the player has not found yet. */
        val openEarlier: Int,
        /** Pinned markers (days) the player has found. */
        val found: Int,
        /** Pinned markers (days) of the event, found ones included. */
        val total: Int,
        val reward: NpcReward,
    ) : NpcStatus
}
