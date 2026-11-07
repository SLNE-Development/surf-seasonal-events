package dev.slne.surf.seasonalevents.paper.listener

import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.seasonalevents.paper.plugin
import dev.slne.surf.seasonalevents.paper.render.ViewService
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

/** Loads a player's view on join and forgets it on quit. */
object PlayerConnectionListener : Listener {

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        val player = event.player
        plugin.launch { ViewService.reload(player) }
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        ViewService.forget(event.player.uniqueId)
    }
}
