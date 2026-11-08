package dev.slne.surf.seasonalevents.microservice.logic

import dev.slne.surf.seasonalevents.api.SeasonalTime
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

/**
 * The microservice's only source of the current time; replaceable with a fixed [Clock] in tests.
 * Calendar days are counted in [SeasonalTime.ZONE].
 */
class SeasonalClock(private val clock: Clock = Clock.systemUTC()) {
    fun now(): Instant = clock.instant()
    fun today(): LocalDate = LocalDate.ofInstant(now(), SeasonalTime.ZONE)
}
