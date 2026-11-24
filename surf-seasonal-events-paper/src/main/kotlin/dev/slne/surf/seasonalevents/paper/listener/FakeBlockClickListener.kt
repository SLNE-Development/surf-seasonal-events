package dev.slne.surf.seasonalevents.paper.listener

import com.github.retrooper.packetevents.event.PacketListener
import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.protocol.player.DiggingAction
import com.github.retrooper.packetevents.protocol.player.InteractionHand
import com.github.retrooper.packetevents.util.Vector3i
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerAcknowledgeBlockChanges
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange
import com.github.shynixn.mccoroutine.folia.entityDispatcher
import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.api.core.extensions.sendPacket
import dev.slne.surf.seasonalevents.api.marker.EventMarker
import dev.slne.surf.seasonalevents.api.marker.MarkerPosition
import dev.slne.surf.seasonalevents.paper.collect.CollectService
import dev.slne.surf.seasonalevents.paper.plugin
import dev.slne.surf.seasonalevents.paper.render.ViewService
import dev.slne.surf.seasonalevents.paper.tool.MarkerActions
import dev.slne.surf.seasonalevents.paper.tool.MarkerTool
import dev.slne.surf.seasonalevents.paper.util.permission.SeasonalEventsPermissions
import io.github.retrooper.packetevents.util.SpigotConversionUtil
import org.bukkit.Bukkit
import org.bukkit.entity.Player

/**
 * Detects left and right clicks on fake blocks. The server only knows air there, so the packet is
 * swallowed: the client gets the fake block and the acknowledgement from here (in that order, so it
 * keeps the fake). Staff with the marker tool unmark on right click; everyone else collects.
 * Runs on Netty threads, so the actual work is moved to the player's thread.
 */
object FakeBlockClickListener : PacketListener {

    override fun onPacketReceive(event: PacketReceiveEvent) {
        when (event.packetType) {
            PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT -> {
                val packet = WrapperPlayClientPlayerBlockPlacement(event)
                val isMainHand = packet.hand == InteractionHand.MAIN_HAND
                handle(event, packet.blockPosition, packet.sequence, rightClick = true, triggers = isMainHand)
            }

            PacketType.Play.Client.PLAYER_DIGGING -> {
                val packet = WrapperPlayClientPlayerDigging(event)
                val isStart = packet.action == DiggingAction.START_DIGGING
                handle(event, packet.blockPosition, packet.sequence, rightClick = false, triggers = isStart)
            }

            else -> Unit
        }
    }

    /**
     * @param triggers whether this packet starts an action; the off-hand use and the follow-up digging
     * packets are only swallowed
     */
    private fun handle(event: PacketReceiveEvent, position: Vector3i, sequence: Int, rightClick: Boolean, triggers: Boolean) {
        val player = event.getPlayer<Player>() ?: return
        val marker = ViewService.cache.markerAt(
            player.uniqueId,
            MarkerPosition(player.world.uid, position.x, position.y, position.z),
        ) ?: return

        event.isCancelled = true
        val stateId = SpigotConversionUtil.fromBukkitBlockData(Bukkit.createBlockData(marker.blockData)).globalId
        WrapperPlayServerBlockChange(position, stateId).sendPacket(player)
        WrapperPlayServerAcknowledgeBlockChanges(sequence).sendPacket(player)

        if (triggers) plugin.launch(plugin.entityDispatcher(player)) { onClick(player, marker, rightClick) }
    }

    private suspend fun onClick(player: Player, marker: EventMarker, rightClick: Boolean) {
        if (MarkerTool.eventIdOf(player.inventory.itemInMainHand) != null) {
            if (rightClick && player.hasPermission(SeasonalEventsPermissions.COMMAND_EDIT)) {
                MarkerActions.unmark(player, marker.position)
            }
            return
        }
        // In edit mode the player sees every marker of the edited event, not their own blocks.
        if (ViewService.editingEventId(player.uniqueId) != null) return

        CollectService.handleClick(player, marker)
    }
}
