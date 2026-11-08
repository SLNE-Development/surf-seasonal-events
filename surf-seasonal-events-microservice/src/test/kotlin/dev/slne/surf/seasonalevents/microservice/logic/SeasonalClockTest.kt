package dev.slne.surf.seasonalevents.microservice.logic

import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.assertEquals

/** Verifies that "today" is always the calendar day in Europe/Berlin, including DST changes. */
class SeasonalClockTest {

    /** Creates a clock frozen at [utc], so the test does not depend on when it runs. */
    private fun clockAt(utc: String) = SeasonalClock(Clock.fixed(Instant.parse(utc), ZoneOffset.UTC))

    @Test
    fun `one minute before midnight in Berlin is still the same day`() {
        // Winter time: Berlin is UTC+1, so 22:59 UTC is 23:59 in Berlin.
        assertEquals(LocalDate.of(2026, 12, 23), clockAt("2026-12-23T22:59:00Z").today())
    }

    @Test
    fun `one minute after midnight in Berlin is already the next day`() {
        // 23:01 UTC is 00:01 on the 24th in Berlin, although UTC is still on the 23rd.
        assertEquals(LocalDate.of(2026, 12, 24), clockAt("2026-12-23T23:01:00Z").today())
    }

    @Test
    fun `summer time is respected`() {
        // On 2026-03-29 Berlin switches to UTC+2: 00:30 UTC is 02:30 in Berlin.
        assertEquals(LocalDate.of(2026, 3, 29), clockAt("2026-03-29T00:30:00Z").today())
        // In summer, 22:30 UTC is already 00:30 on the next day.
        assertEquals(LocalDate.of(2026, 7, 2), clockAt("2026-07-01T22:30:00Z").today())
    }

    @Test
    fun `now returns the instant of the underlying clock`() {
        val instant = Instant.parse("2026-12-01T10:00:00Z")
        assertEquals(instant, clockAt("2026-12-01T10:00:00Z").now())
    }
}
