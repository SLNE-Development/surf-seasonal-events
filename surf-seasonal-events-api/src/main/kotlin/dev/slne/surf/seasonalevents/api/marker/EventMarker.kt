package dev.slne.surf.seasonalevents.api.marker

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.LocalDate

/**
 * A block marked as an event object. The real block is removed from the world and shown to players
 * only as a client-side fake block built from [blockData] and [skullTexture].
 */
@Serializable
data class EventMarker(
    val id: ULong,
    val eventId: ULong,
    val position: MarkerPosition,
    /** The original block, as `BlockData#getAsString()`, e.g. `minecraft:player_head[rotation=4]`. */
    val blockData: String,
    /** Texture property of a player head; null for every other block. */
    val skullTexture: String?,
    /**
     * In a `DAILY` event, the day this marker is the hiding spot for, the same for every player. From that
     * day on it stays visible until the player found it, so missed days can be caught up. Null for markers
     * of the random pool, which gives each player one marker on days without a pinned marker.
     */
    val day: @Contextual LocalDate? = null,
)
