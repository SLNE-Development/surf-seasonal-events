package dev.slne.surf.seasonalevents.config.location

import dev.slne.surf.seasonalevents.config.gift.Gift
import dev.slne.surf.surfapi.core.api.util.requiredService
import it.unimi.dsi.fastutil.objects.ObjectList
import org.bukkit.Location
import org.jetbrains.annotations.Unmodifiable
import java.util.*

private val locationService = requiredService<LocationService>()

interface LocationService {
    val locationCount: Int
    val locations: @Unmodifiable ObjectList<Location>

    fun registerLocations()
    fun registerLocation(location: Location)
    fun unregisterLocation(location: Location)

    companion object : LocationService by locationService
}