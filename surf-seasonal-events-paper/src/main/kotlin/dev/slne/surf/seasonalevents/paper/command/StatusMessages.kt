package dev.slne.surf.seasonalevents.paper.command

import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.seasonalevents.api.event.SeasonalEvent
import dev.slne.surf.seasonalevents.api.progress.DailyToday
import dev.slne.surf.seasonalevents.api.progress.PlayerStatus
import net.kyori.adventure.text.Component

// What `/seasonalevents` without a subcommand shows a player.

private const val PROGRESS_BAR_SEGMENTS = 20

fun PlayerStatus.message(): Component = when (this) {
    PlayerStatus.NoActiveEvent -> buildText {
        appendInfoPrefix()
        info("Gerade läuft kein Event.")
    }

    is PlayerStatus.Daily -> statusMessage(event, listOf(progressRow(found, total), todayRow(today)))

    is PlayerStatus.Collect -> statusMessage(
        event,
        listOfNotNull(
            progressRow(found, total),
            remainingToday?.let { remaining -> panelRow("Heute noch") { variableValue(remaining) } },
        ),
    )
}

private fun todayRow(today: DailyToday): Component = panelRow("Heute") {
    when (today) {
        DailyToday.HIDDEN -> info("Dein Block ist noch versteckt. Such ihn!")
        DailyToday.COLLECTED -> success("Block gefunden, morgen wartet ein neuer!")
        DailyToday.NOTHING_LEFT -> success("Alle Belohnungen erhalten!")
        DailyToday.NO_BLOCK -> spacer("Für dich ist heute kein Block da.")
    }
}

/** A bar of [PROGRESS_BAR_SEGMENTS] segments followed by the count `found/total`. */
private fun progressRow(found: Int, total: Int): Component = panelRow("Fortschritt") {
    val filled = if (total > 0) found.coerceIn(0, total) * PROGRESS_BAR_SEGMENTS / total else 0
    success("|".repeat(filled))
    darkSpacer("|".repeat(PROGRESS_BAR_SEGMENTS - filled))
    spacer(" ")
    variableValue("$found/$total")
}

/** The event's panel with its mode, the given [rows] and, if set, the end date. */
private fun statusMessage(event: SeasonalEvent, rows: List<Component>): Component = eventPanel(
    event,
    buildList {
        add(panelRow("Modus") { variableValue(event.mode.german) })
        addAll(rows)
        event.endsAt?.let { endsAt -> add(panelRow("Endet") { variableValue(endsAt.formatted()) }) }
    },
)
