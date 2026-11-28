package dev.slne.surf.seasonalevents.microservice.logic

import dev.slne.surf.seasonalevents.api.event.EventState
import dev.slne.surf.seasonalevents.api.event.SeasonalEvent
import dev.slne.surf.seasonalevents.api.loot.PackageCoverage
import dev.slne.surf.seasonalevents.api.result.CreateEventResult
import dev.slne.surf.seasonalevents.api.result.DeleteEventResult
import dev.slne.surf.seasonalevents.api.result.StartEventResult
import dev.slne.surf.seasonalevents.api.result.UpdateEventResult
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import java.time.Instant

/** Rules for creating, changing, starting, ending and deleting events. Each check returns null if allowed. */
object EventRules {

    /**
     * Refuses a blank or too long name, a marker limit below 1 or a negative daily limit
     * (0 means unlimited).
     */
    fun checkCreate(name: String, markerLimit: Int, dailyLimit: Int): CreateEventResult? {
        if (name.isBlank() || name.length > SeasonalEvent.MAX_NAME_LENGTH) return CreateEventResult.InvalidName
        if (markerLimit <= 0 || dailyLimit < 0) return CreateEventResult.InvalidLimit
        return null
    }

    /** Refuses changes to an ended event and a display name whose visible text is blank or too long. */
    fun checkDisplayName(event: SeasonalEvent, displayName: Component): UpdateEventResult? {
        if (event.state == EventState.ENDED) return UpdateEventResult.AlreadyEnded
        val visible = PlainTextComponentSerializer.plainText().serialize(displayName)
        if (visible.isBlank() || visible.length > SeasonalEvent.MAX_DISPLAY_NAME_LENGTH) {
            return UpdateEventResult.InvalidDisplayName
        }
        return null
    }

    /**
     * Checks whether [target] may start. Only one event can be active at a time, and an event needs at
     * least one marker and packages that cover every block it hands out (see [PackageRequirement]).
     *
     * @param packageCount number of packages with at least one item that blocks can hand out
     * @param coverage from [PackageRequirement.coverage]; null if one package is enough
     */
    fun checkStart(
        target: SeasonalEvent,
        active: SeasonalEvent?,
        markerCount: Int,
        packageCount: Int,
        coverage: PackageCoverage.Result?,
    ): StartEventResult? {
        if (target.state == EventState.ENDED) return StartEventResult.AlreadyEnded
        if (active != null) return StartEventResult.AnotherEventActive(active.name)
        if (markerCount == 0) return StartEventResult.NoMarkers
        if (packageCount == 0) return StartEventResult.NoPackages
        if (coverage != null && coverage.isShort) {
            return StartEventResult.NotEnoughPackages(filled = coverage.covered, needed = coverage.needed)
        }
        return null
    }

    /**
     * Checks whether [event] may be deleted. A running event must be stopped first, and markers must be
     * restored first: their real blocks are missing from the world and would otherwise be lost.
     */
    fun checkDelete(event: SeasonalEvent, markerCount: Int): DeleteEventResult? {
        if (event.state == EventState.ACTIVE) return DeleteEventResult.IsActive
        if (markerCount > 0) return DeleteEventResult.HasMarkers(markerCount)
        return null
    }

    /** Whether [event] has an end and [now] has reached it. */
    fun isExpired(event: SeasonalEvent, now: Instant): Boolean {
        val end = event.endsAt ?: return false
        return !now.isBefore(end)
    }
}
