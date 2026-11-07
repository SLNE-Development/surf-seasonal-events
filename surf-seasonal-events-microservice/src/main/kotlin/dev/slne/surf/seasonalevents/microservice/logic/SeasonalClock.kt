package dev.slne.surf.seasonalevents.microservice.logic

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class SeasonalClock(private val clock: Clock = Clock.systemUTC()) {
    val zone: ZoneId = ZoneId.of("Europe/Berlin")
    fun now(): Instant = clock.instant()
    fun today(): LocalDate = LocalDate.ofInstant(now(), zone)
}