package dev.slne.surf.seasonalevents.paper.collect

import com.github.shynixn.mccoroutine.folia.entityDispatcher
import dev.slne.surf.api.core.messages.adventure.playSound
import dev.slne.surf.seasonalevents.api.marker.EventMarker
import dev.slne.surf.seasonalevents.api.result.CollectResult
import dev.slne.surf.seasonalevents.api.result.PreviewResult
import dev.slne.surf.seasonalevents.core.client.rpc.RpcOutcome
import dev.slne.surf.seasonalevents.core.client.rpc.progressRpc
import dev.slne.surf.seasonalevents.core.client.rpc.rpcCall
import dev.slne.surf.seasonalevents.paper.command.denialMessage
import dev.slne.surf.seasonalevents.paper.command.inventoryFullMessage
import dev.slne.surf.seasonalevents.paper.command.message
import dev.slne.surf.seasonalevents.paper.command.unavailableMessage
import dev.slne.surf.seasonalevents.paper.gui.LootItems
import dev.slne.surf.seasonalevents.paper.plugin
import dev.slne.surf.seasonalevents.paper.render.FakeBlockRenderer.toLocation
import dev.slne.surf.seasonalevents.paper.render.ViewService
import kotlinx.coroutines.withContext
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Collecting a marker: preview the package, check the inventory, book it, then hand out the items.
 * Items are only given after the microservice booked the collection.
 */
object CollectService {

    /** Players with a click in progress; further clicks are ignored until it is done. */
    private val busy = ConcurrentHashMap.newKeySet<UUID>()

    /** Handles a click on [marker]; can be called from any thread. */
    suspend fun handleClick(player: Player, marker: EventMarker) {
        if (!busy.add(player.uniqueId)) return
        try {
            collect(player, marker)
        } finally {
            busy.remove(player.uniqueId)
        }
    }

    private suspend fun collect(player: Player, marker: EventMarker) {
        val preview = when (val outcome = rpcCall { progressRpc.previewCollect(player.uniqueId, marker.id) }) {
            is RpcOutcome.Unavailable -> return player.sendMessage(unavailableMessage())
            is RpcOutcome.Ok -> outcome.value as? PreviewResult.Preview
                ?: return outcome.value.denialMessage()?.let(player::sendMessage) ?: Unit
        }

        // Nothing is booked yet, so a full inventory just stops here.
        val items = LootItems.decode(preview.items)
        val fits = withContext(plugin.entityDispatcher(player)) {
            InventorySpace.fits(player.inventory.storageContents, items)
        }
        if (!fits) return player.sendMessage(inventoryFullMessage())

        val result = when (val outcome = rpcCall { progressRpc.collect(player.uniqueId, marker.id, preview.packageId) }) {
            is RpcOutcome.Unavailable -> return player.sendMessage(unavailableMessage())
            is RpcOutcome.Ok -> outcome.value
        }

        if (result is CollectResult.Success) {
            val given = LootItems.decode(result.lootPackage.items)
            withContext(plugin.entityDispatcher(player)) {
                give(player, given)
                celebrate(player, marker)
            }
            ViewService.reload(player)
        }
        player.sendMessage(result.message(itemCount = items.sumOf { it.amount }))
    }

    /**
     * Gives [items]; whatever no longer fits (the inventory changed since the check) is dropped. Only the
     * player can pick it up, and it never despawns.
     */
    private fun give(player: Player, items: List<ItemStack>) {
        player.inventory.addItem(*items.toTypedArray()).values.forEach { leftover ->
            player.world.dropItem(player.location, leftover) {
                it.owner = player.uniqueId
                it.isUnlimitedLifetime = true
            }
        }
    }

    private fun celebrate(player: Player, marker: EventMarker) {
        player.playSound(true) { type(Sound.ENTITY_PLAYER_LEVELUP) }
        val location = marker.position.toLocation()?.add(0.5, 0.5, 0.5) ?: return
        player.spawnParticle(Particle.HAPPY_VILLAGER, location, 25, 0.4, 0.4, 0.4)
    }
}
