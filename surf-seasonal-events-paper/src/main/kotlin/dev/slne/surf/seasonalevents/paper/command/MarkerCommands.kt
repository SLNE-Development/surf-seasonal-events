package dev.slne.surf.seasonalevents.paper.command

import com.github.shynixn.mccoroutine.folia.entityDispatcher
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.seasonalevents.api.event.SeasonalEvent
import dev.slne.surf.seasonalevents.api.result.RestoreResult
import dev.slne.surf.seasonalevents.core.client.rpc.RpcOutcome
import dev.slne.surf.seasonalevents.core.client.rpc.markerRpc
import dev.slne.surf.seasonalevents.core.client.rpc.rpcCall
import dev.slne.surf.seasonalevents.paper.plugin
import dev.slne.surf.seasonalevents.paper.render.ViewService
import dev.slne.surf.seasonalevents.paper.tool.MarkerActions
import dev.slne.surf.seasonalevents.paper.tool.MarkerTool
import dev.slne.surf.seasonalevents.paper.util.permission.SeasonalEventsPermissions
import kotlinx.coroutines.withContext
import net.kyori.adventure.audience.Audience
import org.bukkit.entity.Player

// Logic of `edit` and `restore`; the command tree is in SeasonalEventsCommand.kt.

/** Enters edit mode for [event] and gives the tool, or leaves it and takes all marker tools away. */
internal suspend fun Player.toggleEditMode(event: SeasonalEvent) {
    val leaving = ViewService.editingEventId(uniqueId) == event.id

    withContext(plugin.entityDispatcher(this@toggleEditMode)) {
        if (leaving) {
            ViewService.leaveEditMode(this@toggleEditMode)
            inventory.contents.forEachIndexed { slot, item ->
                if (MarkerTool.eventIdOf(item) != null) inventory.setItem(slot, null)
            }
        } else {
            ViewService.enterEditMode(this@toggleEditMode, event.id)
            inventory.addItem(MarkerTool.create(event))
        }
    }

    sendText {
        appendSuccessPrefix()
        if (leaving) {
            success("Edit-Modus beendet.")
        } else {
            success("Edit-Modus für ")
            variableValue(event.name)
            success(" aktiv. Du siehst alle Marker und hast das Werkzeug erhalten.")
        }
    }
    ViewService.reload(this)
}

/** Asks for confirmation, then deletes all markers of [event] and puts the real blocks back. */
internal fun Audience.confirmRestore(event: SeasonalEvent) =
    confirm("Alle Blöcke von ${event.name} zurücksetzen und die Marker löschen", SeasonalEventsPermissions.COMMAND_RESTORE) {
        val outcome = rpcCall { markerRpc.restore(event.id) }
        if (outcome !is RpcOutcome.Ok) return@confirm sendMessage(unavailableMessage())

        val result = outcome.value
        if (result is RestoreResult.Success) {
            // Views first, so nobody keeps a fake on top of the restored block.
            ViewService.reloadAll()
            result.markers.forEach { MarkerActions.restoreBlock(it) }
        }
        sendMessage(result.message(event.name))
    }
