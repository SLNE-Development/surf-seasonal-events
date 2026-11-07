package dev.slne.surf.seasonalevents.microservice.logic

import dev.slne.surf.seasonalevents.api.event.EventState
import dev.slne.surf.seasonalevents.api.event.SeasonalEvent
import dev.slne.surf.seasonalevents.api.result.CreateEventResult
import dev.slne.surf.seasonalevents.api.result.StartEventResult
import java.time.Instant

/** Rules for creating, starting and ending events. Each check returns null if the action is allowed. */
object EventRules {

    /** Refuses a marker limit below 1 or a negative daily limit (0 means unlimited). */
    fun checkCreate(markerLimit: Int, dailyLimit: Int): CreateEventResult? {
        if (markerLimit <= 0 || dailyLimit < 0) return CreateEventResult.InvalidLimit
        return null
    }

    /**
     * Checks whether [target] may start. Only one event can be active at a time,
     * and an event needs at least one marker and one package.
     */
    fun checkStart(
        target: SeasonalEvent,
        active: SeasonalEvent?,
        markerCount: Int,
        packageCount: Int,
    ): StartEventResult? {
        if (target.state == EventState.ENDED) return StartEventResult.AlreadyEnded
        if (active != null) return StartEventResult.AnotherEventActive(active.name)
        if (markerCount == 0) return StartEventResult.NoMarkers
        if (packageCount == 0) return StartEventResult.NoPackages
        return null
    }

    /** Whether [event] has an end and [now] has reached it. */
    fun isExpired(event: SeasonalEvent, now: Instant): Boolean {
        val end = event.endsAt ?: return false
        return !now.isBefore(end)
    }
}
