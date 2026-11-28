package dev.slne.surf.seasonalevents.microservice.logic

import dev.slne.surf.seasonalevents.api.SeasonalTime
import dev.slne.surf.seasonalevents.api.event.EventMode
import dev.slne.surf.seasonalevents.api.loot.PackageCoverage
import java.time.Instant
import java.time.LocalDate

/**
 * Applies [PackageCoverage] to the stored state of an event, so nobody goes away empty: a player receives
 * each package at most once, so every block the event hands out needs a package that can be given for it.
 * Checked when an event starts and kept while it runs. The NPC reward is not part of it.
 */
object PackageRequirement {

    /**
     * Coverage of the event, or null if it cannot be known: a `DAILY` event with a random pool and no end date
     * then only needs one filled package.
     *
     * @param startDay first event day: the start day of a running event, today for an event about to start
     * @param pinnedDays days that markers are pinned to
     * @param packages the filled packages of the event
     */
    fun coverage(
        mode: EventMode,
        markerCount: Int,
        startDay: LocalDate,
        endsAt: Instant?,
        pinnedDays: Set<LocalDate>,
        packages: List<PackageRef>,
    ): PackageCoverage.Result? {
        val forBlocks = packages.filterNot { it.npcReward }
        return when (mode) {
            EventMode.COLLECT -> PackageCoverage.collect(markerCount, forBlocks.size)
            EventMode.DAILY -> PackageCoverage.daily(
                startDay = startDay,
                endDay = endsAt?.let { LocalDate.ofInstant(it, SeasonalTime.ZONE) },
                markerCount = markerCount,
                pinnedDays = pinnedDays,
                fixedDays = forBlocks.mapNotNull { it.fixedDay },
                randomPackages = forBlocks.count { it.fixedDay == null },
            )
        }
    }
}
