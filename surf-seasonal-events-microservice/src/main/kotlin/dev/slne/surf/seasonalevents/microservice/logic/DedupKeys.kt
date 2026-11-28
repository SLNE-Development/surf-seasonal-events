package dev.slne.surf.seasonalevents.microservice.logic

import dev.slne.surf.seasonalevents.api.event.EventMode
import dev.slne.surf.seasonalevents.api.marker.EventMarker
import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * The `dedup_key` values of collections. Each one can exist once per player and event, so it records
 * what is done: a pinned day, a day of the random pool, a marker or the NPC reward.
 */
object DedupKeys {
    private const val PINNED_PREFIX = "S:"

    /** The marker pinned to [day] was found; the same for every marker that is ever pinned to that day. */
    fun pinned(day: LocalDate) = "$PINNED_PREFIX$day"

    /** A random-pool marker was found on [day] (one per day in a `DAILY` event). */
    fun random(day: LocalDate) = "D:$day"

    /** Marker [markerId] was found (one per marker in a `COLLECT` event). */
    fun marker(markerId: ULong) = "M:$markerId"

    /** The NPC reward was received. */
    const val NPC_REWARD = "R"

    /**
     * The key of collecting [marker] on [today]: per pinned day for a pinned marker and per collection day
     * for a random-pool marker in a `DAILY` event, per marker in a `COLLECT` event. Progress of pinned markers
     * belongs to the calendar day, so a marker moved to another day is that day's hiding spot from then on.
     */
    fun forCollection(mode: EventMode, today: LocalDate, marker: EventMarker): String = when (mode) {
        EventMode.DAILY -> marker.day?.let(::pinned) ?: random(today)
        EventMode.COLLECT -> marker(marker.id)
    }

    /** The pinned days found according to [keys]. */
    fun pinnedDays(keys: Set<String>): Set<LocalDate> = keys.mapNotNullTo(HashSet()) { key ->
        if (!key.startsWith(PINNED_PREFIX)) return@mapNotNullTo null
        try {
            LocalDate.parse(key.removePrefix(PINNED_PREFIX))
        } catch (_: DateTimeParseException) {
            null
        }
    }
}
