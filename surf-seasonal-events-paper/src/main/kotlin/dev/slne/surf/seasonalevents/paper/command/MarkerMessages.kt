package dev.slne.surf.seasonalevents.paper.command

import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.seasonalevents.api.event.EventMode
import dev.slne.surf.seasonalevents.api.marker.MarkerPosition
import dev.slne.surf.seasonalevents.api.result.MarkResult
import dev.slne.surf.seasonalevents.api.result.MarkerDayResult
import dev.slne.surf.seasonalevents.api.result.RestoreResult
import dev.slne.surf.seasonalevents.api.result.UnmarkResult
import dev.slne.surf.seasonalevents.paper.gui.LootItems
import net.kyori.adventure.text.Component

// Chat messages of the marker tool and `restore`, one per result type.

private val MarkerPosition.text get() = "$x, $y, $z"

/** E.g. "Block markiert (3/4) bei 12, 64, -30". */
private fun markerMessage(action: String, count: Int, limit: Int, position: MarkerPosition) = buildText {
    appendSuccessPrefix()
    success("$action ")
    spacer("(")
    variableValue("$count/$limit")
    spacer(")")
    success(" bei ")
    variableValue(position.text)
}

fun MarkResult.message(): Component = when (this) {
    is MarkResult.Success -> markerMessage("Block markiert", markerCount, markerLimit, marker.position)
    is MarkResult.LimitReached -> errorMessage("Das Marker-Limit ist erreicht ($limit/$limit). Entmarkiere zuerst einen Block.")
    is MarkResult.NotEnoughPackages ->
        packageShortageMessage("Das laufende Event hätte dann mehr Blöcke als Pakete:", filled, needed, EventMode.COLLECT) {
            error("Befülle zuerst ein weiteres Paket, dann kannst du markieren.")
        }
    MarkResult.AlreadyMarked -> errorMessage("Dieser Block ist schon markiert, in diesem oder einem anderen Event.")
    MarkResult.EventEnded -> errorMessage("In einem beendeten Event kann nicht mehr markiert werden.")
    MarkResult.NotFound -> errorMessage("Das Event des Werkzeugs gibt es nicht mehr.")
}

/** A block with stored contents or data (chest, sign, banner …) would lose them when it is marked. */
fun unsupportedBlockMessage(): Component =
    errorMessage("Blöcke mit Inhalt oder Daten (z. B. Kisten, Schilder, Banner) können nicht markiert werden, nur Köpfe und normale Blöcke.")

fun UnmarkResult.message(): Component = when (this) {
    is UnmarkResult.Success -> markerMessage("Markierung entfernt", markerCount, markerLimit, marker.position)
    UnmarkResult.NotMarked -> errorMessage("Dieser Block ist nicht markiert.")
}

fun MarkerDayResult.message(): Component = when (this) {
    is MarkerDayResult.Success -> buildText {
        appendSuccessPrefix()
        val day = marker.day
        if (day == null) {
            success("Der Block bei ")
            variableValue(marker.position.text)
            success(" gehört wieder zum Zufallspool.")
        } else {
            success("Der Block bei ")
            variableValue(marker.position.text)
            success(" ist jetzt das Versteck vom ")
            variableValue(LootItems.formatDay(day))
            success(".")
        }
    }

    MarkerDayResult.NotFound -> errorMessage("Diesen Marker gibt es nicht mehr.")
    MarkerDayResult.NotDailyEvent -> errorMessage("Tage können nur in DAILY-Events festgelegt werden.")
    MarkerDayResult.DayTaken -> errorMessage("Auf diesen Tag ist schon ein anderer Block festgelegt.")
    MarkerDayResult.EventEnded -> errorMessage("Das Event ist beendet, seine Marker können nicht mehr geändert werden.")
    is MarkerDayResult.NotEnoughPackages ->
        packageShortageMessage("Das laufende Event hätte dann zu wenige Pakete:", filled, needed, EventMode.DAILY) {
            error("Befülle zuerst ein weiteres Paket.")
        }
}

fun RestoreResult.message(eventName: String): Component = when (this) {
    is RestoreResult.Success -> buildText {
        appendSuccessPrefix()
        variableValue(markers.size.toString())
        success(" Blöcke von ")
        variableValue(eventName)
        success(" wurden zurückgesetzt, die Marker sind gelöscht.")
    }

    RestoreResult.IsActive -> errorMessage("Ein laufendes Event kann nicht zurückgesetzt werden. Stoppe es zuerst.")
    RestoreResult.NotFound -> errorMessage("Das Event gibt es nicht mehr.")
}
