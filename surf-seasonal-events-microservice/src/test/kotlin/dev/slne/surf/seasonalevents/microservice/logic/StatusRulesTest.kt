package dev.slne.surf.seasonalevents.microservice.logic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Verifies the numbers shown by `/seasonalevents` in a `COLLECT` event. */
class StatusRulesTest {

    @Test
    fun `total is the marker count while every found marker still exists`() {
        assertEquals(40, StatusRules.collectTotal(found = 12, foundStillMarked = 12, markerCount = 40))
    }

    @Test
    fun `found markers that were unmarked stay in the total`() {
        // 2 of the 12 found markers were unmarked afterwards, so the event now has 38 markers.
        assertEquals(40, StatusRules.collectTotal(found = 12, foundStillMarked = 10, markerCount = 38))
    }

    @Test
    fun `remaining today counts down to zero and never below`() {
        assertEquals(3, StatusRules.remainingToday(dailyLimit = 3, collectedToday = 0))
        assertEquals(0, StatusRules.remainingToday(dailyLimit = 3, collectedToday = 3))
        assertEquals(0, StatusRules.remainingToday(dailyLimit = 3, collectedToday = 5))
    }

    @Test
    fun `no daily limit means no remaining count`() {
        assertNull(StatusRules.remainingToday(dailyLimit = 0, collectedToday = 7))
    }
}
