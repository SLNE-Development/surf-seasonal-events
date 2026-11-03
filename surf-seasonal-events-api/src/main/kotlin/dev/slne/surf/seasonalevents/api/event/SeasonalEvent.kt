package dev.slne.surf.seasonalevents.api.event

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.Instant

/** A seasonal event such as Easter, Halloween or Christmas. */
@Serializable
data class SeasonalEvent(
    val id: ULong,
    /** Unique technical name used in commands, e.g. `christmas-2026`. */
    val name: String,
    /** Name shown to players, in MiniMessage format. */
    val displayName: String,
    val mode: EventMode,
    val state: EventState,
    /** Maximum number of blocks that can be marked for this event. */
    val markerLimit: Int,
    /** Maximum collections per player and day in [EventMode.COLLECT]; 0 means unlimited. */
    val dailyLimit: Int,
    /** When the event ends automatically; null if it only ends by a manual stop. */
    val endsAt: @Contextual Instant?,
    /** When the event was started; null while it is still a draft. */
    val startedAt: @Contextual Instant?,
    val createdAt: @Contextual Instant,
)
