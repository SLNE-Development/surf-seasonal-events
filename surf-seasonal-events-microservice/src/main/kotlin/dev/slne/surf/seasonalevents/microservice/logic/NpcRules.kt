package dev.slne.surf.seasonalevents.microservice.logic

import dev.slne.surf.seasonalevents.api.progress.NpcReward
import dev.slne.surf.seasonalevents.api.progress.NpcToday
import java.time.LocalDate

/** What the event's NPC tells a player, derived from the pinned days and what the player found. */
object NpcRules {

    /** Whether today's pinned marker is waiting, found, or whether today has none. */
    fun today(pinnedDays: Set<LocalDate>, foundDays: Set<LocalDate>, today: LocalDate): NpcToday = when {
        today !in pinnedDays -> NpcToday.NO_GIFT
        today in foundDays -> NpcToday.GIFT_FOUND
        else -> NpcToday.GIFT_HIDDEN
    }

    /**
     * Where the player stands with the NPC reward. It is ready from [rewardDay] on once every day pinned
     * before it is found; days pinned on or after [rewardDay] do not count.
     *
     * @param rewardDay the fixed day of the filled NPC reward package; null if the event has none
     * @param claimed whether the player already received the reward
     */
    fun reward(
        rewardDay: LocalDate?,
        claimed: Boolean,
        pinnedDays: Set<LocalDate>,
        foundDays: Set<LocalDate>,
        today: LocalDate,
    ): NpcReward {
        if (rewardDay == null) return NpcReward.None
        if (claimed) return NpcReward.Claimed
        val missing = pinnedDays.count { it.isBefore(rewardDay) && it !in foundDays }
        return if (!today.isBefore(rewardDay) && missing == 0) NpcReward.Ready else NpcReward.Waiting(rewardDay, missing)
    }

    /**
     * Total number of pinned days for the "found x of y" line. Days found and unpinned afterwards still
     * count as found, so they also stay in the total.
     */
    fun total(pinnedDays: Set<LocalDate>, foundDays: Set<LocalDate>): Int = (pinnedDays + foundDays).size
}
