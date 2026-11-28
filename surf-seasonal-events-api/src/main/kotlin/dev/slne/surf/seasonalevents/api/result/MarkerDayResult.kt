package dev.slne.surf.seasonalevents.api.result

import dev.slne.surf.seasonalevents.api.marker.EventMarker
import kotlinx.serialization.Serializable

/** Outcome of pinning a marker to a day or releasing it into the random pool. */
@Serializable
sealed interface MarkerDayResult {
    /** The day was saved; contains the marker's new state. */
    @Serializable
    data class Success(val marker: EventMarker) : MarkerDayResult

    /** The marker or its event does not exist. */
    @Serializable
    data object NotFound : MarkerDayResult

    /** Days can only be pinned in DAILY events. */
    @Serializable
    data object NotDailyEvent : MarkerDayResult

    /** Another marker of the event is already pinned to this day. */
    @Serializable
    data object DayTaken : MarkerDayResult

    /** The event has ended; its markers can no longer be changed. */
    @Serializable
    data object EventEnded : MarkerDayResult

    /**
     * The running event would then hand out blocks on more days than it has filled packages
     * ([StartEventResult.NotEnoughPackages] explains the rule); nothing was changed.
     */
    @Serializable
    data class NotEnoughPackages(val filled: Int, val needed: Int) : MarkerDayResult
}
