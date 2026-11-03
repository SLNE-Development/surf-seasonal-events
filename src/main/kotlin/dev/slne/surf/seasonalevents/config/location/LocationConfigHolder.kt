package dev.slne.surf.seasonalevents.config.location

import dev.slne.surf.seasonalevents.plugin
import dev.slne.surf.surfapi.core.api.config.createSpongeYmlConfig
import dev.slne.surf.surfapi.core.api.config.manager.SpongeConfigManager
import dev.slne.surf.surfapi.core.api.config.surfConfigApi
import org.bukkit.Location
import org.spongepowered.configurate.serialize.TypeSerializerCollection
import org.spongepowered.configurate.yaml.YamlConfigurationLoader

object LocationConfigHolder {
    private val manager: SpongeConfigManager<LocationConfig>

    init {
        surfConfigApi.createSpongeYmlConfig<LocationConfig>(plugin.dataPath, "locations.yml")
        manager = surfConfigApi.getSpongeConfigManagerForConfig(LocationConfig::class.java)

    }

    val config: LocationConfig get() = manager.config

    fun save() {
        manager.save()
    }

    fun reload() {
        manager.reloadFromFile()
    }
}

val locationConfig get() = LocationConfigHolder.config