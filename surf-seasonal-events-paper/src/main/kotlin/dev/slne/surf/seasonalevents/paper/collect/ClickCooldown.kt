package dev.slne.surf.seasonalevents.paper.collect

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Lets each player trigger an action at most once per [cooldown], so spam clicks do not turn into
 * a flood of microservice calls. Thread-safe.
 *
 * @param nanoTime source of monotonic time; replaceable in tests
 */
class ClickCooldown(
    private val cooldown: Duration = 500.milliseconds,
    private val nanoTime: () -> Long = System::nanoTime,
) {
    private val lastAccepted = ConcurrentHashMap<UUID, Long>()

    /** Returns true and starts the cooldown if [player] may act now, false while the cooldown runs. */
    fun tryAcquire(player: UUID): Boolean {
        val now = nanoTime()
        var accepted = false
        lastAccepted.compute(player) { _, last ->
            if (last == null || now - last >= cooldown.inWholeNanoseconds) {
                accepted = true
                now
            } else {
                last
            }
        }
        return accepted
    }

    /** Forgets [player], e.g. when they leave. */
    fun forget(player: UUID) {
        lastAccepted.remove(player)
    }
}
