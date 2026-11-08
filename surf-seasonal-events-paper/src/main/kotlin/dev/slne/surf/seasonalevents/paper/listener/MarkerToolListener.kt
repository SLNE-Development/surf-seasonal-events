package dev.slne.surf.seasonalevents.paper.listener

import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.seasonalevents.paper.command.errorMessage
import dev.slne.surf.seasonalevents.paper.command.unsupportedBlockMessage
import dev.slne.surf.seasonalevents.paper.plugin
import dev.slne.surf.seasonalevents.paper.tool.BlockSnapshot
import dev.slne.surf.seasonalevents.paper.tool.MarkerActions
import dev.slne.surf.seasonalevents.paper.tool.MarkerTool
import dev.slne.surf.seasonalevents.paper.util.permission.SeasonalEventsPermissions
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.player.PlayerInteractEvent

/**
 * Left click with the marker tool on a real block marks it. Right click on a marker is handled by
 * `FakeBlockClickListener`, because the server only sees air there.
 */
object MarkerToolListener : Listener {

    @EventHandler(priority = EventPriority.LOW)
    fun onLeftClick(event: PlayerInteractEvent) {
        if (event.action != Action.LEFT_CLICK_BLOCK) return
        val eventId = MarkerTool.eventIdOf(event.item) ?: return
        event.isCancelled = true

        val player = event.player
        if (!player.hasPermission(SeasonalEventsPermissions.COMMAND_EDIT)) {
            return player.sendMessage(errorMessage("Du darfst keine Blöcke markieren."))
        }
        val block = event.clickedBlock?.takeUnless { it.type.isAir } ?: return
        if (!BlockSnapshot.isSupported(block)) return player.sendMessage(unsupportedBlockMessage())

        val snapshot = BlockSnapshot.capture(block)
        plugin.launch { MarkerActions.mark(player, block, eventId, snapshot) }
    }

    /** The tool never breaks blocks, also not in creative mode. */
    @EventHandler(priority = EventPriority.LOW)
    fun onBreak(event: BlockBreakEvent) {
        if (MarkerTool.eventIdOf(event.player.inventory.itemInMainHand) != null) event.isCancelled = true
    }
}
