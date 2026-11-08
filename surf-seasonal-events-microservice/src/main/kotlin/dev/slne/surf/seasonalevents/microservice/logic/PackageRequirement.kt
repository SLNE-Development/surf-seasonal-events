package dev.slne.surf.seasonalevents.microservice.logic

import dev.slne.surf.seasonalevents.api.SeasonalTime
import dev.slne.surf.seasonalevents.api.event.EventMode
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * How many filled packages an event needs so nobody goes away empty, because a player receives each
 * package at most once: one per marker in `COLLECT` (every found block gives a package), one per event
 * day in `DAILY` (every day gives a package). Checked when an event starts and kept while it runs.
 */
object PackageRequirement {

    /** A rule violation: the event would have [filled] packages but needs [needed]. */
    data class Shortage(val filled: Int, val needed: Int)

    /**
     * Packages needed, or null if there is no requirement beyond one package: a `DAILY` event
     * without an end date has no known number of days.
     *
     * @param startDay first event day: the start day of a running event, today for an event about to start
     */
    fun required(mode: EventMode, markerCount: Int, startDay: LocalDate, endsAt: Instant?): Int? = when (mode) {
        EventMode.COLLECT -> markerCount
        EventMode.DAILY -> endsAt?.let { end ->
            val endDay = LocalDate.ofInstant(end, SeasonalTime.ZONE)
            (ChronoUnit.DAYS.between(startDay, endDay) + 1).toInt().coerceAtLeast(0)
        }
    }

    /** Returns the shortage if [filled] packages do not meet [needed], null if they do. */
    fun check(filled: Int, needed: Int?): Shortage? =
        if (needed != null && filled < needed) Shortage(filled, needed) else null
}
