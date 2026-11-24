package dev.slne.surf.seasonalevents.paper.tool

import com.github.shynixn.mccoroutine.folia.regionDispatcher
import dev.slne.surf.seasonalevents.api.marker.EventMarker
import dev.slne.surf.seasonalevents.api.marker.MarkerPosition
import dev.slne.surf.seasonalevents.api.result.MarkResult
import dev.slne.surf.seasonalevents.api.result.UnmarkResult
import dev.slne.surf.seasonalevents.core.client.rpc.RpcOutcome
import dev.slne.surf.seasonalevents.core.client.rpc.markerRpc
import dev.slne.surf.seasonalevents.core.client.rpc.rpcCall
import dev.slne.surf.seasonalevents.paper.command.message
import dev.slne.surf.seasonalevents.paper.command.unavailableMessage
import dev.slne.surf.seasonalevents.paper.plugin
import dev.slne.surf.seasonalevents.paper.render.FakeBlockRenderer.toLocation
import dev.slne.surf.seasonalevents.paper.render.ViewService
import kotlinx.coroutines.withContext
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Player

/**
 * Marking and unmarking real blocks. The views are always reloaded before the real block changes:
 * then the server's own block update is already rewritten to the fake for everyone who sees the marker
 * (see `FakeBlockPacketListener`), and nobody sees the block flicker.
 */
object MarkerActions {

    /** Turns [block] into a marker of [eventId]; [snapshot] was captured on the block's region thread. */
    suspend fun mark(player: Player, block: Block, eventId: ULong, snapshot: BlockSnapshot.Snapshot) {
        val position = MarkerPosition(block.world.uid, block.x, block.y, block.z)
        val texture = snapshot.texture()
        val outcome = rpcCall { markerRpc.mark(eventId, position, snapshot.blockData, texture, player.uniqueId) }
        if (outcome !is RpcOutcome.Ok) return player.sendMessage(unavailableMessage())

        val result = outcome.value
        player.sendMessage(result.message())
        if (result !is MarkResult.Success) return

        ViewService.reloadAfterMarkerChange(eventId)
        withContext(plugin.regionDispatcher(block.location)) { block.setType(Material.AIR, false) }
    }

    /** Removes the marker at [position] and puts the real block back. */
    suspend fun unmark(player: Player, position: MarkerPosition) {
        val outcome = rpcCall { markerRpc.unmark(position) }
        if (outcome !is RpcOutcome.Ok) return player.sendMessage(unavailableMessage())

        val result = outcome.value
        player.sendMessage(result.message())
        if (result !is UnmarkResult.Success) return

        ViewService.reloadAfterMarkerChange(result.marker.eventId)
        restoreBlock(result.marker)
    }

    /** Puts the real block of [marker] back into the world. */
    suspend fun restoreBlock(marker: EventMarker) {
        val location = marker.position.toLocation() ?: return
        withContext(plugin.regionDispatcher(location)) {
            BlockSnapshot.restore(location, marker.blockData, marker.skullTexture)
        }
    }
}
