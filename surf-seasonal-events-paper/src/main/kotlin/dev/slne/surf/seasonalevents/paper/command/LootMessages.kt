package dev.slne.surf.seasonalevents.paper.command

import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.seasonalevents.api.result.DeleteLootResult
import dev.slne.surf.seasonalevents.api.result.UpdateLootResult
import net.kyori.adventure.text.Component

// Chat messages of the loot GUIs.

private val eventEnded = errorMessage("Das Event ist beendet, seine Pakete können nicht mehr geändert werden.")

fun UpdateLootResult.message(): Component = when (this) {
    is UpdateLootResult.Success -> buildText {
        appendSuccessPrefix()
        success("Paket ")
        variableValue(lootPackage.name)
        success(" gespeichert.")
    }

    UpdateLootResult.NotFound -> errorMessage("Das Paket gibt es nicht mehr.")
    UpdateLootResult.FixedDayTaken -> errorMessage("An diesem Tag gibt es schon ein festes Paket in diesem Event.")
    UpdateLootResult.NotDailyEvent -> errorMessage("Feste Tage gibt es nur in DAILY-Events.")
    UpdateLootResult.EventEnded -> eventEnded
    is UpdateLootResult.NotEnoughPackages -> shortageAfterRemoval(filled, needed, "Leeren")
    UpdateLootResult.Conflict ->
        errorMessage("Jemand anderes hat das Paket gerade gespeichert. Öffne es neu und ändere es dann.")
}

fun DeleteLootResult.message(): Component = when (this) {
    DeleteLootResult.Success -> buildText {
        appendSuccessPrefix()
        success("Paket gelöscht.")
    }

    DeleteLootResult.NotFound -> errorMessage("Das Paket gibt es nicht mehr.")
    DeleteLootResult.EventEnded -> eventEnded
    is DeleteLootResult.NotEnoughPackages -> shortageAfterRemoval(filled, needed, "Löschen")
}

/** The running event would lack packages after emptying or deleting one ([action]). */
private fun shortageAfterRemoval(filled: Int, needed: Int, action: String) =
    packageShortageMessage("Nach dem $action hätte das laufende Event nur", filled, needed, mode = null) {
        error("Lege zuerst ein anderes Paket an und befülle es.")
    }

/** Shown instead of the loot GUI, because nothing in an ended event can change anymore. */
fun lootOfEndedEventMessage(eventName: String): Component =
    errorMessage("Das Event $eventName ist beendet, seine Pakete können nicht mehr bearbeitet werden.")

/** Shown when the editor content is handed back after a failed save. */
fun itemsReturnedMessage(): Component = buildText {
    appendWarningPrefix()
    warning("Nicht gespeichert: Du hast die Items aus dem Paket zurückbekommen.")
}
