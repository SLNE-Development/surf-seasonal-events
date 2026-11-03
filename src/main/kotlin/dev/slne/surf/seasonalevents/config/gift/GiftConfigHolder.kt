package dev.slne.surf.seasonalevents.config.gift

import dev.slne.surf.seasonalevents.plugin
import dev.slne.surf.surfapi.core.api.config.createSpongeYmlConfig
import dev.slne.surf.surfapi.core.api.config.manager.SpongeConfigManager
import dev.slne.surf.surfapi.core.api.config.surfConfigApi
import kotlin.jvm.java

object GiftConfigHolder {
    private val manager: SpongeConfigManager<GiftConfig>

    init {
        surfConfigApi.createSpongeYmlConfig<GiftConfig>(plugin.dataPath, "gifts.yml")
        manager = surfConfigApi.getSpongeConfigManagerForConfig(GiftConfig::class.java)
    }

    val config: GiftConfig get() = manager.config

    fun save() {
        manager.save()
    }

    fun reload() {
        manager.reloadFromFile()
    }
}

val giftConfig get() = GiftConfigHolder.config