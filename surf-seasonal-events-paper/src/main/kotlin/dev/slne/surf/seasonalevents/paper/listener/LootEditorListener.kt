package dev.slne.surf.seasonalevents.paper.listener

import dev.slne.surf.seasonalevents.paper.gui.LootEditor
import dev.slne.surf.seasonalevents.paper.gui.LootEditor.AfterClose
import dev.slne.surf.seasonalevents.paper.gui.supportsFixedDay
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

/** Locks row 6 of the loot editor, handles its buttons and saves the package when it is closed. */
object LootEditorListener : Listener {

    private val buttonSlots = LootEditor.CONTENT_SLOTS until 54

    @EventHandler
    fun onClick(event: InventoryClickEvent) {
        val holder = event.view.topInventory.holder as? LootEditor.Holder ?: return

        if (event.rawSlot !in buttonSlots) {
            // Clicks elsewhere could still pull a button out (double click collects equal items to the
            // cursor) or push an item onto a button (shift click merges into equal stacks).
            val moved = when (event.action) {
                InventoryAction.COLLECT_TO_CURSOR -> event.cursor
                InventoryAction.MOVE_TO_OTHER_INVENTORY -> event.currentItem
                else -> null
            }
            if (moved != null && event.view.topInventory.matchesButton(moved)) event.isCancelled = true
            return
        }
        event.isCancelled = true

        holder.afterClose = when (event.rawSlot) {
            LootEditor.BACK_SLOT -> AfterClose.OVERVIEW
            LootEditor.FIXED_DAY_SLOT -> if (holder.event.supportsFixedDay) AfterClose.FIXED_DAY_DIALOG else return
            LootEditor.DELETE_SLOT -> AfterClose.DELETE_DIALOG
            else -> return
        }
        event.whoClicked.closeInventory()
    }

    @EventHandler
    fun onDrag(event: InventoryDragEvent) {
        if (event.view.topInventory.holder !is LootEditor.Holder) return
        if (event.rawSlots.any { it in buttonSlots }) event.isCancelled = true
    }

    @EventHandler
    fun onClose(event: InventoryCloseEvent) {
        val holder = event.inventory.holder as? LootEditor.Holder ?: return
        val player = event.player as? Player ?: return
        LootEditor.saveAndContinue(player, holder)
    }

    /** Whether [item] stacks with any button or filler of the editor's last row. */
    private fun Inventory.matchesButton(item: ItemStack): Boolean =
        !item.isEmpty && buttonSlots.any { getItem(it)?.isSimilar(item) == true }
}
