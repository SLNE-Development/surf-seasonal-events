package dev.slne.surf.seasonalevents.microservice.logic

import dev.slne.surf.seasonalevents.api.event.EventMode
import dev.slne.surf.seasonalevents.api.event.EventState
import dev.slne.surf.seasonalevents.api.event.SeasonalEvent
import dev.slne.surf.seasonalevents.api.result.CreateEventResult
import dev.slne.surf.seasonalevents.api.result.StartEventResult
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Verifies when events may be created and started, and when they are expired. */
class EventRulesTest {

    private val draft = SeasonalEvent(
        id = 1u, name = "christmas-2026", displayName = "Advent",
        mode = EventMode.DAILY, state = EventState.DRAFT,
        markerLimit = 24, dailyLimit = 0,
        endsAt = null, startedAt = null, createdAt = Instant.parse("2026-11-01T00:00:00Z"),
    )

    // ---------- checkStart ----------

    @Test
    fun `draft with markers and packages may start`() {
        assertNull(EventRules.checkStart(draft, active = null, markerCount = 24, packageCount = 24))
    }

    @Test
    fun `start is refused while another event is active`() {
        val other = draft.copy(id = 2u, name = "halloween-2026", state = EventState.ACTIVE)
        val result = EventRules.checkStart(draft, active = other, markerCount = 24, packageCount = 24)
        assertEquals(StartEventResult.AnotherEventActive("halloween-2026"), result)
    }

    @Test
    fun `start is refused if the event already ended`() {
        val ended = draft.copy(state = EventState.ENDED)
        val result = EventRules.checkStart(ended, active = null, markerCount = 24, packageCount = 24)
        assertEquals(StartEventResult.AlreadyEnded, result)
    }

    @Test
    fun `start is refused if there are no markers`() {
        val result = EventRules.checkStart(draft, active = null, markerCount = 0, packageCount = 24)
        assertEquals(StartEventResult.NoMarkers, result)
    }

    @Test
    fun `start is refused if there are no packages`() {
        val result = EventRules.checkStart(draft, active = null, markerCount = 24, packageCount = 0)
        assertEquals(StartEventResult.NoPackages, result)
    }

    // ---------- isExpired ----------

    private val end = Instant.parse("2026-12-24T22:59:00Z")

    @Test
    fun `event without end never expires`() {
        assertFalse(EventRules.isExpired(draft.copy(endsAt = null), end))
    }

    @Test
    fun `event is not expired one second before its end`() {
        assertFalse(EventRules.isExpired(draft.copy(endsAt = end), end.minusSeconds(1)))
    }

    @Test
    fun `event is expired exactly at its end`() {
        assertTrue(EventRules.isExpired(draft.copy(endsAt = end), end))
    }

    // ---------- checkCreate ----------

    @Test
    fun `marker limit of zero is invalid`() {
        assertEquals(CreateEventResult.InvalidLimit, EventRules.checkCreate(markerLimit = 0, dailyLimit = 0))
    }

    @Test
    fun `negative daily limit is invalid`() {
        assertEquals(CreateEventResult.InvalidLimit, EventRules.checkCreate(markerLimit = 24, dailyLimit = -1))
    }

    @Test
    fun `positive marker limit with unlimited daily limit is valid`() {
        assertNull(EventRules.checkCreate(markerLimit = 24, dailyLimit = 0))
    }
}
