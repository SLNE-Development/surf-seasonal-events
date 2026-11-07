package dev.slne.surf.seasonalevents.microservice.logic

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Verifies the daily marker assignment. */
class AssignmentPickerTest {

    private val markerIds = listOf<ULong>(1u, 2u, 3u, 4u, 5u)

    @Test
    fun `empty list gives no assignment`() {
        assertNull(AssignmentPicker.pick(emptyList(), Random(1)))
    }

    @Test
    fun `picked marker is always from the list`() {
        val random = Random(7)
        repeat(100) { assertTrue(AssignmentPicker.pick(markerIds, random) in markerIds) }
    }

    @Test
    fun `same seed gives the same marker`() {
        assertEquals(AssignmentPicker.pick(markerIds, Random(42)), AssignmentPicker.pick(markerIds, Random(42)))
    }
}
