package dev.slne.surf.seasonalevents.listeners

import dev.slne.surf.seasonalevents.services.GiftVisualizationService
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent

object PlayerQuitListener : Listener {

    @EventHandler
    fun onJoin(event: PlayerQuitEvent) {
        val player = event.player
        GiftVisualizationService.removeGiftForPlayer(player)
    }
}