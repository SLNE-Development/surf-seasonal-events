package dev.slne.surf.seasonalevents.paper.collect

import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

/** Verifies that clicks are throttled per player. */
class ClickCooldownTest {

    private var now = 0L
    private val cooldown = ClickCooldown(500.milliseconds) { now }
    private val alice = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val bob = UUID.fromString("00000000-0000-0000-0000-000000000002")

    private fun advance(millis: Long) {
        now += millis * 1_000_000
    }

    @Test
    fun `click within the cooldown is refused, after it accepted again`() {
        assertTrue(cooldown.tryAcquire(alice))
        advance(499)
        assertFalse(cooldown.tryAcquire(alice))
        advance(1)
        assertTrue(cooldown.tryAcquire(alice))
    }

    @Test
    fun `players have separate cooldowns`() {
        assertTrue(cooldown.tryAcquire(alice))
        assertTrue(cooldown.tryAcquire(bob))
    }

    @Test
    fun `a forgotten player may click right away`() {
        assertTrue(cooldown.tryAcquire(alice))
        cooldown.forget(alice)
        assertTrue(cooldown.tryAcquire(alice))
    }
}
