package dev.slne.surf.seasonalevents.microservice.logic

import dev.slne.surf.seasonalevents.api.progress.NpcReward
import dev.slne.surf.seasonalevents.api.progress.NpcToday
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

/** Verifies what the event's NPC tells a player and when it hands out its reward. */
class NpcRulesTest {

    private fun day(dayOfMonth: Int) = LocalDate.of(2026, 12, dayOfMonth)

    private val pinned = (1..23).map(::day).toSet()
    private val christmasEve = day(24)

    @Test
    fun `today's gift is hidden until found`() {
        assertEquals(NpcToday.GIFT_HIDDEN, NpcRules.today(pinned, emptySet(), day(5)))
        assertEquals(NpcToday.GIFT_FOUND, NpcRules.today(pinned, setOf(day(5)), day(5)))
    }

    @Test
    fun `a day without a pinned marker has no gift`() {
        assertEquals(NpcToday.NO_GIFT, NpcRules.today(pinned, emptySet(), christmasEve))
    }

    @Test
    fun `the reward waits for its day even if everything is found`() {
        assertEquals(
            NpcReward.Waiting(christmasEve, missing = 0),
            NpcRules.reward(christmasEve, claimed = false, pinned, foundDays = pinned, today = day(23)),
        )
    }

    @Test
    fun `the reward waits for every missing gift on its day`() {
        assertEquals(
            NpcReward.Waiting(christmasEve, missing = 2),
            NpcRules.reward(christmasEve, claimed = false, pinned, foundDays = pinned - day(3) - day(17), today = christmasEve),
        )
    }

    @Test
    fun `the reward is ready on its day once all 23 gifts are found`() {
        assertEquals(NpcReward.Ready, NpcRules.reward(christmasEve, claimed = false, pinned, pinned, christmasEve))
        // Catching up after Christmas Eve still works.
        assertEquals(NpcReward.Ready, NpcRules.reward(christmasEve, claimed = false, pinned, pinned, day(26)))
    }

    @Test
    fun `days pinned on or after the reward day do not block it`() {
        val withLate = pinned + christmasEve + day(25)
        assertEquals(NpcReward.Ready, NpcRules.reward(christmasEve, claimed = false, withLate, pinned, christmasEve))
    }

    @Test
    fun `a claimed reward stays claimed`() {
        assertEquals(NpcReward.Claimed, NpcRules.reward(christmasEve, claimed = true, pinned, pinned, christmasEve))
    }

    @Test
    fun `no reward package means no reward`() {
        assertEquals(NpcReward.None, NpcRules.reward(null, claimed = false, pinned, pinned, christmasEve))
    }

    @Test
    fun `unpinned found days stay in the total`() {
        assertEquals(24, NpcRules.total(pinned - day(1), setOf(day(1), day(30))))
    }
}
