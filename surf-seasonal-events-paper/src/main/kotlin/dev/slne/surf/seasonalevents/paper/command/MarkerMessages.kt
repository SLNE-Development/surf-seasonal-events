package dev.slne.surf.seasonalevents.paper.command

import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.seasonalevents.api.marker.MarkerPosition
import dev.slne.surf.seasonalevents.api.result.MarkResult
import dev.slne.surf.seasonalevents.api.result.RestoreResult
import dev.slne.surf.seasonalevents.api.result.UnmarkResult
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
    MarkResult.AlreadyMarked -> errorMessage("Dieser Block ist schon markiert, in diesem oder einem anderen Event.")
    MarkResult.EventEnded -> errorMessage("In einem beendeten Event kann nicht mehr markiert werden.")
    MarkResult.NotFound -> errorMessage("Das Event des Werkzeugs gibt es nicht mehr.")
}

fun UnmarkResult.message(): Component = when (this) {
    is UnmarkResult.Success -> markerMessage("Markierung entfernt", markerCount, markerLimit, marker.position)
    UnmarkResult.NotMarked -> errorMessage("Dieser Block ist nicht markiert.")
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
