package dev.slne.surf.seasonalevents.commands.subcommand.location

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.LocationType
import dev.jorel.commandapi.kotlindsl.locationArgument
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.seasonalevents.config.location.LocationService
import dev.slne.surf.surfapi.core.api.messages.adventure.sendText
import org.bukkit.Location

fun CommandAPICommand.addLocationCommand() = subcommand("addLocation"){

    locationArgument("location", LocationType.BLOCK_POSITION, optional = true)

    playerExecutor { player, arguments ->
        val newLocation = arguments.getUnchecked<Location>("location")

        if(newLocation == null) {
            player.sendText {
                appendErrorPrefix()
                error("Die angegebenen Koordinaten sind ungültig!")
            }
            return@playerExecutor
        }

        LocationService.registerLocation(newLocation)

        player.sendText {
            appendSuccessPrefix()
            success("Die angegebenen Koordinaten")
            appendSpace()
            variableValue("${newLocation.blockX} ${newLocation.blockY} ${newLocation.blockZ}")
            appendSpace()
            success("wurden erfolgreich gespeichert!")
        }
    }
}