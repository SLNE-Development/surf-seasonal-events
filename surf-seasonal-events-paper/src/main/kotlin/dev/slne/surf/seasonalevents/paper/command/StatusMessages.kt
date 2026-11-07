package dev.slne.surf.seasonalevents.paper.command

import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import dev.slne.surf.seasonalevents.api.event.SeasonalEvent
import dev.slne.surf.seasonalevents.api.progress.PlayerStatus
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit

// What `/seasonalevents` without a subcommand shows a player.

fun PlayerStatus.message(): Component = when (this) {
    PlayerStatus.NoActiveEvent -> buildText {
        appendInfoPrefix()
        info("Gerade läuft kein Event.")
    }

    is PlayerStatus.Daily -> statusMessage(event) {
        val position = position
        when {
            nothingLeft && !collectedToday -> info("Du hast schon alle Belohnungen dieses Events erhalten.")
            collectedToday -> info("Deinen Block für heute hast du schon gefunden. Morgen wartet ein neuer!")
            position != null -> {
                info("Dein Block für heute ist bei ")
                variableValue("${position.x}, ${position.y}, ${position.z}")
                Bukkit.getWorld(position.world)?.let { spacer(" (${it.name})") }
                info(".")
            }

            else -> info("Für dich ist heute kein Block da.")
        }
    }

    is PlayerStatus.Collect -> statusMessage(event) {
        info("Gefunden: ")
        variableValue("$found/$total")
        if (remainingToday != null) {
            info(", heute noch ")
            variableValue(remainingToday.toString())
        }
        info(".")
    }
}

/** Header with the event's display name, the [body] line and, if set, the end date. */
private fun statusMessage(
    event: SeasonalEvent,
    body: SurfComponentBuilder.() -> Unit,
) = buildText {
    appendInfoPrefix()
    append(event.displayNameComponent())
    appendNewInfoPrefixedLine(body)
    event.endsAt?.let { endsAt ->
        appendNewInfoPrefixedLine {
            info("Endet am ")
            variableValue(endsAt.formatted())
            info(".")
        }
    }
}
