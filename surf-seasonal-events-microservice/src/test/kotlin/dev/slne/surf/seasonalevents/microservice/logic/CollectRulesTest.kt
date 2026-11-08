package dev.slne.surf.seasonalevents.microservice.logic

import dev.slne.surf.seasonalevents.api.event.EventMode
import dev.slne.surf.seasonalevents.api.event.EventState
import dev.slne.surf.seasonalevents.api.event.SeasonalEvent
import dev.slne.surf.seasonalevents.api.result.PreviewResult
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Verifies when a player may collect a marker. */
class CollectRulesTest {

    private val daily = SeasonalEvent(
        id = 1u, name = "christmas-2026", displayName = "Advent",
        mode = EventMode.DAILY, state = EventState.ACTIVE,
        markerLimit = 24, dailyLimit = 0,
        endsAt = null, startedAt = null, createdAt = Instant.parse("2026-11-01T00:00:00Z"),
    )
    private val collect = daily.copy(mode = EventMode.COLLECT, dailyLimit = 3)

    @Test
    fun `own uncollected marker may be collected`() {
        assertNull(CollectRules.check(daily, ownsMarker = true, alreadyCollected = false, collectedToday = 0))
    }

    @Test
    fun `no event means no active event`() {
        assertEquals(
            PreviewResult.NoActiveEvent,
            CollectRules.check(null, ownsMarker = true, alreadyCollected = false, collectedToday = 0),
        )
    }

    @Test
    fun `event that is not active means no active event`() {
        val ended = daily.copy(state = EventState.ENDED)
        assertEquals(
            PreviewResult.NoActiveEvent,
            CollectRules.check(ended, ownsMarker = true, alreadyCollected = false, collectedToday = 0),
        )
    }

    @Test
    fun `marker of another player is not yours`() {
        assertEquals(
            PreviewResult.NotYours,
            CollectRules.check(daily, ownsMarker = false, alreadyCollected = false, collectedToday = 0),
        )
    }

    @Test
    fun `collected marker cannot be collected again`() {
        assertEquals(
            PreviewResult.AlreadyCollected,
            CollectRules.check(daily, ownsMarker = true, alreadyCollected = true, collectedToday = 1),
        )
    }

    @Test
    fun `collect event refuses when the daily limit is reached`() {
        assertEquals(
            PreviewResult.DailyLimitReached,
            CollectRules.check(collect, ownsMarker = true, alreadyCollected = false, collectedToday = 3),
        )
    }

    @Test
    fun `collect event allows collecting below the daily limit`() {
        assertNull(CollectRules.check(collect, ownsMarker = true, alreadyCollected = false, collectedToday = 2))
    }

    @Test
    fun `collect event without daily limit is unlimited`() {
        val unlimited = collect.copy(dailyLimit = 0)
        assertNull(CollectRules.check(unlimited, ownsMarker = true, alreadyCollected = false, collectedToday = 100))
    }

    @Test
    fun `daily limit is ignored in a daily event`() {
        val withLimit = daily.copy(dailyLimit = 1)
        assertNull(CollectRules.check(withLimit, ownsMarker = true, alreadyCollected = false, collectedToday = 5))
    }
}
