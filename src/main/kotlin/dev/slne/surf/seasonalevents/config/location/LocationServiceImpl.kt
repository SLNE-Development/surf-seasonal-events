package dev.slne.surf.seasonalevents.config.location

import com.google.auto.service.AutoService
import dev.slne.surf.surfapi.core.api.util.freeze
import dev.slne.surf.surfapi.core.api.util.mutableObjectListOf
import net.kyori.adventure.util.Services
import org.bukkit.Location

@AutoService(LocationService::class)
class LocationServiceImpl : LocationService, Services.Fallback {

    private val _locations = mutableObjectListOf<Location>()
    override val locations get() = _locations.freeze()

    override val locationCount get() = locations.size

    override fun registerLocations() {
        _locations.clear()

        val bukkitLocations = locationConfig.locations.map { it.toBukkitLocation() }
        _locations.addAll(bukkitLocations)
    }


    override fun registerLocation(location: Location) {
        if (_locations.contains(location)) return

        _locations.add(location)

        locationConfig.locations.add(SerializableLocation.fromBukkit(location))

        LocationConfigHolder.save()
    }


    override fun unregisterLocation(location: Location) {
        if (!_locations.remove(location)) return

        locationConfig.locations.removeIf { configLoc ->
            val worldId = location.world?.uid?.toString()
            configLoc.worldUuid == worldId &&
                    configLoc.x == location.x &&
                    configLoc.y == location.y &&
                    configLoc.z == location.z
        }

        LocationConfigHolder.save()
    }
}