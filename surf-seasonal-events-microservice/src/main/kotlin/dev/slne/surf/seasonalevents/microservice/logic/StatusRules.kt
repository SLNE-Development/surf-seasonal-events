package dev.slne.surf.seasonalevents.microservice.logic

/** Numbers shown by `/seasonalevents` for a `COLLECT` event. */
object StatusRules {

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
