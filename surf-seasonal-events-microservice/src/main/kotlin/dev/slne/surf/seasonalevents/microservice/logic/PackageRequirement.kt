package dev.slne.surf.seasonalevents.microservice.logic

import dev.slne.surf.seasonalevents.api.SeasonalTime
import dev.slne.surf.seasonalevents.api.event.EventMode
import java.time.Instant
import java.time.LocalDate

/**
 * How many filled packages an event needs so nobody goes away empty, because a player receives each
 * package at most once: one per marker in `COLLECT` (every found block gives a package), one per day that
 * hands out a block in `DAILY`. Checked when an event starts and kept while it runs. The NPC reward is not
 * part of this count; it is handed out on its own.
 */
object PackageRequirement {

    /** A rule violation: the event would have [filled] packages but needs [needed]. */
    data class Shortage(val filled: Int, val needed: Int)

    /**
     * Packages needed, or null if there is no requirement beyond one package: a `DAILY` event
     * without an end date has no known number of days.
     *
     * In `DAILY`, every event day hands out a block while the random pool has markers. Without a random
     * pool only the pinned days do, including pinned days before [startDay], because those can be caught up.
     *
     * @param startDay first event day: the start day of a running event, today for an event about to start
     * @param pinnedDays days that markers are pinned to; each pinned marker has its own day
     */
    fun required(
        mode: EventMode,
        markerCount: Int,
        startDay: LocalDate,
        endsAt: Instant?,
        pinnedDays: Set<LocalDate> = emptySet(),
    ): Int? = when (mode) {
        EventMode.COLLECT -> markerCount
        EventMode.DAILY -> endsAt?.let { end ->
            val endDay = LocalDate.ofInstant(end, SeasonalTime.ZONE)
            val hasRandomPool = pinnedDays.isEmpty() || markerCount > pinnedDays.size
            val eventDays = if (hasRandomPool) {
                generateSequence(startDay) { it.plusDays(1) }.takeWhile { !it.isAfter(endDay) }.toSet()
            } else {
                emptySet()
            }
            (eventDays + pinnedDays.filter { !it.isAfter(endDay) }).size
        }
    }

    /** Returns the shortage if [filled] packages do not meet [needed], null if they do. */
    fun check(filled: Int, needed: Int?): Shortage? =
        if (needed != null && filled < needed) Shortage(filled, needed) else null
}
