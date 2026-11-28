package dev.slne.surf.seasonalevents.microservice.logic

import dev.slne.surf.seasonalevents.api.event.EventMode
import dev.slne.surf.seasonalevents.api.loot.PackageCoverage
import java.time.Instant
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Verifies whether an event's packages cover every block it hands out. */
class PackageRequirementTest {

    private fun day(dayOfMonth: Int) = LocalDate.of(2026, 12, dayOfMonth)

    private val firstOfDecember = day(1)

    /** 24.12.2026 23:59 in Berlin. */
    private val christmasEveEnd = Instant.parse("2026-12-24T22:59:00Z")

    private var nextId = 1uL
    private fun random(count: Int) = List(count) { PackageRef(nextId++, fixedDay = null) }
    private fun fixed(vararg days: LocalDate) = days.map { PackageRef(nextId++, fixedDay = it) }
    private fun reward(day: LocalDate) = PackageRef(nextId++, fixedDay = day, npcReward = true)

    private fun daily(
        markerCount: Int,
        packages: List<PackageRef>,
        endsAt: Instant? = christmasEveEnd,
        pinned: Set<LocalDate> = emptySet(),
        startDay: LocalDate = firstOfDecember,
    ) = PackageRequirement.coverage(EventMode.DAILY, markerCount, startDay, endsAt, pinned, packages)

    @Test
    fun `collect event needs one package per marker`() {
        val coverage = PackageRequirement.coverage(EventMode.COLLECT, 40, firstOfDecember, null, emptySet(), random(39))
        assertEquals(PackageCoverage.Result(covered = 39, needed = 40), coverage)
    }

    @Test
    fun `daily event with a random pool needs one package per day, both ends included`() {
        assertEquals(PackageCoverage.Result(covered = 23, needed = 24), daily(markerCount = 5, packages = random(23)))
    }

    @Test
    fun `daily end is counted on the Berlin calendar day`() {
        // 23:30 UTC on the 24th is already the 25th in Berlin.
        val end = Instant.parse("2026-12-24T23:30:00Z")
        assertEquals(25, daily(markerCount = 5, packages = random(1), endsAt = end)?.needed)
    }

    @Test
    fun `random pool without end has no known requirement`() {
        assertNull(daily(markerCount = 5, packages = random(1), endsAt = null))
    }

    @Test
    fun `end before the start needs nothing`() {
        assertEquals(0, daily(markerCount = 5, packages = random(1), endsAt = Instant.parse("2026-11-20T10:00:00Z"))?.needed)
    }

    @Test
    fun `christmas setup with 23 pinned days, 23 packages and a reward is covered until new year's eve`() {
        val pinned = (1..23).map(::day).toSet()
        val newYearsEve = Instant.parse("2026-12-31T22:59:00Z")
        val coverage = daily(23, random(23) + reward(day(24)), endsAt = newYearsEve, pinned = pinned)
        assertEquals(PackageCoverage.Result(covered = 23, needed = 23), coverage)
        assertFalse(coverage!!.isShort)
    }

    @Test
    fun `the npc reward never counts for blocks`() {
        val pinned = (1..23).map(::day).toSet()
        assertTrue(daily(23, random(22) + reward(day(24)), pinned = pinned)!!.isShort)
    }

    @Test
    fun `a pinned day without a fitting package is a shortage even if the count matches`() {
        // Review example: Dec 1 and 2 pinned, packages fixed to Dec 2 and Dec 3. Dec 3 hands out no block,
        // so its package is useless and Dec 1 has nothing.
        val coverage = daily(
            markerCount = 2,
            packages = fixed(day(2), day(3)),
            endsAt = Instant.parse("2026-12-03T22:59:00Z"),
            pinned = setOf(day(1), day(2)),
        )
        assertEquals(PackageCoverage.Result(covered = 1, needed = 2), coverage)
    }

    @Test
    fun `pinned-only event is checked even without an end date`() {
        val coverage = daily(markerCount = 3, packages = random(2), endsAt = null, pinned = setOf(day(1), day(2), day(3)))
        assertEquals(PackageCoverage.Result(covered = 2, needed = 3), coverage)
    }

    @Test
    fun `pinned days before a late start still need packages, those after the end do not`() {
        val pinned = setOf(day(1), day(2), day(30))
        assertEquals(2, daily(markerCount = 3, packages = random(2), pinned = pinned, startDay = day(2))?.needed)
    }

    @Test
    fun `a random pool next to pinned markers needs every event day`() {
        assertEquals(24, daily(markerCount = 2, packages = random(24), pinned = setOf(day(1)))?.needed)
    }

    @Test
    fun `a fixed package counts once and only for a day that hands out a block`() {
        val coverage = daily(markerCount = 1, packages = fixed(day(1)), endsAt = null, pinned = setOf(day(1)))
        assertEquals(PackageCoverage.Result(covered = 1, needed = 1), coverage)
    }
}
