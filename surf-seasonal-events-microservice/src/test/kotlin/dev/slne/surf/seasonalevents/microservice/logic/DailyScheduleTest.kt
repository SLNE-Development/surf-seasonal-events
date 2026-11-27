package dev.slne.surf.seasonalevents.microservice.logic

import dev.slne.surf.seasonalevents.api.marker.EventMarker
import dev.slne.surf.seasonalevents.api.marker.MarkerPosition
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Verifies which pinned markers a player sees and can catch up. */
class DailyScheduleTest {

    private val world = UUID.fromString("c926568e-d50f-4b97-851f-65415c131e3b")

    private fun day(dayOfMonth: Int) = LocalDate.of(2026, 12, dayOfMonth)

    private fun marker(id: Int, day: LocalDate?) =
        EventMarker(id.toULong(), 1u, MarkerPosition(world, id, 64, 0), "minecraft:player_head", null, day)

    /** One pinned marker per day from the 1st to the 23rd, plus one of the random pool. */
    private val markers = (1..23).map { marker(it, day(it)) } + marker(100, null)

    @Test
    fun `a late starter sees every pinned marker up to today`() {
        val visible = DailySchedule.visible(markers, today = day(4), foundDays = emptySet())
        assertEquals(listOf(1, 2, 3, 4), visible.map { it.id.toInt() })
    }

    @Test
    fun `found days disappear and nothing expires`() {
        val visible = DailySchedule.visible(markers, today = day(20), foundDays = setOf(day(2), day(20)))
        assertEquals((1..19).filter { it != 2 }, visible.map { it.id.toInt() })
    }

    @Test
    fun `future days and the random pool are never part of the schedule`() {
        val visible = DailySchedule.visible(markers, today = day(1), foundDays = emptySet())
        assertEquals(listOf(1), visible.map { it.id.toInt() })
    }

    @Test
    fun `open earlier days exclude today and found days`() {
        val pinned = DailySchedule.pinnedDays(markers)
        assertEquals(3, DailySchedule.openEarlier(pinned, today = day(4), foundDays = emptySet()))
        assertEquals(2, DailySchedule.openEarlier(pinned, today = day(4), foundDays = setOf(day(1), day(4))))
        assertEquals(0, DailySchedule.openEarlier(pinned, today = day(1), foundDays = emptySet()))
    }

    @Test
    fun `days without a pinned marker are not pinned`() {
        assertTrue(DailySchedule.isPinned(markers, day(23)))
        assertFalse(DailySchedule.isPinned(markers, day(24)))
    }

    @Test
    fun `pinned days are read back from dedup keys`() {
        val keys = setOf(DedupKeys.pinned(day(1)), DedupKeys.random(day(2)), DedupKeys.marker(5u), DedupKeys.NPC_REWARD)
        assertEquals(setOf(day(1)), DedupKeys.pinnedDays(keys))
    }
}
