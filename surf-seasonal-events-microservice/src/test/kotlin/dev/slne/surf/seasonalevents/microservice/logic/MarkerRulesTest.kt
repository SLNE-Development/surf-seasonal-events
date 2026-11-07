package dev.slne.surf.seasonalevents.microservice.logic

import dev.slne.surf.seasonalevents.api.event.EventMode
import dev.slne.surf.seasonalevents.api.event.EventState
import dev.slne.surf.seasonalevents.api.event.SeasonalEvent
import dev.slne.surf.seasonalevents.api.result.MarkResult
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Verifies when a block may be marked as an event object. */
class MarkerRulesTest {

    private val draft = SeasonalEvent(
        id = 1u, name = "christmas-2026", displayName = "Advent",
        mode = EventMode.DAILY, state = EventState.DRAFT,
        markerLimit = 24, dailyLimit = 0,
        endsAt = null, startedAt = null, createdAt = Instant.parse("2026-11-01T00:00:00Z"),
    )

    @Test
    fun `marking is allowed in a draft event below the limit`() {
        assertNull(MarkerRules.checkMark(draft, markerCount = 23, positionTaken = false))
    }

    @Test
    fun `marking is allowed in an active event`() {
        val active = draft.copy(state = EventState.ACTIVE)
        assertNull(MarkerRules.checkMark(active, markerCount = 0, positionTaken = false))
    }

    @Test
    fun `marking is refused in an ended event`() {
        val ended = draft.copy(state = EventState.ENDED)
        assertEquals(MarkResult.EventEnded, MarkerRules.checkMark(ended, markerCount = 0, positionTaken = false))
    }

    @Test
    fun `marking is refused if the position is taken`() {
        assertEquals(MarkResult.AlreadyMarked, MarkerRules.checkMark(draft, markerCount = 0, positionTaken = true))
    }

    @Test
    fun `marking is refused when the limit is reached`() {
        assertEquals(MarkResult.LimitReached(24), MarkerRules.checkMark(draft, markerCount = 24, positionTaken = false))
    }
}
