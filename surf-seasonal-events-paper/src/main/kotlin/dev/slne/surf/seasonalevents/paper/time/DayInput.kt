package dev.slne.surf.seasonalevents.paper.time

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle

/** Reads calendar days typed into dialogs as `TT.MM.JJJJ`. */
object DayInput {

    /** Strict, so `31.02.2026` is refused instead of quietly becoming the 28th. */
    private val format = DateTimeFormatter.ofPattern("dd.MM.uuuu").withResolverStyle(ResolverStyle.STRICT)

    /** The day, or null if [input] is not a valid `TT.MM.JJJJ` date. */
    fun parse(input: String): LocalDate? = try {
        LocalDate.parse(input.trim(), format)
    } catch (_: DateTimeParseException) {
        null
    }
}
