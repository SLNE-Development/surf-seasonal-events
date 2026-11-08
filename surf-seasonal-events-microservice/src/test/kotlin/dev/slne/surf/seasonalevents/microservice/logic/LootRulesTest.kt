package dev.slne.surf.seasonalevents.microservice.logic

import dev.slne.surf.seasonalevents.api.event.EventMode
import dev.slne.surf.seasonalevents.api.event.EventState
import dev.slne.surf.seasonalevents.api.event.SeasonalEvent
import dev.slne.surf.seasonalevents.api.result.UpdateLootResult
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Verifies when loot packages may be changed. */
class LootRulesTest {

    private val draft = SeasonalEvent(
        id = 1u, name = "christmas-2026", displayName = "Advent",
        mode = EventMode.DAILY, state = EventState.DRAFT,
        markerLimit = 24, dailyLimit = 0,
        endsAt = null, startedAt = null, createdAt = Instant.parse("2026-11-01T00:00:00Z"),
    )
    private val ended = draft.copy(state = EventState.ENDED)

    @Test
    fun `packages of draft and active events may change`() {
        assertNull(LootRules.checkChange(draft))
        assertNull(LootRules.checkChange(draft.copy(state = EventState.ACTIVE)))
    }

    @Test
    fun `packages of an ended event cannot change`() {
        assertEquals(UpdateLootResult.EventEnded, LootRules.checkChange(ended))
    }

    @Test
    fun `save is allowed if nobody saved since the editor opened`() {
        assertNull(LootRules.checkSave(draft, currentItems = byteArrayOf(1, 2), expectedItems = byteArrayOf(1, 2)))
    }

    @Test
    fun `save is refused if someone else saved in between`() {
        assertEquals(
            UpdateLootResult.Conflict,
            LootRules.checkSave(draft, currentItems = byteArrayOf(1, 2, 3), expectedItems = byteArrayOf(1, 2)),
        )
    }

    @Test
    fun `save into an ended event is refused before the conflict check`() {
        assertEquals(
            UpdateLootResult.EventEnded,
            LootRules.checkSave(ended, currentItems = byteArrayOf(9), expectedItems = byteArrayOf(1)),
        )
    }
}
