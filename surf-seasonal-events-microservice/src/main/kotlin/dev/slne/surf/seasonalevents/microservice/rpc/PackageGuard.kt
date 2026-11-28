package dev.slne.surf.seasonalevents.microservice.rpc

import dev.slne.surf.seasonalevents.api.SeasonalTime
import dev.slne.surf.seasonalevents.api.event.EventState
import dev.slne.surf.seasonalevents.api.event.SeasonalEvent
import dev.slne.surf.seasonalevents.api.loot.PackageCoverage
import dev.slne.surf.seasonalevents.microservice.db.repository.LootRepository
import dev.slne.surf.seasonalevents.microservice.db.repository.MarkerRepository
import dev.slne.surf.seasonalevents.microservice.logic.DailySchedule
import dev.slne.surf.seasonalevents.microservice.logic.PackageRef
import dev.slne.surf.seasonalevents.microservice.logic.PackageRequirement
import java.time.Instant
import java.time.LocalDate

/**
 * Keeps the [PackageRequirement] of a running event when markers, packages, fixed days or the end date
 * change. Call it inside the transaction and under [StaffWriteLock] of the change it guards.
 */
internal object PackageGuard {

    /**
     * Returns the shortage [event] would have after a change, or null if its packages still cover every
     * block or it is not running (drafts are checked when they start).
     *
     * @param addedMarkers markers the change adds; new markers are never pinned
     * @param endsAt the end date after the change
     * @param pinnedDays the days markers are pinned to after the change; null for the stored ones
     * @param packages turns the stored filled packages into those after the change
     */
    suspend fun shortageAfter(
        event: SeasonalEvent,
        addedMarkers: Int = 0,
        endsAt: Instant? = event.endsAt,
        pinnedDays: Set<LocalDate>? = null,
        packages: (List<PackageRef>) -> List<PackageRef> = { it },
    ): PackageCoverage.Result? {
        if (event.state != EventState.ACTIVE) return null
        val startedAt = event.startedAt ?: return null

        val markers = MarkerRepository.findByEvent(event.id)
        return PackageRequirement.coverage(
            mode = event.mode,
            markerCount = markers.size + addedMarkers,
            startDay = LocalDate.ofInstant(startedAt, SeasonalTime.ZONE),
            endsAt = endsAt,
            pinnedDays = pinnedDays ?: DailySchedule.pinnedDays(markers),
            packages = packages(LootRepository.findFilledRefs(event.id)),
        )?.takeIf { it.isShort }
    }

    /** The packages without [packageId], for a change that empties or deletes it. */
    fun without(packageId: ULong): (List<PackageRef>) -> List<PackageRef> = { refs -> refs.filterNot { it.id == packageId } }

    /** The packages with [packageId] changed by [change], for a change of its fixed day or reward flag. */
    fun changed(packageId: ULong, change: (PackageRef) -> PackageRef): (List<PackageRef>) -> List<PackageRef> =
        { refs -> refs.map { if (it.id == packageId) change(it) else it } }
}
