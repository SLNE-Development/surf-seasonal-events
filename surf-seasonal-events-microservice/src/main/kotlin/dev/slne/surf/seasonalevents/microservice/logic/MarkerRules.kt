package dev.slne.surf.seasonalevents.microservice.logic

import dev.slne.surf.seasonalevents.api.event.EventState
import dev.slne.surf.seasonalevents.api.event.SeasonalEvent
import dev.slne.surf.seasonalevents.api.result.MarkResult

/** Rules for marking blocks as event objects. */
object MarkerRules {

    /**
     * Checks whether a block may be marked in [event]. Marking is allowed in `DRAFT` and `ACTIVE`.
     *
     * @param markerCount number of markers the event already has
     * @param positionTaken whether the position is already marked by any event
     * @return null if the block may be marked
     */
    fun checkMark(event: SeasonalEvent, markerCount: Int, positionTaken: Boolean): MarkResult? {
        if (event.state == EventState.ENDED) return MarkResult.EventEnded
        if (positionTaken) return MarkResult.AlreadyMarked
        if (markerCount >= event.markerLimit) return MarkResult.LimitReached(event.markerLimit)
        return null
    }
}
