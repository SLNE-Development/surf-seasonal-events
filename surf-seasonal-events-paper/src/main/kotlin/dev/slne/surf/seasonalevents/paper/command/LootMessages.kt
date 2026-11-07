package dev.slne.surf.seasonalevents.paper.command

import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.seasonalevents.api.result.UpdateLootResult
import net.kyori.adventure.text.Component

// Chat messages of the loot GUIs.

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
}
