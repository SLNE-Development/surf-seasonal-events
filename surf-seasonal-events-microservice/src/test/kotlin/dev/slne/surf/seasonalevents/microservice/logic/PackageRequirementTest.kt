package dev.slne.surf.seasonalevents.microservice.logic

import dev.slne.surf.seasonalevents.api.event.EventMode
import java.time.Instant
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Verifies how many packages an event needs. */
class PackageRequirementTest {

    private val firstOfDecember = LocalDate.of(2026, 12, 1)

    /** 24.12.2026 23:59 in Berlin. */
    private val christmasEveEnd = Instant.parse("2026-12-24T22:59:00Z")

    @Test
    fun `collect event needs one package per marker`() {
        assertEquals(40, PackageRequirement.required(EventMode.COLLECT, markerCount = 40, firstOfDecember, endsAt = null))
    }

    @Test
    fun `daily event needs one package per day, both ends included`() {
        assertEquals(24, PackageRequirement.required(EventMode.DAILY, markerCount = 5, firstOfDecember, christmasEveEnd))
    }

    @Test
    fun `daily end is counted on the Berlin calendar day`() {
        // 23:30 UTC on the 24th is already the 25th in Berlin.
        val end = Instant.parse("2026-12-24T23:30:00Z")
        assertEquals(25, PackageRequirement.required(EventMode.DAILY, markerCount = 5, firstOfDecember, end))
    }

    @Test
    fun `daily event without end has no requirement`() {
        assertNull(PackageRequirement.required(EventMode.DAILY, markerCount = 5, firstOfDecember, endsAt = null))
    }

    @Test
    fun `daily end before the start needs nothing`() {
        val end = Instant.parse("2026-11-20T10:00:00Z")
        assertEquals(0, PackageRequirement.required(EventMode.DAILY, markerCount = 5, firstOfDecember, end))
    }

    @Test
    fun `shortage only when fewer packages than needed`() {
        assertEquals(PackageRequirement.Shortage(filled = 23, needed = 24), PackageRequirement.check(23, 24))
        assertNull(PackageRequirement.check(24, 24))
        assertNull(PackageRequirement.check(0, null))
    }
}
