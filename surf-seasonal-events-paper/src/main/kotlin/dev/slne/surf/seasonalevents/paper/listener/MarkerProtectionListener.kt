package dev.slne.surf.seasonalevents.paper.listener

import dev.slne.surf.seasonalevents.paper.render.ViewService
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockFormEvent
import org.bukkit.event.block.BlockFromToEvent
import org.bukkit.event.block.BlockMultiPlaceEvent
import org.bukkit.event.block.BlockPistonExtendEvent
import org.bukkit.event.block.BlockPistonRetractEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.block.BlockSpreadEvent
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.event.world.StructureGrowEvent

/**
 * Keeps marker positions empty on the server. Only air is stored there, so without this a player,
 * fluid, piston, falling block or growing plant could put a real block underneath a fake one.
 */
object MarkerProtectionListener : Listener {

    private fun Block.isProtected() = ViewService.isProtected(this)

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onPlace(event: BlockPlaceEvent) {
        if (event.block.isProtected()) event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onMultiPlace(event: BlockMultiPlaceEvent) {
        if (event.replacedBlockStates.any { it.block.isProtected() }) event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onFluid(event: BlockFromToEvent) {
        if (event.toBlock.isProtected()) event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onPistonExtend(event: BlockPistonExtendEvent) {
        if (pistonTouchesMarker(event.blocks, event.direction)) event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onPistonRetract(event: BlockPistonRetractEvent) {
        if (pistonTouchesMarker(event.blocks, event.direction)) event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onForm(event: BlockFormEvent) {
        if (event.block.isProtected()) event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onSpread(event: BlockSpreadEvent) {
        if (event.block.isProtected()) event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onEntityChange(event: EntityChangeBlockEvent) {
        if (event.block.isProtected()) event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onGrow(event: StructureGrowEvent) {
        if (event.blocks.any { it.block.isProtected() }) event.isCancelled = true
    }

    private fun pistonTouchesMarker(blocks: List<Block>, direction: BlockFace) =
        blocks.any { it.isProtected() || it.getRelative(direction).isProtected() }
}
