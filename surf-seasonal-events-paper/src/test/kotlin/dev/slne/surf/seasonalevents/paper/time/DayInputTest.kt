package dev.slne.surf.seasonalevents.paper.time

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Verifies how days typed into dialogs are read. */
class DayInputTest {

    @Test
    fun `a valid day is read`() {
        assertEquals(LocalDate.of(2026, 12, 24), DayInput.parse("24.12.2026"))
        assertEquals(LocalDate.of(2026, 12, 1), DayInput.parse(" 01.12.2026 "))
    }

    @Test
    fun `an impossible day is refused instead of being moved`() {
        assertNull(DayInput.parse("31.02.2026"))
        assertNull(DayInput.parse("29.02.2027"))
    }

    @Test
    fun `a leap day is accepted in a leap year`() {
        assertEquals(LocalDate.of(2028, 2, 29), DayInput.parse("29.02.2028"))
    }

    @Test
    fun `other formats are refused`() {
        assertNull(DayInput.parse("2026-12-24"))
        assertNull(DayInput.parse("1.12.2026"))
        assertNull(DayInput.parse(""))
    }
}
