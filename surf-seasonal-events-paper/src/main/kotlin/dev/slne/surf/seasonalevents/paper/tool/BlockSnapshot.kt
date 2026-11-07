package dev.slne.surf.seasonalevents.paper.tool

import com.destroystokyo.paper.profile.ProfileProperty
import io.papermc.paper.datacomponent.item.ResolvableProfile
import kotlinx.coroutines.future.await
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.block.Skull
import java.util.UUID

/** Saves and restores a real block, including rotation and the texture of a player head. */
object BlockSnapshot {

    /**
     * A captured block. A head placed by player name only holds the name until Paper looks the skin
     * up, so its [profile] is kept and resolved by [texture] off the region thread.
     */
    class Snapshot(val blockData: String, private val profile: ResolvableProfile?) {

        /** The head's `textures` property, looked up if it is not known yet; null for other blocks. */
        suspend fun texture(): String? {
            val profile = profile ?: return null
            profile.properties().textures()?.let { return it }
            return runCatching { profile.resolve().await().properties.textures() }.getOrNull()
        }
    }

    private fun Collection<ProfileProperty>.textures() = firstOrNull { it.name == "textures" }?.value

    /** Must run on the region thread that owns [block]. */
    fun capture(block: Block) = Snapshot(block.blockData.asString, (block.state as? Skull)?.profile)

    /** Must run on the region thread that owns [location]. */
    fun restore(location: Location, blockData: String, skullTexture: String?) {
        val block = location.block
        block.setBlockData(Bukkit.createBlockData(blockData), false)

        val skull = block.state as? Skull ?: return
        if (skullTexture == null) return
        skull.setPlayerProfile(Bukkit.createProfile(UUID.randomUUID(), null).apply {
            setProperty(ProfileProperty("textures", skullTexture))
        })
        skull.update(true, false)
    }
}
