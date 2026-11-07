package dev.slne.surf.seasonalevents.paper.listener

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListener
import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.protocol.player.InteractionHand
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerAcknowledgeBlockChanges
import com.github.shynixn.mccoroutine.folia.entityDispatcher
import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.seasonalevents.api.marker.EventMarker
import dev.slne.surf.seasonalevents.api.marker.MarkerPosition
import dev.slne.surf.seasonalevents.paper.plugin
import dev.slne.surf.seasonalevents.paper.render.ViewService
import dev.slne.surf.seasonalevents.paper.tool.MarkerActions
import dev.slne.surf.seasonalevents.paper.tool.MarkerTool
import dev.slne.surf.seasonalevents.paper.util.permission.SeasonalEventsPermissions
import org.bukkit.entity.Player

/**
 * Detects right clicks on fake blocks. The server only knows air there, so the packet is swallowed and
 * acknowledged here; the client then falls back to the fake. Runs on Netty threads, so the actual work
 * is moved to the player's thread.
 */
object FakeBlockClickListener : PacketListener {

    override fun onPacketReceive(event: PacketReceiveEvent) {
        if (event.packetType != PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) return
        val player = event.getPlayer<Player>() ?: return
        val packet = WrapperPlayClientPlayerBlockPlacement(event)
        val position = packet.blockPosition
        val marker = ViewService.cache.markerAt(
            player.uniqueId,
            MarkerPosition(player.world.uid, position.x, position.y, position.z),
        ) ?: return

        event.isCancelled = true
        PacketEvents.getAPI().playerManager.sendPacket(player, WrapperPlayServerAcknowledgeBlockChanges(packet.sequence))

        if (packet.hand != InteractionHand.MAIN_HAND) return
        plugin.launch(plugin.entityDispatcher(player)) { onRightClick(player, marker) }
    }

    private suspend fun onRightClick(player: Player, marker: EventMarker) {
        // Collecting by players follows in a later step; for now only the marker tool reacts.
        if (MarkerTool.eventIdOf(player.inventory.itemInMainHand) == null) return
        if (!player.hasPermission(SeasonalEventsPermissions.COMMAND_EDIT)) return
        MarkerActions.unmark(player, marker.position)
    }
}
