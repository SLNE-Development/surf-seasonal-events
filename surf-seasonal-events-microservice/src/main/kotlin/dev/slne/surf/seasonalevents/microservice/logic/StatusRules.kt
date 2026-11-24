package dev.slne.surf.seasonalevents.microservice.logic

import dev.slne.surf.seasonalevents.api.progress.DailyToday

/** What `/seasonalevents` shows for the active event. */
object StatusRules {

    /**
     * The player's situation today in a `DAILY` event. Having found today's block wins over having
     * received everything, so the last block of the event still reads as found until the next day.
     *
     * @param collectedToday whether the player already collected today
     * @param hasBlock whether the player has an existing block assigned today
     * @param openPackages filled packages the player has not received yet
     */
    fun dailyToday(collectedToday: Boolean, hasBlock: Boolean, openPackages: Int): DailyToday = when {
        collectedToday -> DailyToday.COLLECTED
        openPackages == 0 -> DailyToday.NOTHING_LEFT
        hasBlock -> DailyToday.HIDDEN
        else -> DailyToday.NO_BLOCK
    }

    /**
     * Total number of markers for the "found x of y" line. Markers unmarked after the player found them
     * still count as found, so they also stay in the total.
     *
     * @param found all collections of the player, including those of unmarked markers
     * @param foundStillMarked collections whose marker still exists
     * @param markerCount markers the event currently has
     */
    fun collectTotal(found: Int, foundStillMarked: Int, markerCount: Int): Int =
        markerCount + (found - foundStillMarked)

    /** Collections left today, or null if the event has no daily limit (0). */
    fun remainingToday(dailyLimit: Int, collectedToday: Int): Int? =
        if (dailyLimit > 0) (dailyLimit - collectedToday).coerceAtLeast(0) else null
}
