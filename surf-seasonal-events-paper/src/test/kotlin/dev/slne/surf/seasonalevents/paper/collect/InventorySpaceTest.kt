package dev.slne.surf.seasonalevents.paper.collect

import dev.slne.surf.seasonalevents.paper.collect.InventorySpace.Stack
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Verifies the inventory space check that decides whether a package can be collected. */
class InventorySpaceTest {

    private fun stone(amount: Int) = Stack("stone", amount, maxStackSize = 64)
    private fun sword() = Stack("sword", 1, maxStackSize = 1)
    private fun dirt(amount: Int) = Stack("dirt", amount, maxStackSize = 64)

    private fun empty(slots: Int) = List<Stack?>(slots) { null }

    @Test
    fun `empty inventory fits a package`() {
        assertTrue(InventorySpace.fits(empty(36), listOf(stone(64), sword(), dirt(10))))
    }

    @Test
    fun `inventory full of other items does not fit`() {
        val full = List<Stack?>(36) { dirt(64) }
        assertFalse(InventorySpace.fits(full, listOf(stone(1))))
    }

    @Test
    fun `item is merged into an existing stack`() {
        val storage = listOf<Stack?>(stone(63)) + List(35) { dirt(64) }
        assertTrue(InventorySpace.fits(storage, listOf(stone(1))))
        assertFalse(InventorySpace.fits(storage, listOf(stone(2))))
    }

    @Test
    fun `unstackable items need one slot each`() {
        val storage = List<Stack?>(34) { dirt(64) } + empty(2)
        assertTrue(InventorySpace.fits(storage, listOf(sword(), sword())))
        assertFalse(InventorySpace.fits(storage, listOf(sword(), sword(), sword())))
    }

    @Test
    fun `items of one package share the free space`() {
        // One free slot: 32 + 32 stone fit together, a sword on top does not.
        val storage = List<Stack?>(35) { dirt(64) } + empty(1)
        assertTrue(InventorySpace.fits(storage, listOf(stone(32), stone(32))))
        assertFalse(InventorySpace.fits(storage, listOf(stone(32), sword())))
    }

    @Test
    fun `oversized stacks are split by the max stack size`() {
        val storage = List<Stack?>(34) { dirt(64) } + empty(2)
        assertTrue(InventorySpace.fits(storage, listOf(stone(128))))
        assertFalse(InventorySpace.fits(storage, listOf(stone(129))))
    }

    @Test
    fun `empty package always fits`() {
        assertTrue(InventorySpace.fits(List(36) { dirt(64) }, emptyList()))
    }
}
