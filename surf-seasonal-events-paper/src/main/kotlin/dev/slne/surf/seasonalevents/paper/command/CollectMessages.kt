package dev.slne.surf.seasonalevents.paper.command

import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.seasonalevents.api.result.CollectResult
import dev.slne.surf.seasonalevents.api.result.PreviewResult
import net.kyori.adventure.text.Component

// Chat messages when a player clicks their block.

private val notYours = errorMessage("Dieser Block gehört nicht dir.")
private val alreadyCollected = errorMessage("Diesen Block hast du schon gefunden.")
private val dailyLimitReached = errorMessage("Du hast für heute genug gefunden. Komm morgen wieder!")
private val nothingLeft = errorMessage("Du hast schon alle Belohnungen dieses Events erhalten.")
private val noActiveEvent = errorMessage("Gerade läuft kein Event.")

/** Only for the cases that deny collecting; a preview itself is not shown. */
fun PreviewResult.denialMessage(): Component? = when (this) {
    is PreviewResult.Preview -> null
    PreviewResult.NotYours -> notYours
    PreviewResult.AlreadyCollected -> alreadyCollected
    PreviewResult.DailyLimitReached -> dailyLimitReached
    PreviewResult.NothingLeft -> nothingLeft
    PreviewResult.NoActiveEvent -> noActiveEvent
}

fun CollectResult.message(itemCount: Int): Component = when (this) {
    is CollectResult.Success -> buildText {
        appendSuccessPrefix()
        success("Gefunden! Du hast ")
        variableValue(itemCount.toString())
        success(if (itemCount == 1) " Item erhalten." else " Items erhalten.")
    }

    CollectResult.NotYours -> notYours
    CollectResult.AlreadyCollected -> alreadyCollected
    CollectResult.DailyLimitReached -> dailyLimitReached
    CollectResult.NothingLeft -> nothingLeft
    CollectResult.NoActiveEvent -> noActiveEvent
    CollectResult.PackageUnavailable -> errorMessage("Die Belohnung hat sich gerade geändert. Klicke noch einmal.")
}

fun inventoryFullMessage(): Component = errorMessage("Dein Inventar ist zu voll für diese Belohnung. Mach Platz und klicke noch einmal.")
