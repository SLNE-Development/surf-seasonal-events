package dev.slne.surf.seasonalevents.paper.command

import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import dev.slne.surf.api.core.messages.pagination.Pagination
import dev.slne.surf.api.core.minimessage.miniMessage
import dev.slne.surf.api.core.util.dateTimeFormatter
import dev.slne.surf.seasonalevents.api.SeasonalTime
import dev.slne.surf.seasonalevents.api.event.EventMode
import dev.slne.surf.seasonalevents.api.event.EventState
import dev.slne.surf.seasonalevents.api.event.SeasonalEvent
import dev.slne.surf.seasonalevents.api.result.CreateEventResult
import dev.slne.surf.seasonalevents.api.result.DeleteEventResult
import dev.slne.surf.seasonalevents.api.result.StartEventResult
import dev.slne.surf.seasonalevents.api.result.StopEventResult
import dev.slne.surf.seasonalevents.api.result.UpdateEventResult
import net.kyori.adventure.text.Component
import java.time.Instant

// Chat messages of /seasonalevents, one per result type.

private val eventDateTime = dateTimeFormatter.withZone(SeasonalTime.ZONE)

/** The instant as event-local date and time, e.g. "24.12.2026 23:59"; "-" for null. */
internal fun Instant?.formatted(): String = this?.let(eventDateTime::format) ?: "-"

/** The display name with the surf MiniMessage tags (`<primary>`, `<success>` …) resolved. */
internal fun SeasonalEvent.displayNameComponent(): Component = miniMessage.deserialize(displayName)

private val EventState.german
    get() = when (this) {
        EventState.DRAFT -> "Entwurf"
        EventState.ACTIVE -> "Aktiv"
        EventState.ENDED -> "Beendet"
    }

internal val EventMode.german
    get() = when (this) {
        EventMode.DAILY -> "Täglich"
        EventMode.COLLECT -> "Sammeln"
    }

/** Appends the state's German name in the color that signals it. */
private fun SurfComponentBuilder.eventState(state: EventState) = when (state) {
    EventState.DRAFT -> warning(state.german)
    EventState.ACTIVE -> success(state.german)
    EventState.ENDED -> error(state.german)
}

fun unavailableMessage() =errorMessage("Das Event-System ist gerade nicht erreichbar.")

fun errorMessage(text: String) = buildText {
    appendErrorPrefix()
    error(text)
}

private fun successMessage(event: String, text: String) = buildText {
    appendSuccessPrefix()
    success("Event ")
    variableValue(event)
    success(" $text")
}

fun eventListMessage(events: List<SeasonalEvent>): Component {
    if (events.isEmpty()) {
        return buildText {
            appendInfoPrefix()
            info("Es gibt noch keine Events.")
        }
    }

    return Pagination<SeasonalEvent> {
        title { primary("Events (${events.size})") }
        rowRendererSimple { event ->
            buildText {
                variableValue(event.name)
                spacer(" - ")
                append(event.displayNameComponent())
                spacer(" - ${event.mode.german}, ")
                eventState(event.state)
            }
        }
    }.renderComponent(events.sortedByDescending { it.createdAt })
}

/** Framed panel titled with the event's display name, one line per entry of the non-empty [rows]. */
internal fun eventPanel(event: SeasonalEvent, rows: List<Component>): Component = Pagination<Component> {
    title = event.displayNameComponent()
    resultsPerPage = rows.size
    rowRendererSimple { it }
}.renderComponent(rows)

/** A `key: value` line of an [eventPanel]. */
internal fun panelRow(key: String, value: SurfComponentBuilder.() -> Unit): Component = buildText {
    variableKey("$key: ")
    value()
}

fun eventInfoMessage(event: SeasonalEvent): Component {
    fun line(key: String, value: Any) = panelRow(key) { variableValue(value.toString()) }
    return eventPanel(
        event,
        listOf(
            line("Name", event.name),
            line("Modus", event.mode.german),
            panelRow("Zustand") { eventState(event.state) },
            line("Marker-Limit", event.markerLimit),
            line("Tageslimit", if (event.dailyLimit > 0) event.dailyLimit else "keins"),
            line("Erstellt", event.createdAt.formatted()),
            line("Gestartet", event.startedAt.formatted()),
            line("Ende", event.endsAt.formatted()),
        ),
    )
}

fun CreateEventResult.message(name: String): Component = when (this) {
    is CreateEventResult.Success -> successMessage(event.name, "wurde angelegt.")
    CreateEventResult.NameTaken -> errorMessage("Ein Event mit dem Namen $name gibt es schon.")
    CreateEventResult.InvalidName ->
        errorMessage("Der Name darf nicht leer und höchstens ${SeasonalEvent.MAX_NAME_LENGTH} Zeichen lang sein.")
    CreateEventResult.InvalidLimit -> errorMessage("Das Marker-Limit muss mindestens 1 sein, das Tageslimit mindestens 0.")
}

/**
 * Explains the package rule after a refused action: [problem], the count `filled/needed`, why that many
 * are needed in [mode] (generic if unknown) and, if given, how to [fix] it.
 */
internal fun packageShortageMessage(
    problem: String,
    filled: Int,
    needed: Int,
    mode: EventMode?,
    fix: (SurfComponentBuilder.() -> Unit)? = null,
) = buildText {
    appendErrorPrefix()
    error("$problem ")
    variableValue("$filled/$needed")
    error(" Pakete befüllt.")
    appendNewErrorPrefixedLine {
        error(
            when (mode) {
                EventMode.COLLECT -> "Jeder gefundene Block gibt ein eigenes Paket, also braucht es eins pro Block."
                EventMode.DAILY -> "Jeder Event-Tag gibt ein eigenes Paket, also braucht es eins pro Tag bis zum Ende."
                null -> "Jeder Spieler bekommt jedes Paket nur einmal, deshalb braucht das Event so viele."
            },
        )
    }
    if (fix != null) appendNewErrorPrefixedLine(fix)
}

fun StartEventResult.message(event: SeasonalEvent): Component = when (this) {
    is StartEventResult.Success -> successMessage(this.event.name, "wurde gestartet. Ende: ${this.event.endsAt.formatted()}")
    is StartEventResult.AnotherEventActive -> errorMessage("Es läuft schon das Event $activeEventName. Stoppe es zuerst.")
    StartEventResult.AlreadyEnded -> errorMessage("Ein beendetes Event kann nicht erneut gestartet werden.")
    StartEventResult.NoMarkers -> errorMessage("Das Event hat noch keine markierten Blöcke.")
    StartEventResult.NoPackages -> errorMessage("Das Event hat noch kein Loot-Paket mit Items.")
    is StartEventResult.NotEnoughPackages -> packageShortageMessage("Zu wenige Loot-Pakete:", filled, needed, event.mode) {
        error("Befülle ${needed - filled} weitere mit ")
        variableValue("/seasonalevents loot ${event.name}")
        error(
            when (event.mode) {
                EventMode.COLLECT -> " oder entmarkiere Blöcke."
                EventMode.DAILY -> " oder wähle ein früheres Ende."
            },
        )
    }
    StartEventResult.NotFound -> errorMessage("Das Event gibt es nicht mehr.")
}

/** Shared by `displayname` and `setend`. */
fun UpdateEventResult.message(): Component = when (this) {
    is UpdateEventResult.Success -> buildText {
        append(successMessage(event.name, "geändert. Anzeigename: "))
        append(event.displayNameComponent())
        success(", Ende: ")
        variableValue(event.endsAt?.formatted() ?: "kein Ende")
    }

    UpdateEventResult.NotFound -> errorMessage("Das Event gibt es nicht mehr.")
    UpdateEventResult.AlreadyEnded -> errorMessage("Ein beendetes Event kann nicht mehr geändert werden.")
    is UpdateEventResult.NotEnoughPackages ->
        packageShortageMessage("Mit diesem Ende hätte das Event zu wenige Pakete:", filled, needed, EventMode.DAILY) {
            error("Befülle zuerst ${needed - filled} weitere oder wähle ein früheres Ende.")
        }
    UpdateEventResult.InvalidDisplayName -> errorMessage(
        "Der Anzeigename darf nicht leer und mit Tags höchstens ${SeasonalEvent.MAX_DISPLAY_NAME_LENGTH} Zeichen lang sein.",
    )
}

fun StopEventResult.message(): Component = when (this) {
    is StopEventResult.Success -> buildText {
        append(successMessage(event.name, "wurde beendet."))
        // Marker positions stay reserved until the real blocks are back.
        appendNewInfoPrefixedLine {
            info("Die Blöcke sind ausgeblendet. Setze sie mit ")
            variableValue("/seasonalevents restore ${event.name}")
            info(" zurück, erst dann ist dort wieder Bauen möglich.")
        }
    }
    StopEventResult.NoActiveEvent -> errorMessage("Es läuft gerade kein Event.")
}

fun DeleteEventResult.message(name: String): Component = when (this) {
    DeleteEventResult.Success -> successMessage(name, "wurde gelöscht.")
    DeleteEventResult.IsActive -> errorMessage("Ein laufendes Event kann nicht gelöscht werden. Stoppe es zuerst.")
    is DeleteEventResult.HasMarkers -> buildText {
        appendErrorPrefix()
        error("Das Event hat noch ")
        variableValue(markerCount.toString())
        error(" markierte Blöcke, die in der Welt fehlen. Setze sie zuerst mit ")
        variableValue("/seasonalevents restore $name")
        error(" zurück, sonst wären sie verloren.")
    }
    DeleteEventResult.NotFound -> errorMessage("Das Event gibt es nicht mehr.")
}
