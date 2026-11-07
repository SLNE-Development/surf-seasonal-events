package dev.slne.surf.seasonalevents.paper.command

import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.core.util.dateTimeFormatter
import dev.slne.surf.seasonalevents.api.event.EventMode
import dev.slne.surf.seasonalevents.api.event.EventState
import dev.slne.surf.seasonalevents.api.event.SeasonalEvent
import dev.slne.surf.seasonalevents.api.result.CreateEventResult
import dev.slne.surf.seasonalevents.api.result.DeleteEventResult
import dev.slne.surf.seasonalevents.api.result.StartEventResult
import dev.slne.surf.seasonalevents.api.result.StopEventResult
import dev.slne.surf.seasonalevents.api.result.UpdateEventResult
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import java.time.Instant
import java.time.ZoneId

// Chat messages of /seasonalevents, one per result type.

private val berlinDateTime = dateTimeFormatter.withZone(ZoneId.of("Europe/Berlin"))

private fun Instant?.formatted() = this?.let(berlinDateTime::format) ?: "-"

private fun SeasonalEvent.displayNameComponent() = MiniMessage.miniMessage().deserialize(displayName)

private val EventState.german
    get() = when (this) {
        EventState.DRAFT -> "Entwurf"
        EventState.ACTIVE -> "Aktiv"
        EventState.ENDED -> "Beendet"
    }

private val EventMode.german
    get() = when (this) {
        EventMode.DAILY -> "Täglich"
        EventMode.COLLECT -> "Sammeln"
    }

fun unavailableMessage() = errorMessage("Das Event-System ist gerade nicht erreichbar.")

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

fun eventListMessage(events: List<SeasonalEvent>): Component = buildText {
    appendInfoPrefix()
    if (events.isEmpty()) {
        info("Es gibt noch keine Events.")
        return@buildText
    }

    info("Events (${events.size}):")
    for (event in events.sortedByDescending { it.createdAt }) {
        appendNewInfoPrefixedLine {
            variableValue(event.name)
            spacer(" - ")
            append(event.displayNameComponent())
            spacer(" - ${event.mode.german}, ${event.state.german}")
        }
    }
}

fun eventInfoMessage(event: SeasonalEvent): Component = buildText {
    appendInfoPrefix()
    info("Event ")
    variableValue(event.name)
    fun line(key: String, value: Any) = appendNewInfoPrefixedLine {
        variableKey("$key: ")
        variableValue(value.toString())
    }
    appendNewInfoPrefixedLine { variableKey("Anzeigename: "); append(event.displayNameComponent()) }
    line("Modus", event.mode.german)
    line("Zustand", event.state.german)
    line("Marker-Limit", event.markerLimit)
    line("Tageslimit", if (event.dailyLimit > 0) event.dailyLimit else "keins")
    line("Erstellt", event.createdAt.formatted())
    line("Gestartet", event.startedAt.formatted())
    line("Ende", event.endsAt.formatted())
}

fun CreateEventResult.message(name: String): Component = when (this) {
    is CreateEventResult.Success -> successMessage(event.name, "wurde angelegt.")
    CreateEventResult.NameTaken -> errorMessage("Ein Event mit dem Namen $name gibt es schon.")
    CreateEventResult.InvalidLimit -> errorMessage("Das Marker-Limit muss mindestens 1 sein, das Tageslimit mindestens 0.")
}

fun StartEventResult.message(): Component = when (this) {
    is StartEventResult.Success -> successMessage(event.name, "wurde gestartet. Ende: ${event.endsAt.formatted()}")
    is StartEventResult.AnotherEventActive -> errorMessage("Es läuft schon das Event $activeEventName. Stoppe es zuerst.")
    StartEventResult.AlreadyEnded -> errorMessage("Ein beendetes Event kann nicht erneut gestartet werden.")
    StartEventResult.NoMarkers -> errorMessage("Das Event hat noch keine markierten Blöcke.")
    StartEventResult.NoPackages -> errorMessage("Das Event hat noch kein Loot-Paket mit Items.")
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
}

fun StopEventResult.message(): Component = when (this) {
    is StopEventResult.Success -> successMessage(event.name, "wurde beendet.")
    StopEventResult.NoActiveEvent -> errorMessage("Es läuft gerade kein Event.")
}

fun DeleteEventResult.message(name: String): Component = when (this) {
    DeleteEventResult.Success -> successMessage(name, "wurde gelöscht.")
    DeleteEventResult.IsActive -> errorMessage("Ein laufendes Event kann nicht gelöscht werden. Stoppe es zuerst.")
    DeleteEventResult.NotFound -> errorMessage("Das Event gibt es nicht mehr.")
}
