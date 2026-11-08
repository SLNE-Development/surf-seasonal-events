package dev.slne.surf.seasonalevents.microservice.logic

import dev.slne.surf.seasonalevents.api.event.EventMode
import dev.slne.surf.seasonalevents.api.event.EventState
import dev.slne.surf.seasonalevents.api.event.SeasonalEvent
import dev.slne.surf.seasonalevents.api.result.PreviewResult

/** Rules for collecting a marker. Whether a package is left is decided by [PackagePicker]. */
object CollectRules {

    /**
     * Checks whether a player may collect a marker.
     *
     * @param event the active event, or null if none is running
     * @param ownsMarker whether the marker is visible to the player: today's assignment in `DAILY`,
     * any marker of the event in `COLLECT`
     * @param alreadyCollected whether the player already collected today (`DAILY`) or this marker (`COLLECT`)
     * @param collectedToday number of collections of the player today; only checked against the daily limit
     * of a `COLLECT` event
     * @return null if the player may collect
     */
    fun check(
        event: SeasonalEvent?,
        ownsMarker: Boolean,
        alreadyCollected: Boolean,
        collectedToday: Int,
    ): PreviewResult? {
        if (event == null || event.state != EventState.ACTIVE) return PreviewResult.NoActiveEvent
        if (!ownsMarker) return PreviewResult.NotYours
        if (alreadyCollected) return PreviewResult.AlreadyCollected
        if (event.mode == EventMode.COLLECT && event.dailyLimit > 0 && collectedToday >= event.dailyLimit) {
            return PreviewResult.DailyLimitReached
        }
        return null
    }
}
