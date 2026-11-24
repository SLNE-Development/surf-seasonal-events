package dev.slne.surf.seasonalevents.api.marker

import kotlinx.serialization.Serializable

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
)
