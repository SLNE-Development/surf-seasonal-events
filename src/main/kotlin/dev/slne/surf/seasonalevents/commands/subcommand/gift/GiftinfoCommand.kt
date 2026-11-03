package dev.slne.surf.seasonalevents.commands.subcommand.gift

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.kotlindsl.getValue
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.seasonalevents.commands.arguments.giftArgument
import dev.slne.surf.seasonalevents.config.gift.Gift
import dev.slne.surf.surfapi.core.api.messages.adventure.sendText

fun CommandAPICommand.giftInfoCommand() = subcommand("info") {

    giftArgument("gift")

    playerExecutor { player, arguments ->
        val gift: Gift by arguments


        player.sendText {
            primary("Geschenk-Info: ")
            secondary(gift.name)
            appendNewline()

            primary("ID: ")
            secondary(gift.giftId.toString())
            appendNewline()

            val items = gift.content

            primary("Inhalt (${items.size} Items):")
            appendNewline()

            if (items.isEmpty()) {
                secondary("  - Keine Items enthalten")
            } else {
                items.forEach { item ->
                    val amount = item.amount
                    val typeName = item.type.name.lowercase().replace("_", " ")

                    primary("  - ")
                    secondary("$amount x ")

                    secondary(typeName)
                    appendNewline()
                }
            }
        }
    }
}