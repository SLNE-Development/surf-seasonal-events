package dev.slne.surf.seasonalevents.paper.render

import com.destroystokyo.paper.profile.ProfileProperty
import dev.slne.surf.seasonalevents.api.marker.EventMarker
import dev.slne.surf.seasonalevents.api.marker.MarkerPosition
import dev.slne.surf.seasonalevents.paper.plugin
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Skull
import org.bukkit.entity.Player
import java.util.UUID

/**
 * Sends marker blocks to single players. The server only holds air at a marker position, so every
 * player sees exactly the blocks sent to them. Must run on the player's thread.
 */
object FakeBlockRenderer {

    /**
     * Ticks to wait after the real block was changed. The server broadcasts block changes at the end of
     * the tick, which would overwrite a fake sent in the same tick with air.
     */
    private const val SEND_DELAY_AFTER_CHANGE_TICKS = 2L

    private val AIR = Material.AIR.createBlockData()

    fun MarkerPosition.toLocation(): Location? =
        Bukkit.getWorld(world)?.let { Location(it, x.toDouble(), y.toDouble(), z.toDouble()) }

    /** Sends [marker] as a client-side block, including the head texture if there is one. */
    fun show(player: Player, marker: EventMarker) {
        val location = marker.position.toLocation() ?: return
        if (location.world != player.world) return

        val blockData = Bukkit.createBlockData(marker.blockData)
        player.sendBlockChange(location, blockData)

        val texture = marker.skullTexture ?: return
        val skull = blockData.createBlockState() as? Skull ?: return
        skull.setPlayerProfile(Bukkit.createProfile(UUID.randomUUID(), null).apply {
            setProperty(ProfileProperty("textures", texture))
        })
        player.sendBlockUpdate(location, skull)
    }

    /** Like [show], but waits until the change of the real block has been broadcast. */
    fun showAfterBlockChange(player: Player, marker: EventMarker) {
        player.scheduler.runDelayed(plugin, { show(player, marker) }, null, SEND_DELAY_AFTER_CHANGE_TICKS)
    }

    /**
     * Replaces the fake at [position] with the real block. Marker positions are air on the server; if the
     * block lies in another region its state cannot be read here, so air is sent.
     */
    fun hide(player: Player, position: MarkerPosition) {
        val location = position.toLocation() ?: return
        if (location.world != player.world) return

        val real = if (Bukkit.isOwnedByCurrentRegion(location)) location.block.blockData else AIR
        player.sendBlockChange(location, real)
    }
}
