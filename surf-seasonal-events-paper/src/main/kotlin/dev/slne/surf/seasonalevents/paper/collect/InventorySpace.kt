package dev.slne.surf.seasonalevents.paper.collect

import org.bukkit.inventory.ItemStack
import kotlin.math.min

/** Decides whether a package fits into an inventory, without changing the inventory. */
object InventorySpace {

    /**
     * An item reduced to what matters for stacking.
     *
     * @param key equal keys stack with each other
     */
    data class Stack(val key: Any, val amount: Int, val maxStackSize: Int)

    /**
     * Simulates adding [items] one after another like `Inventory.addItem`: first onto existing stacks of the
     * same kind, then into empty slots. [storage] holds one entry per slot, null for an empty slot.
     */
    fun fits(storage: List<Stack?>, items: List<Stack>): Boolean {
        val slots = storage.toMutableList()

        for (item in items) {
            var remaining = item.amount

            for (index in slots.indices) {
                val slot = slots[index] ?: continue
                if (remaining == 0) break
                if (slot.key != item.key) continue
                val moved = min(slot.maxStackSize - slot.amount, remaining).coerceAtLeast(0)
                slots[index] = slot.copy(amount = slot.amount + moved)
                remaining -= moved
            }

            while (remaining > 0) {
                val free = slots.indexOfFirst { it == null }
                if (free < 0) return false
                val moved = min(item.maxStackSize, remaining)
                slots[free] = item.copy(amount = moved)
                remaining -= moved
            }
        }
        return true
    }

    /** Whether [items] fit into the main inventory and hotbar, i.e. `PlayerInventory.storageContents`. */
    fun fits(storage: Array<ItemStack?>, items: List<ItemStack>): Boolean =
        fits(storage.map { it?.takeUnless(ItemStack::isEmpty)?.toStack() }, items.map { it.toStack() })

    /** `asOne()` makes the key compare type and data but not the amount, like `isSimilar`. */
    private fun ItemStack.toStack() = Stack(asOne(), amount, maxStackSize)
}
