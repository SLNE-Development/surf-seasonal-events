package dev.slne.surf.seasonalevents.microservice.rpc

import dev.slne.surf.seasonalevents.api.SeasonalTime
import dev.slne.surf.seasonalevents.api.event.EventState
import dev.slne.surf.seasonalevents.api.event.SeasonalEvent
import dev.slne.surf.seasonalevents.microservice.db.repository.LootRepository
import dev.slne.surf.seasonalevents.microservice.db.repository.MarkerRepository
import dev.slne.surf.seasonalevents.microservice.logic.DailySchedule
import dev.slne.surf.seasonalevents.microservice.logic.PackageRequirement
import dev.slne.surf.seasonalevents.microservice.logic.PackageRequirement.Shortage
import java.time.Instant
import java.time.LocalDate

/**
 * Keeps the [PackageRequirement] of a running event when markers, packages or the end date change.
 * Call it inside the transaction and under [StaffWriteLock] of the change it guards.
 */
internal object PackageGuard {

    /**
     * Returns the shortage [event] would have after a change, or null if it still has enough packages
     * or is not running (drafts are checked when they start).
     *
     * @param addedMarkers markers the change adds
     * @param removedPackages filled packages the change deletes or empties
     * @param endsAt the end date after the change
     * @param pinnedDays the days markers are pinned to after the change; null for the stored ones
     */
    suspend fun shortageAfter(
        event: SeasonalEvent,
        addedMarkers: Int = 0,
        removedPackages: Int = 0,
        endsAt: Instant? = event.endsAt,
        pinnedDays: Set<LocalDate>? = null,
    ): Shortage? {
        if (event.state != EventState.ACTIVE) return null
        val startedAt = event.startedAt ?: return null

        val markers = MarkerRepository.findByEvent(event.id)
        val needed = PackageRequirement.required(
            mode = event.mode,
            markerCount = markers.size + addedMarkers,
            startDay = LocalDate.ofInstant(startedAt, SeasonalTime.ZONE),
            endsAt = endsAt,
            pinnedDays = pinnedDays ?: DailySchedule.pinnedDays(markers),
        )
        return PackageRequirement.check(LootRepository.countFilled(event.id) - removedPackages, needed)
    }
}
