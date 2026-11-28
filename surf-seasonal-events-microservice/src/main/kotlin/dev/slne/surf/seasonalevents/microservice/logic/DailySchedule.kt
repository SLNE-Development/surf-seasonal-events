package dev.slne.surf.seasonalevents.microservice.logic

import dev.slne.surf.seasonalevents.api.marker.EventMarker
import java.time.LocalDate

/**
 * Pinned markers of a `DAILY` event: each is the hiding spot of one day for every player. It appears on
 * its day and stays until the player found it, so nobody misses a day by not playing.
 */
object DailySchedule {

    /**
     * The pinned markers [markers] the player sees today: those of today and earlier days that are not in
     * [foundDays].
     */
    fun visible(markers: List<EventMarker>, today: LocalDate, foundDays: Set<LocalDate>): List<EventMarker> =
        markers.filter { marker ->
            val day = marker.day ?: return@filter false
            !day.isAfter(today) && day !in foundDays
        }

    /** Whether a marker is pinned to [day]; then nobody gets a random-pool marker that day. */
    fun isPinned(markers: List<EventMarker>, day: LocalDate): Boolean = markers.any { it.day == day }

    /**
     * Whether random-pool marker [markerId] is the player's block today: it is their assignment
     * ([assignedId]), still part of the pool and today has no pinned marker, which takes precedence.
     */
    fun ownsRandomMarker(markers: List<EventMarker>, today: LocalDate, markerId: ULong, assignedId: ULong?): Boolean =
        assignedId == markerId && !isPinned(markers, today) && markers.any { it.id == markerId && it.day == null }

    /** Pinned days before [today] that are not in [foundDays]: what the player can still catch up. */
    fun openEarlier(pinnedDays: Set<LocalDate>, today: LocalDate, foundDays: Set<LocalDate>): Int =
        pinnedDays.count { it.isBefore(today) && it !in foundDays }

    /** The days markers are pinned to. */
    fun pinnedDays(markers: List<EventMarker>): Set<LocalDate> = markers.mapNotNullTo(HashSet()) { it.day }
}
