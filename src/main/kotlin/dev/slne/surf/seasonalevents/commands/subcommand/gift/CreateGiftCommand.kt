package dev.slne.surf.seasonalevents.commands.subcommand.gift

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.stringArgument
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.seasonalevents.config.gift.Gift
import dev.slne.surf.seasonalevents.config.gift.GiftService
import dev.slne.surf.seasonalevents.config.gift.gift
import dev.slne.surf.surfapi.core.api.messages.Colors
import dev.slne.surf.surfapi.core.api.messages.adventure.buildText
import dev.slne.surf.surfapi.core.api.messages.adventure.sendText
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.TextDecoration

private fun clickable(gift: Gift) = buildText {
    text("HIER", Colors.VARIABLE_VALUE, TextDecoration.UNDERLINED)
    hoverEvent(HoverEvent.showText(buildText { info("Klicke hier, um das Geschenk zu erstellen.") }))
    clickEvent(ClickEvent.callback {
        GiftService.registerGift(gift)

        it.sendText {
            appendSuccessPrefix()
            success("Das Geschenk mit dem namen")
            appendSpace()
            variableValue(gift.name)
            appendSpace()
            success("wurde erfolgreich erstellt.")

            hoverEvent(HoverEvent.showText(buildText { spacer("ID: ${gift.giftId}") }))
        }
    })
}

fun CommandAPICommand.createGiftCommand() = subcommand("createGift") {

    stringArgument("name")

    playerExecutor { player, arguments ->
        val name = arguments["name"] as String

        val content = player.inventory.contents.filterNotNull().filter { !it.type.isAir }
        val unusedId = GiftService.generateUnusedId()

        val gift = gift(unusedId, name, content)

        if (GiftService.getByName(name) != null) {
            player.sendText {
                appendErrorPrefix()
                error("Es existiert bereits ein Geschenk mit dem Namen")
                appendSpace()
                variableValue(name)
                error(".")
                return@playerExecutor
            }
        }

        player.sendText {
            appendInfoPrefix()
            info("Klicke")
            appendSpace()
            append(clickable(gift))
            appendSpace()
            info("um das Geschenk mit dem Namen")
            appendSpace()
            variableValue(name)
            appendSpace()
            info("und deinem aktuellen Inventar als Inhalt zu erstellen.")
        }
    }
}