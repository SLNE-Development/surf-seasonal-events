package dev.slne.surf.seasonalevents.microservice.logic

import dev.slne.surf.seasonalevents.api.event.EventMode
import java.time.LocalDate
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Verifies which loot package a player receives. */
class PackagePickerTest {

    private val today = LocalDate.of(2026, 12, 3)

    private fun pkg(id: Int, fixedDay: LocalDate? = null) = PackageRef(id.toULong(), fixedDay)

    private val todays = pkg(1, today)
    private val christmasEve = pkg(2, LocalDate.of(2026, 12, 24))
    private val randomA = pkg(3)
    private val randomB = pkg(4)
    private val all = listOf(todays, christmasEve, randomA, randomB)

    @Test
    fun `daily event hands out the package fixed to today`() {
        repeat(20) { seed ->
            val picked = PackagePicker.pick(EventMode.DAILY, all, emptySet(), today, Random(seed))
            assertEquals(todays.id, picked?.id)
        }
    }

    @Test
    fun `daily event falls back to a random package if today's package was received`() {
        repeat(20) { seed ->
            val picked = PackagePicker.pick(EventMode.DAILY, all, setOf(todays.id), today, Random(seed))
            assertTrue(picked?.id in setOf(randomA.id, randomB.id))
        }
    }

    @Test
    fun `daily event never hands out a package fixed to another day`() {
        repeat(50) { seed ->
            val picked = PackagePicker.pick(EventMode.DAILY, all, emptySet(), today.plusDays(1), Random(seed))
            assertNotEquals(christmasEve.id, picked?.id)
            assertNotEquals(todays.id, picked?.id)
        }
    }

    @Test
    fun `daily event gives nothing if only packages for other days are left`() {
        val received = setOf(todays.id, randomA.id, randomB.id)
        assertNull(PackagePicker.pick(EventMode.DAILY, all, received, today, Random(1)))
    }

    @Test
    fun `collect event ignores fixed days`() {
        val picked = (0 until 200).mapNotNull {
            PackagePicker.pick(EventMode.COLLECT, all, emptySet(), today, Random(it))?.id
        }.toSet()
        assertEquals(all.map { it.id }.toSet(), picked)
    }

    @Test
    fun `nothing left gives null`() {
        val received = all.map { it.id }.toSet()
        assertNull(PackagePicker.pick(EventMode.COLLECT, all, received, today, Random(1)))
        assertNull(PackagePicker.pick(EventMode.DAILY, all, received, today, Random(1)))
    }

    @Test
    fun `no packages gives null`() {
        assertNull(PackagePicker.pick(EventMode.DAILY, emptyList(), emptySet(), today, Random(1)))
    }

    @Test
    fun `daily candidates are only today's package while it is open`() {
        assertEquals(listOf(todays), PackagePicker.candidates(EventMode.DAILY, all, emptySet(), today))
        assertEquals(
            listOf(randomA, randomB),
            PackagePicker.candidates(EventMode.DAILY, all, setOf(todays.id), today),
        )
    }

    @Test
    fun `collect candidates are all packages not received`() {
        assertEquals(
            listOf(todays, christmasEve, randomB),
            PackagePicker.candidates(EventMode.COLLECT, all, setOf(randomA.id), today),
        )
    }
}
