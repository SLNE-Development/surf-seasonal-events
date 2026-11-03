package dev.slne.surf.seasonalevents.listeners

import dev.slne.surf.seasonalevents.services.GiftVisualizationService
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

object PlayerJoinListener : Listener {

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        val player = event.player
        GiftVisualizationService.visualizeGiftForPlayer(player)
    }
}