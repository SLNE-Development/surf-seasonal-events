package dev.slne.surf.seasonalevents.paper.command

import com.github.shynixn.mccoroutine.folia.launch
import dev.jorel.commandapi.CommandAPI
import dev.jorel.commandapi.kotlindsl.argument
import dev.jorel.commandapi.kotlindsl.commandTree
import dev.jorel.commandapi.kotlindsl.greedyStringArgument
import dev.jorel.commandapi.kotlindsl.integerArgument
import dev.jorel.commandapi.kotlindsl.literalArgument
import dev.jorel.commandapi.kotlindsl.multiLiteralArgument
import dev.jorel.commandapi.kotlindsl.stringArgument
import dev.jorel.commandapi.executors.CommandArguments
import dev.slne.surf.api.core.command.args.awaiting
import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.core.messages.adventure.lifetime
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.paper.command.executors.anyExecutorSuspend
import dev.slne.surf.seasonalevents.api.event.EventMode
import dev.slne.surf.seasonalevents.api.event.SeasonalEvent
import dev.slne.surf.seasonalevents.core.client.rpc.RpcOutcome
import dev.slne.surf.seasonalevents.core.client.rpc.eventRpc
import dev.slne.surf.seasonalevents.core.client.rpc.rpcCall
import dev.slne.surf.seasonalevents.paper.command.argument.SeasonalEventArgument
import dev.slne.surf.seasonalevents.paper.plugin
import dev.slne.surf.seasonalevents.paper.render.ViewService
import dev.slne.surf.seasonalevents.paper.time.DateArgumentParser
import dev.slne.surf.seasonalevents.paper.util.permission.SeasonalEventsPermissions
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import java.time.Instant
import kotlin.time.Duration.Companion.minutes

/** `/seasonalevents`: staff commands to manage seasonal events. Messages live in `EventMessages.kt`. */
fun seasonalEventsCommand() = commandTree("seasonalevents") {
    withPermission(SeasonalEventsPermissions.COMMAND_SEASONAL_EVENTS)

    literalArgument("list") {
        withPermission(SeasonalEventsPermissions.COMMAND_LIST)
        anyExecutorSuspend { sender, _ -> sender.reply({ eventRpc.findAll() }, ::eventListMessage) }
    }

    literalArgument("info") {
        withPermission(SeasonalEventsPermissions.COMMAND_INFO)
        argument(SeasonalEventArgument("event")) {
            anyExecutorSuspend { sender, args -> sender.sendMessage(eventInfoMessage(args.awaiting("event"))) }
        }
    }

    // create <name> <daily|collect> <markerLimit> [dailyLimit]
    literalArgument("create") {
        withPermission(SeasonalEventsPermissions.COMMAND_CREATE)
        stringArgument("name") {
            multiLiteralArgument("mode", "daily", "collect") {
                integerArgument("markerLimit", min = 1) {
                    anyExecutorSuspend { sender, args -> sender.create(args, dailyLimit = 0) }
                    integerArgument("dailyLimit", min = 0) {
                        anyExecutorSuspend { sender, args -> sender.create(args, args["dailyLimit"] as Int) }
                    }
                }
            }
        }
    }

    literalArgument("displayname") {
        withPermission(SeasonalEventsPermissions.COMMAND_EDIT)
        argument(SeasonalEventArgument("event")) {
            greedyStringArgument("displayName") {
                anyExecutorSuspend { sender, args ->
                    val event: SeasonalEvent = args.awaiting("event")
                    sender.reply({ eventRpc.setDisplayName(event.id, args["displayName"] as String) }) { it.message() }
                }
            }
        }
    }

    // start <event> [end]
    literalArgument("start") {
        withPermission(SeasonalEventsPermissions.COMMAND_START)
        argument(SeasonalEventArgument("event")) {
            anyExecutorSuspend { sender, args -> sender.start(args.awaiting("event"), endsAt = null) }
            greedyStringArgument("end") {
                anyExecutorSuspend { sender, args -> sender.start(args.awaiting("event"), parseEnd(args)) }
            }
        }
    }

    // setend <event> <date|none>
    literalArgument("setend") {
        withPermission(SeasonalEventsPermissions.COMMAND_EDIT)
        argument(SeasonalEventArgument("event")) {
            greedyStringArgument("end") {
                anyExecutorSuspend { sender, args ->
                    val event: SeasonalEvent = args.awaiting("event")
                    val endsAt = parseEnd(args)
                    sender.reply({ eventRpc.setEnd(event.id, endsAt) }) { it.message() }
                }
            }
        }
    }

    literalArgument("stop") {
        withPermission(SeasonalEventsPermissions.COMMAND_STOP)
        anyExecutorSuspend { sender, _ -> sender.confirmStop() }
    }

    literalArgument("delete") {
        withPermission(SeasonalEventsPermissions.COMMAND_DELETE)
        argument(SeasonalEventArgument("event")) {
            anyExecutorSuspend { sender, args ->
                val event: SeasonalEvent = args.awaiting("event")
                sender.confirm("Event ${event.name} mit allen Markern, Paketen und Fortschritten löschen", SeasonalEventsPermissions.COMMAND_DELETE) {
                    reply({ eventRpc.delete(event.id) }) { it.message(event.name) }
                    ViewService.reloadAll()
                }
            }
        }
    }
}

/** Runs an RPC call and sends its message, or the "unavailable" message if the microservice is down. */
private suspend fun <T> Audience.reply(call: suspend () -> T, message: (T) -> Component) {
    when (val outcome = rpcCall(call)) {
        is RpcOutcome.Ok -> sendMessage(message(outcome.value))
        is RpcOutcome.Unavailable -> sendMessage(unavailableMessage())
    }
}

/** Reads the `end` argument; an invalid date becomes a normal command error. */
private fun parseEnd(args: CommandArguments): Instant? = try {
    DateArgumentParser.parse(args["end"] as String)
} catch (_: IllegalArgumentException) {
    throw CommandAPI.failWithString("Ungültiges Datum. Beispiel: ${DateArgumentParser.EXAMPLE} oder none")
}

/**
 * Asks for confirmation with a clickable `[HIER]`. The click works once, within one minute, and only
 * for someone holding [permission]; [action] then runs on the plugin scope.
 */
private fun Audience.confirm(question: String, permission: String, action: suspend Audience.() -> Unit) = sendText {
    appendWarningPrefix()
    warning("$question? Das kann nicht rückgängig gemacht werden. ")
    append {
        error("[HIER]", TextDecoration.BOLD)
        hoverEvent(buildText { warning("Klicken zum Bestätigen") })
        clickCallbackWithOptions {
            requiresPermission(permission)
            callback { clicker -> plugin.launch { clicker.action() } }
            options { uses(1).lifetime(1.minutes) }
        }
    }
}

private suspend fun Audience.create(args: CommandArguments, dailyLimit: Int) {
    val name = args["name"] as String
    val mode = EventMode.valueOf((args["mode"] as String).uppercase())
    val markerLimit = args["markerLimit"] as Int
    reply({ eventRpc.create(name, mode, markerLimit, dailyLimit) }) { it.message(name) }
}

private suspend fun Audience.start(event: SeasonalEvent, endsAt: Instant?) {
    reply({ eventRpc.start(event.id, endsAt) }) { it.message() }
    ViewService.reloadAll()
}

private suspend fun Audience.confirmStop() {
    val active = when (val outcome = rpcCall { eventRpc.getActiveEvent() }) {
        is RpcOutcome.Unavailable -> return sendMessage(unavailableMessage())
        is RpcOutcome.Ok -> outcome.value ?: return sendMessage(errorMessage("Es läuft gerade kein Event."))
    }

    confirm("Event ${active.name} wirklich beenden", SeasonalEventsPermissions.COMMAND_STOP) {
        // Do not stop a different event that was started after the confirmation was sent.
        val current = rpcCall { eventRpc.getActiveEvent() }
        if (current is RpcOutcome.Ok && current.value?.id != active.id) {
            return@confirm sendMessage(errorMessage("Das Event ${active.name} läuft nicht mehr."))
        }
        reply({ eventRpc.stop() }) { it.message() }
        ViewService.reloadAll()
    }
}
