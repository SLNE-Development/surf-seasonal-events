package dev.slne.surf.seasonalevents.paper.collect

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Ticker
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaDuration

/**
 * Lets each player trigger an action at most once per [cooldown], so spam clicks do not turn into
 * a flood of microservice calls. Entries expire on their own, so nothing has to be cleaned up when a
 * player leaves. Thread-safe.
 *
 * @param ticker source of monotonic time in nanoseconds; replaceable in tests
 */
class ClickCooldown(
    cooldown: Duration = 500.milliseconds,
    ticker: Ticker = Ticker.systemTicker(),
) {
    private val running: Cache<UUID, Unit> = Caffeine.newBuilder()
        .expireAfterWrite(cooldown.toJavaDuration())
        .ticker(ticker)
        .build()

    /** Returns true and starts the cooldown if [player] may act now, false while the cooldown runs. */
    fun tryAcquire(player: UUID): Boolean {
        var accepted = false
        running.asMap().computeIfAbsent(player) { accepted = true }
        return accepted
    }
}
