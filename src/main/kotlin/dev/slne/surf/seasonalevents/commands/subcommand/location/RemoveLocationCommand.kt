package dev.slne.surf.seasonalevents.commands.subcommand.location

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.arguments.LocationArgument
import dev.jorel.commandapi.kotlindsl.argument
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.seasonalevents.config.location.LocationService
import dev.slne.surf.surfapi.core.api.messages.adventure.sendText
import org.bukkit.Location

fun CommandAPICommand.removeLocationCommand() = subcommand("removeLocation"){

    argument(LocationArgument("location")) {
        replaceSuggestions(ArgumentSuggestions.strings { info ->
            LocationService.locations.map { loc ->
                "${loc.blockX} ${loc.blockY} ${loc.blockZ}"
            }.toTypedArray()
        })
    }


    playerExecutor { player, arguments ->
        val location = arguments["location"] as Location

        LocationService.unregisterLocation(location)

        player.sendText {
            appendSuccessPrefix()
            success("Die angegebene Koordinate")
            appendSpace()
            variableValue("${location.blockX} ${location.blockY} ${location.blockZ}")
            appendSpace()
            success("wurde erfolgreich entfernt.")
        }
    }
}