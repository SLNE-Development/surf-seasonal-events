package dev.slne.surf.seasonalevents.paper.time

import dev.slne.surf.seasonalevents.api.SeasonalTime
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeParseException

/** Reads end dates typed in commands. */
object DateArgumentParser {

    /** Staff enter dates in the event's local time. */
    private val zone = SeasonalTime.ZONE

    /** Example shown in error messages. */
    const val EXAMPLE = "2026-12-24T23:59"

    /**
     * Parses `yyyy-MM-ddTHH:mm` in [SeasonalTime.ZONE], so daylight saving time is applied.
     *
     * @return the instant, or null for `none` (no end date)
     * @throws IllegalArgumentException if [input] is neither a valid date nor `none`
     */
    fun parse(input: String): Instant? {
        val trimmed = input.trim()
        if (trimmed.equals("none", ignoreCase = true)) return null

        return try {
            LocalDateTime.parse(trimmed).atZone(zone).toInstant()
        } catch (e: DateTimeParseException) {
            throw IllegalArgumentException("Invalid date '$trimmed', expected e.g. $EXAMPLE", e)
        }
    }
}
