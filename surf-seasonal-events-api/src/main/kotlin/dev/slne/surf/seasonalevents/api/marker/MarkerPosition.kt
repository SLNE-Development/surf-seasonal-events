package dev.slne.surf.seasonalevents.api.marker

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.util.UUID

/** A block position, independent of Bukkit so the microservice can use it. */
@Serializable
data class MarkerPosition(
    /** UUID of the world. */
    val world: @Contextual UUID,
    val x: Int,
    val y: Int,
    val z: Int,
)
