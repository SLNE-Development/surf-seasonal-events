package dev.slne.surf.seasonalevents.commands.subcommand

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.seasonalevents.config.gift.GiftConfigHolder
import dev.slne.surf.seasonalevents.config.gift.GiftService
import dev.slne.surf.seasonalevents.config.location.LocationConfigHolder
import dev.slne.surf.seasonalevents.config.location.LocationService
import dev.slne.surf.surfapi.core.api.messages.adventure.sendText

fun CommandAPICommand.reloadCommand() = subcommand("reload"){

    anyExecutor { sender, _ ->
        GiftConfigHolder.reload()
        GiftService.registerGifts()

        LocationConfigHolder.reload()
        LocationService.registerLocations()

        sender.sendText {
            appendSuccessPrefix()
            success("Die existierenden Geschenke & Positionen wurde aktualisiert!")

            appendNewSuccessPrefixedLine()
            success("Es wurden")
            appendSpace()
            variableValue(GiftService.giftCount)
            appendSpace()
            success("Geschenke neu geladen!")

            appendNewSuccessPrefixedLine()
            success("Es wurden")
            appendSpace()
            variableValue(LocationService.locationCount)
            appendSpace()
            success("Positionen neu geladen!")
        }
    }
}