package dev.slne.surf.seasonalevents.paper.listener

import com.github.retrooper.packetevents.event.PacketListener
import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMultiBlockChange
import dev.slne.surf.seasonalevents.api.marker.EventMarker
import dev.slne.surf.seasonalevents.api.marker.MarkerPosition
import dev.slne.surf.seasonalevents.paper.plugin
import dev.slne.surf.seasonalevents.paper.render.FakeBlockRenderer
import dev.slne.surf.seasonalevents.paper.render.ViewService
import io.github.retrooper.packetevents.util.SpigotConversionUtil
import org.bukkit.Bukkit
import org.bukkit.entity.Player

/**
 * Keeps fake blocks on the client when the server corrects a block there, e.g. after a cancelled
 * placement or a click: outgoing block updates at a position where the player sees a marker are
 * rewritten to the marker's block. Runs on Netty threads, so it only reads the view cache.
 */
object FakeBlockPacketListener : PacketListener {

    override fun onPacketSend(event: PacketSendEvent) {
        when (event.packetType) {
            PacketType.Play.Server.BLOCK_CHANGE -> rewriteSingle(event)
            PacketType.Play.Server.MULTI_BLOCK_CHANGE -> rewriteMulti(event)
            else -> Unit
        }
    }

    private fun rewriteSingle(event: PacketSendEvent) {
        val player = event.getPlayer<Player>() ?: return
        val packet = WrapperPlayServerBlockChange(event)
        val position = packet.blockPosition
        val marker = markerAt(player, position.x, position.y, position.z) ?: return

        val fakeId = marker.stateId()
        if (packet.blockId == fakeId) return
        packet.setBlockID(fakeId)
        event.markForReEncode(true)
        resendTextureLater(player, marker)
    }

    private fun rewriteMulti(event: PacketSendEvent) {
        val player = event.getPlayer<Player>() ?: return
        val packet = WrapperPlayServerMultiBlockChange(event)
        var changed = false

        for (block in packet.blocks) {
            val marker = markerAt(player, block.x, block.y, block.z) ?: continue
            val fakeId = marker.stateId()
            if (block.blockId == fakeId) continue
            block.setBlockId(fakeId)
            changed = true
            resendTextureLater(player, marker)
        }
        if (changed) event.markForReEncode(true)
    }

    private fun markerAt(player: Player, x: Int, y: Int, z: Int): EventMarker? =
        ViewService.cache.markerAt(player.uniqueId, MarkerPosition(player.world.uid, x, y, z))

    private fun EventMarker.stateId() =
        SpigotConversionUtil.fromBukkitBlockData(Bukkit.createBlockData(blockData)).globalId

    /** A rewritten head loses its texture on the client, so the full marker is sent again. */
    private fun resendTextureLater(player: Player, marker: EventMarker) {
        if (marker.skullTexture == null) return
        player.scheduler.runDelayed(plugin, { FakeBlockRenderer.show(player, marker) }, null, 1L)
    }
}
