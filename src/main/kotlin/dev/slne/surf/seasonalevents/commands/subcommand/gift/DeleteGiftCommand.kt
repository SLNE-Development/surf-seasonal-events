package dev.slne.surf.seasonalevents.commands.subcommand.gift

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.kotlindsl.getValue
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.seasonalevents.commands.arguments.giftArgument
import dev.slne.surf.seasonalevents.config.gift.Gift
import dev.slne.surf.seasonalevents.config.gift.GiftService
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
        GiftService.unregisterGift(gift)

        it.sendText {
            appendSuccessPrefix()
            success("Das Geschenk mit dem Namen")
            appendSpace()
            variableValue(gift.name)
            appendSpace()
            success("wurde erfolgreich gelöscht.")
        }
    })
}

fun CommandAPICommand.deleteGiftCommand() = subcommand("deleteGift"){

    giftArgument("gift")

    playerExecutor { player, arguments ->
        val gift: Gift by arguments

        player.sendText {
            appendInfoPrefix()
            info("Klicke")
            appendSpace()
            append(clickable(gift))
            appendSpace()
            info("um das Geschenk mit nem Namen")
            appendSpace()
            variableValue(gift.name)
            appendSpace()
            info("unwiderruflich zu löschen!")
        }
    }
}