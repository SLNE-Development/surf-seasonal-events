package dev.slne.surf.seasonalevents.commands

import com.github.shynixn.mccoroutine.folia.launch
import dev.jorel.commandapi.kotlindsl.commandAPICommand
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.slne.surf.seasonalevents.commands.subcommand.gift.createGiftCommand
import dev.slne.surf.seasonalevents.commands.subcommand.gift.deleteGiftCommand
import dev.slne.surf.seasonalevents.commands.subcommand.gift.giftInfoCommand
import dev.slne.surf.seasonalevents.commands.subcommand.location.addLocationCommand
import dev.slne.surf.seasonalevents.commands.subcommand.location.removeLocationCommand
import dev.slne.surf.seasonalevents.commands.subcommand.reloadCommand
import dev.slne.surf.seasonalevents.db.dailygift.DailyGiftService
import dev.slne.surf.seasonalevents.plugin
import dev.slne.surf.surfapi.core.api.messages.adventure.sendText

fun giftCommand() = commandAPICommand("gift") {

    createGiftCommand()
    deleteGiftCommand()

    addLocationCommand()
    removeLocationCommand()

    reloadCommand()

    giftInfoCommand()

    playerExecutor { player, _ ->

        plugin.launch {
            val todayGift = DailyGiftService.getTodayGift(player.uniqueId)

            if (todayGift == null) {
                player.sendText {
                    appendErrorPrefix()
                    error("Du hast heute noch kein Geschenk bekommen!")
                }
                return@launch
            }

            player.sendText {
                appendSuccessPrefix()
                success("Dein Geschenk befindet sich bei:")
                appendSpace()
                variableValue("${todayGift.location.blockX} ${todayGift.location.blockY} ${todayGift.location.blockZ}")

            }
        }
    }
}