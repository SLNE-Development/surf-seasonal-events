package dev.slne.surf.seasonalevents.paper.time

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/** Verifies that command dates are read as Europe/Berlin local time. */
class DateArgumentParserTest {

    @Test
    fun `winter date is converted from CET`() {
        assertEquals(Instant.parse("2026-12-24T22:59:00Z"), DateArgumentParser.parse("2026-12-24T23:59"))
    }

    @Test
    fun `summer date is converted from CEST`() {
        assertEquals(Instant.parse("2026-07-01T10:00:00Z"), DateArgumentParser.parse("2026-07-01T12:00"))
    }

    @Test
    fun `none means no end date`() {
        assertNull(DateArgumentParser.parse("none"))
        assertNull(DateArgumentParser.parse("NONE"))
    }

    @Test
    fun `surrounding spaces are ignored`() {
        assertEquals(Instant.parse("2026-12-24T22:59:00Z"), DateArgumentParser.parse(" 2026-12-24T23:59 "))
    }

    @Test
    fun `nonsense is rejected`() {
        assertFailsWith<IllegalArgumentException> { DateArgumentParser.parse("morgen") }
        assertFailsWith<IllegalArgumentException> { DateArgumentParser.parse("2026-13-01T10:00") }
        assertFailsWith<IllegalArgumentException> { DateArgumentParser.parse("") }
    }
}
