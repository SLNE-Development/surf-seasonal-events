package dev.slne.surf.seasonalevents.config.location

import org.bukkit.Bukkit
import org.bukkit.Location
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import java.util.*

@ConfigSerializable
data class LocationConfig(
    val locations: MutableList<SerializableLocation>
)

@ConfigSerializable
data class SerializableLocation(
    val worldUuid: String? = null,
    val x: Double = 0.0,
    val y: Double = 0.0,
    val z: Double = 0.0,
    val yaw: Float = 0f,
    val pitch: Float = 0f
) {
    fun toBukkitLocation(): Location {
        val world = worldUuid?.let { Bukkit.getWorld(UUID.fromString(it)) }
        return Location(world, x, y, z, yaw, pitch)
    }

    companion object {
        fun fromBukkit(loc: Location): SerializableLocation {
            return SerializableLocation(
                worldUuid = loc.world?.uid?.toString(),
                x = loc.x,
                y = loc.y,
                z = loc.z,
                yaw = loc.yaw,
                pitch = loc.pitch
            )
        }
    }
}