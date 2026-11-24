package dev.slne.surf.seasonalevents.paper.gui

import com.github.shynixn.mccoroutine.folia.entityDispatcher
import com.github.shynixn.mccoroutine.folia.launch
import com.github.shynixn.mccoroutine.folia.regionDispatcher
import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.paper.builder.buildItem
import dev.slne.surf.api.paper.builder.displayName
import dev.slne.surf.seasonalevents.api.event.SeasonalEvent
import dev.slne.surf.seasonalevents.api.loot.LootPackage
import dev.slne.surf.seasonalevents.api.result.UpdateLootResult
import dev.slne.surf.seasonalevents.core.client.rpc.RpcOutcome
import dev.slne.surf.seasonalevents.core.client.rpc.lootRpc
import dev.slne.surf.seasonalevents.core.client.rpc.rpcCall
import dev.slne.surf.seasonalevents.paper.command.errorMessage
import dev.slne.surf.seasonalevents.paper.command.itemsReturnedMessage
import dev.slne.surf.seasonalevents.paper.command.message
import dev.slne.surf.seasonalevents.paper.command.unavailableMessage
import dev.slne.surf.seasonalevents.paper.plugin
import kotlinx.coroutines.withContext
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack

/**
 * Editor of one loot package: rows 1 to 5 are filled freely, row 6 holds the buttons. A plain Bukkit
 * inventory, because the surf inventory framework does not support slots the player fills freely.
 * Saved when closed; clicks are guarded by `LootEditorListener`.
 */
object LootEditor {

    const val CONTENT_SLOTS = 45
    const val BACK_SLOT = 45
    const val FIXED_DAY_SLOT = 49
    const val DELETE_SLOT = 53

    /** What happens after the editor was closed and saved. */
    enum class AfterClose { NOTHING, OVERVIEW, FIXED_DAY_DIALOG, DELETE_DIALOG }

    class Holder(val event: SeasonalEvent, val lootPackage: LootPackage) : InventoryHolder {
        var afterClose = AfterClose.NOTHING
        val view: Inventory = Bukkit.createInventory(this, 54, buildText {
            primary("Paket: ")
            variableValue(lootPackage.name)
        })

        override fun getInventory() = view
    }

    /** Opens the editor of [lootPackage]; must run on the player's thread. */
    fun open(player: Player, event: SeasonalEvent, lootPackage: LootPackage) {
        val holder = Holder(event, lootPackage)
        val inventory = holder.view
        LootItems.decode(lootPackage.items).take(CONTENT_SLOTS).forEachIndexed { slot, item -> inventory.setItem(slot, item) }

        for (slot in CONTENT_SLOTS until inventory.size) inventory.setItem(slot, filler)
        inventory.setItem(BACK_SLOT, backIcon)
        if (event.supportsFixedDay) inventory.setItem(FIXED_DAY_SLOT, fixedDayIcon(lootPackage))
        inventory.setItem(DELETE_SLOT, deleteIcon)

        player.openInventory(inventory)
    }

    /**
     * Saves the contents of a closed editor, then runs its [Holder.afterClose] action. If saving fails,
     * the items go back to the player, so nothing that was put in is lost.
     */
    fun saveAndContinue(player: Player, holder: Holder) {
        val items = (0 until CONTENT_SLOTS).mapNotNull { holder.view.getItem(it)?.takeUnless(ItemStack::isEmpty) }
        val bytes = LootItems.encode(items)
        // Taken now: when the editor closes because the player quits, they are gone afterwards.
        val dropLocation = player.location

        plugin.launch {
            val saved = save(player, holder.lootPackage, bytes)
            if (player.isOnline) {
                withContext(plugin.entityDispatcher(player)) {
                    if (!saved) giveBack(player, items)
                    continueAfterClose(player, holder)
                }
            } else if (!saved) {
                dropAt(dropLocation, items)
            }
        }
    }

    /** Saves [bytes] unless nothing changed; reports the outcome to [player] and returns whether it was saved. */
    private suspend fun save(player: Player, lootPackage: LootPackage, bytes: ByteArray): Boolean {
        if (bytes.contentEquals(lootPackage.items)) return true
        if (bytes.size > LootItems.MAX_BYTES) {
            player.sendMessage(errorMessage("Das Paket ist zu groß zum Speichern."))
            player.sendMessage(itemsReturnedMessage())
            return false
        }

        val outcome = rpcCall { lootRpc.save(lootPackage.id, expectedItems = lootPackage.items, items = bytes) }
        if (outcome !is RpcOutcome.Ok) {
            player.sendMessage(unavailableMessage())
            player.sendMessage(itemsReturnedMessage())
            return false
        }
        player.sendMessage(outcome.value.message())
        val saved = outcome.value is UpdateLootResult.Success
        if (!saved) player.sendMessage(itemsReturnedMessage())
        return saved
    }

    private fun giveBack(player: Player, items: List<ItemStack>) {
        player.inventory.addItem(*items.toTypedArray()).values.forEach { player.world.dropItem(player.location, it) }
    }

    /** For a player who left meanwhile: drops [items] where they closed the editor, so they are not lost. */
    private suspend fun dropAt(location: Location, items: List<ItemStack>) {
        withContext(plugin.regionDispatcher(location)) {
            items.forEach { location.world.dropItem(location, it) { drop -> drop.isUnlimitedLifetime = true } }
        }
    }

    private fun continueAfterClose(player: Player, holder: Holder) {
        when (holder.afterClose) {
            AfterClose.NOTHING -> Unit
            AfterClose.OVERVIEW -> openLootOverview(player, holder.event)
            AfterClose.FIXED_DAY_DIALOG -> player.showDialog(fixedDayDialog(holder.event, holder.lootPackage))
            AfterClose.DELETE_DIALOG -> player.showDialog(deletePackageDialog(holder.event, holder.lootPackage))
        }
    }

    private val filler = buildItem(Material.GRAY_STAINED_GLASS_PANE) { displayName { spacer(" ") } }
}
