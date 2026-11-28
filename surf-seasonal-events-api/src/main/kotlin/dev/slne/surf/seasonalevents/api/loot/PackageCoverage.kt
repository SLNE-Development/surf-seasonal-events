package dev.slne.surf.seasonalevents.api.loot

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Whether the filled packages of an event can serve every block it hands out, because a player receives
 * each package at most once. Shared by the microservice, which refuses starts and changes that cause a
 * shortage, and the loot GUI, which shows the same numbers. The NPC reward is never part of it.
 */
object PackageCoverage {

    /** [covered] packages can be handed out for blocks, [needed] blocks hand out one each. */
    data class Result(val covered: Int, val needed: Int) {
        val isShort get() = covered < needed
    }

    /** `COLLECT`: every found marker gives one package, of any kind. */
    fun collect(markerCount: Int, filledPackages: Int) = Result(covered = filledPackages, needed = markerCount)

    /**
     * `DAILY`: every day that hands out a block needs one package. While the random pool has markers that
     * is every day from [startDay] to [endDay]; without one only the pinned days up to [endDay] do, including
     * pinned days before [startDay], because those can be caught up. A package with a fixed day only serves
     * that day, so it counts only if that day hands out a block; random packages serve any day.
     *
     * @param markerCount all markers of the event, pinned or not
     * @param pinnedDays days that markers are pinned to; each pinned marker has its own day
     * @param fixedDays fixed days of the filled packages, NPC reward excluded
     * @param randomPackages filled packages without a fixed day, NPC reward excluded
     * @return null if the number of days is unknown: a random pool without an end date
     */
    fun daily(
        startDay: LocalDate,
        endDay: LocalDate?,
        markerCount: Int,
        pinnedDays: Set<LocalDate>,
        fixedDays: Collection<LocalDate>,
        randomPackages: Int,
    ): Result? {
        val hasRandomPool = pinnedDays.isEmpty() || markerCount > pinnedDays.size

        fun beforeEnd(day: LocalDate) = endDay == null || !day.isAfter(endDay)
        fun handsOutBlock(day: LocalDate) =
            beforeEnd(day) && (day in pinnedDays || (hasRandomPool && !day.isBefore(startDay)))

        val needed = if (hasRandomPool) {
            val end = endDay ?: return null
            // Counted, not listed: an event of any length costs the same.
            val eventDays = (ChronoUnit.DAYS.between(startDay, end) + 1).coerceAtLeast(0).toInt()
            eventDays + pinnedDays.count { it.isBefore(startDay) && beforeEnd(it) }
        } else {
            pinnedDays.count(::beforeEnd)
        }
        val coveredByFixed = fixedDays.toSet().count(::handsOutBlock)
        return Result(covered = coveredByFixed + randomPackages, needed = needed)
    }
}
