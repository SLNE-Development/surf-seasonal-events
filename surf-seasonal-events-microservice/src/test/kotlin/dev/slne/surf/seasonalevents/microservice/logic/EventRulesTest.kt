package dev.slne.surf.seasonalevents.microservice.logic

import dev.slne.surf.seasonalevents.api.event.EventMode
import dev.slne.surf.seasonalevents.api.event.EventState
import dev.slne.surf.seasonalevents.api.event.SeasonalEvent
import dev.slne.surf.seasonalevents.api.result.CreateEventResult
import dev.slne.surf.seasonalevents.api.result.DeleteEventResult
import dev.slne.surf.seasonalevents.api.result.StartEventResult
import dev.slne.surf.seasonalevents.api.result.UpdateEventResult
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Verifies when events may be created, changed, started and deleted, and when they are expired. */
class EventRulesTest {

    private val draft = SeasonalEvent(
        id = 1u, name = "christmas-2026", displayName = "Advent",
        mode = EventMode.DAILY, state = EventState.DRAFT,
        markerLimit = 24, dailyLimit = 0,
        endsAt = null, startedAt = null, createdAt = Instant.parse("2026-11-01T00:00:00Z"),
    )
    private val collectDraft = draft.copy(mode = EventMode.COLLECT, name = "easter-2027")

    // ---------- checkStart ----------

    @Test
    fun `draft with markers and packages may start`() {
        assertNull(EventRules.checkStart(draft, active = null, markerCount = 24, packageCount = 24, requiredPackages = null))
    }

    @Test
    fun `start is refused while another event is active`() {
        val other = draft.copy(id = 2u, name = "halloween-2026", state = EventState.ACTIVE)
        val result = EventRules.checkStart(draft, active = other, markerCount = 24, packageCount = 24, requiredPackages = null)
        assertEquals(StartEventResult.AnotherEventActive("halloween-2026"), result)
    }

    @Test
    fun `start is refused if the event already ended`() {
        val ended = draft.copy(state = EventState.ENDED)
        val result = EventRules.checkStart(ended, active = null, markerCount = 24, packageCount = 24, requiredPackages = null)
        assertEquals(StartEventResult.AlreadyEnded, result)
    }

    @Test
    fun `start is refused if there are no markers`() {
        val result = EventRules.checkStart(draft, active = null, markerCount = 0, packageCount = 24, requiredPackages = null)
        assertEquals(StartEventResult.NoMarkers, result)
    }

    @Test
    fun `start is refused if there are no packages`() {
        val result = EventRules.checkStart(draft, active = null, markerCount = 24, packageCount = 0, requiredPackages = null)
        assertEquals(StartEventResult.NoPackages, result)
    }

    @Test
    fun `collect event needs one package per marker`() {
        val result = EventRules.checkStart(collectDraft, active = null, markerCount = 40, packageCount = 39, requiredPackages = 40)
        assertEquals(StartEventResult.NotEnoughPackages(filled = 39, needed = 40), result)
    }

    @Test
    fun `collect event with as many packages as markers may start`() {
        assertNull(EventRules.checkStart(collectDraft, active = null, markerCount = 40, packageCount = 40, requiredPackages = 40))
    }

    @Test
    fun `daily event without end may start with fewer packages than markers`() {
        assertNull(EventRules.checkStart(draft, active = null, markerCount = 40, packageCount = 1, requiredPackages = null))
    }

    @Test
    fun `daily event with end needs one package per day`() {
        val result = EventRules.checkStart(draft, active = null, markerCount = 5, packageCount = 23, requiredPackages = 24)
        assertEquals(StartEventResult.NotEnoughPackages(filled = 23, needed = 24), result)
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
        assertEquals(CreateEventResult.InvalidLimit, EventRules.checkCreate("x", markerLimit = 0, dailyLimit = 0))
    }

    @Test
    fun `negative daily limit is invalid`() {
        assertEquals(CreateEventResult.InvalidLimit, EventRules.checkCreate("x", markerLimit = 24, dailyLimit = -1))
    }

    @Test
    fun `positive marker limit with unlimited daily limit is valid`() {
        assertNull(EventRules.checkCreate("christmas-2026", markerLimit = 24, dailyLimit = 0))
    }

    @Test
    fun `blank name is invalid`() {
        assertEquals(CreateEventResult.InvalidName, EventRules.checkCreate(" ", markerLimit = 24, dailyLimit = 0))
    }

    @Test
    fun `name up to the maximum length is valid, one more is not`() {
        val longest = "a".repeat(SeasonalEvent.MAX_NAME_LENGTH)
        assertNull(EventRules.checkCreate(longest, markerLimit = 24, dailyLimit = 0))
        assertEquals(
            CreateEventResult.InvalidName,
            EventRules.checkCreate(longest + "a", markerLimit = 24, dailyLimit = 0),
        )
    }

    // ---------- checkDisplayName ----------

    @Test
    fun `display name of a draft may change`() {
        assertNull(EventRules.checkDisplayName(draft, "<red>Advent"))
    }

    @Test
    fun `display name of an ended event cannot change`() {
        assertEquals(
            UpdateEventResult.AlreadyEnded,
            EventRules.checkDisplayName(draft.copy(state = EventState.ENDED), "Advent"),
        )
    }

    @Test
    fun `blank or too long display name is invalid`() {
        assertEquals(UpdateEventResult.InvalidDisplayName, EventRules.checkDisplayName(draft, ""))
        val tooLong = "a".repeat(SeasonalEvent.MAX_DISPLAY_NAME_LENGTH + 1)
        assertEquals(UpdateEventResult.InvalidDisplayName, EventRules.checkDisplayName(draft, tooLong))
    }

    // ---------- checkDelete ----------

    @Test
    fun `ended event without markers may be deleted`() {
        assertNull(EventRules.checkDelete(draft.copy(state = EventState.ENDED), markerCount = 0))
    }

    @Test
    fun `active event cannot be deleted`() {
        assertEquals(
            DeleteEventResult.IsActive,
            EventRules.checkDelete(draft.copy(state = EventState.ACTIVE), markerCount = 0),
        )
    }

    @Test
    fun `event with markers cannot be deleted, because their blocks would be lost`() {
        assertEquals(
            DeleteEventResult.HasMarkers(3),
            EventRules.checkDelete(draft.copy(state = EventState.ENDED), markerCount = 3),
        )
    }
}
