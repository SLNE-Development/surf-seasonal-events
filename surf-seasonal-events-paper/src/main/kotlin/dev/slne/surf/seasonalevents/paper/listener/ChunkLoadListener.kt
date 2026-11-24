package dev.slne.surf.seasonalevents.paper.listener

import dev.slne.surf.seasonalevents.paper.render.FakeBlockRenderer
import dev.slne.surf.seasonalevents.paper.render.ViewService
import io.papermc.paper.event.packet.PlayerChunkLoadEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

/**
 * Re-sends the markers of a chunk when the client receives it, because the chunk data overwrites
 * fake blocks with the real state. Covers walking, teleporting, respawning and changing worlds.
 */
object ChunkLoadListener : Listener {

    @EventHandler
    fun onChunkLoad(event: PlayerChunkLoadEvent) {
        val player = event.player
        ViewService.cache
            .markersInChunk(player.uniqueId, event.world.uid, event.chunk.x, event.chunk.z)
            .forEach { FakeBlockRenderer.show(player, it) }
    }
}
