@file:Suppress("UnstableApiUsage")

package dev.slne.surf.seasonalevents.paper.gui

import com.github.shynixn.mccoroutine.folia.entityDispatcher
import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.api.paper.dialog.base
import dev.slne.surf.api.paper.dialog.dialog
import dev.slne.surf.api.paper.dialog.type
import dev.slne.surf.seasonalevents.api.event.SeasonalEvent
import dev.slne.surf.seasonalevents.api.loot.LootPackage
import dev.slne.surf.seasonalevents.api.result.UpdateLootResult
import dev.slne.surf.seasonalevents.core.client.rpc.RpcOutcome
import dev.slne.surf.seasonalevents.core.client.rpc.lootRpc
import dev.slne.surf.seasonalevents.core.client.rpc.rpcCall
import dev.slne.surf.seasonalevents.paper.command.message
import dev.slne.surf.seasonalevents.paper.command.unavailableMessage
import dev.slne.surf.seasonalevents.paper.plugin
import io.papermc.paper.dialog.Dialog
import kotlinx.coroutines.withContext
import org.bukkit.entity.Player
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

// Dialogs of the loot editor. Built per show, because their click callbacks work once.

private const val DAY_KEY = "day"

/** Asks for the fixed day of [lootPackage] (`dd.MM.yyyy`, empty for none), then reopens the editor. */
fun fixedDayDialog(event: SeasonalEvent, lootPackage: LootPackage, error: String? = null): Dialog = dialog {
    base {
        title { primary("Fester Tag: "); variableValue(lootPackage.name) }
        body {
            plainMessage { info("An diesem Tag bekommt jeder Spieler dieses Paket, sofern er es noch nicht hat.") }
            plainMessage { spacer("Format: TT.MM.JJJJ, leer lassen für keinen festen Tag.") }
            if (error != null) plainMessage { error(error) }
        }
        input {
            text(DAY_KEY) {
                label { text("Tag") }
                initial(lootPackage.fixedDay?.let(LootItems::formatDay).orEmpty())
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
                        val day = parseDay(input)
                            ?: return@customPlayerClick player.showDialog(
                                fixedDayDialog(event, lootPackage, "„$input“ ist kein gültiges Datum."),
                            )
                        plugin.launch { setFixedDay(player, event, lootPackage, day.value) }
                    }
                }
            }
            no {
                label { text("Abbrechen") }
                action { playerCallback { player -> reopenEditor(player, event, lootPackage.id) } }
            }
        }
    }
}

/** Asks whether [lootPackage] should really be deleted; afterwards the overview opens. */
fun deletePackageDialog(event: SeasonalEvent, lootPackage: LootPackage): Dialog = dialog {
    base {
        title { error("Paket löschen") }
        body {
            plainMessage { text("Willst du "); variableValue(lootPackage.name); text(" wirklich löschen?") }
            plainMessage { spacer("Spieler, die es schon bekommen haben, behalten ihre Items.") }
        }
    }
    type {
        confirmation {
            yes {
                label { error("Löschen") }
                action {
                    playerCallback { player ->
                        plugin.launch {
                            val outcome = rpcCall { lootRpc.delete(lootPackage.id) }
                            player.sendMessage(if (outcome is RpcOutcome.Ok) outcome.value.message() else unavailableMessage())
                            withContext(plugin.entityDispatcher(player)) { openLootOverview(player, event) }
                        }
                    }
                }
            }
            no {
                label { text("Abbrechen") }
                action { playerCallback { player -> reopenEditor(player, event, lootPackage.id) } }
            }
        }
    }
}

/** Wraps the result so "no fixed day" (null) can be told apart from invalid input. */
private class Day(val value: LocalDate?)

private val inputDayFormat = DateTimeFormatter.ofPattern("dd.MM.uuuu")

private fun parseDay(input: String): Day? {
    if (input.isEmpty()) return Day(null)
    return try {
        Day(LocalDate.parse(input, inputDayFormat))
    } catch (_: DateTimeParseException) {
        null
    }
}

private suspend fun setFixedDay(player: Player, event: SeasonalEvent, lootPackage: LootPackage, day: LocalDate?) {
    val outcome = rpcCall { lootRpc.setFixedDay(lootPackage.id, day) }
    if (outcome !is RpcOutcome.Ok) return player.sendMessage(unavailableMessage())

    val result = outcome.value
    if (result is UpdateLootResult.FixedDayTaken) {
        withContext(plugin.entityDispatcher(player)) {
            player.showDialog(fixedDayDialog(event, lootPackage, "An diesem Tag gibt es schon ein festes Paket."))
        }
        return
    }
    player.sendMessage(result.message())
    reopenEditor(player, event, lootPackage.id)
}

/** Loads the package again, so the editor shows the saved state, and opens it. */
private fun reopenEditor(player: Player, event: SeasonalEvent, packageId: ULong) {
    plugin.launch {
        val lootPackage = (rpcCall { lootRpc.findByEvent(event.id) } as? RpcOutcome.Ok)?.value?.firstOrNull { it.id == packageId }
        withContext(plugin.entityDispatcher(player)) {
            if (lootPackage == null) openLootOverview(player, event) else LootEditor.open(player, event, lootPackage)
        }
    }
}
