package dev.slne.surf.seasonalevents.config.gift

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class GiftConfig(
    val gifts: MutableList<Gift>
)