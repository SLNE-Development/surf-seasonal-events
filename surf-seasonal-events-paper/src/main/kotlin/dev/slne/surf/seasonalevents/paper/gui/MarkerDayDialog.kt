@file:Suppress("UnstableApiUsage")

package dev.slne.surf.seasonalevents.paper.gui

import com.github.shynixn.mccoroutine.folia.entityDispatcher
import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.api.paper.dialog.base
import dev.slne.surf.api.paper.dialog.dialog
import dev.slne.surf.api.paper.dialog.type
import dev.slne.surf.seasonalevents.api.marker.EventMarker
import dev.slne.surf.seasonalevents.api.result.MarkerDayResult
import dev.slne.surf.seasonalevents.core.client.rpc.RpcOutcome
import dev.slne.surf.seasonalevents.core.client.rpc.markerRpc
import dev.slne.surf.seasonalevents.core.client.rpc.rpcCall
import dev.slne.surf.seasonalevents.paper.command.message
import dev.slne.surf.seasonalevents.paper.command.unavailableMessage
import dev.slne.surf.seasonalevents.paper.plugin
import dev.slne.surf.seasonalevents.paper.render.ViewService
import io.papermc.paper.dialog.Dialog
import kotlinx.coroutines.withContext
import org.bukkit.entity.Player
import dev.slne.surf.seasonalevents.paper.time.DayInput
import java.time.LocalDate

// Dialog of the marker tool to pin a marker to a day. Built per show, because its click callback works once.

private const val DAY_KEY = "day"

/** Asks for the day [marker] is the hiding spot for (`dd.MM.yyyy`, empty for the random pool). */
fun markerDayDialog(marker: EventMarker, error: String? = null): Dialog = dialog {
    base {
        title { primary("Tag des Verstecks") }
        body {
            plainMessage { info("An diesem Tag taucht der Block für alle Spieler auf und bleibt,") }
            plainMessage { info("bis sie ihn gefunden haben. So kann jeder verpasste Tage nachholen.") }
            plainMessage { spacer("Format: TT.MM.JJJJ, leer lassen für den Zufallspool.") }
            if (error != null) plainMessage { error(error) }
        }
        input {
            text(DAY_KEY) {
                label { text("Tag") }
                initial(marker.day?.let(LootItems::formatDay).orEmpty())
                maxLength(10)
            }
        }
    }
    type {
        confirmation {
            yes {
                label { success("Speichern") }
                action {
                    customPlayerClick { response, player ->
                        val input = response.getText(DAY_KEY).orEmpty().trim()
                        val day = if (input.isEmpty()) {
                            null
                        } else {
                            DayInput.parse(input) ?: return@customPlayerClick player.showDialog(
                                markerDayDialog(marker, "„$input“ ist kein gültiges Datum."),
                            )
                        }
                        plugin.launch { setDay(player, marker, day) }
                    }
                }
            }
            no { label { text("Abbrechen") } }
        }
    }
}

private suspend fun setDay(player: Player, marker: EventMarker, day: LocalDate?) {
    val outcome = rpcCall { markerRpc.setDay(marker.id, day) }
    if (outcome !is RpcOutcome.Ok) return player.sendMessage(unavailableMessage())

    val result = outcome.value
    if (result is MarkerDayResult.DayTaken) {
        withContext(plugin.entityDispatcher(player)) {
            player.showDialog(markerDayDialog(marker, "Auf diesen Tag ist schon ein anderer Block festgelegt."))
        }
        return
    }
    player.sendMessage(result.message())
    if (result is MarkerDayResult.Success) ViewService.reloadAfterMarkerChange(marker.eventId)
}
