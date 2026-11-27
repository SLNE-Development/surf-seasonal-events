package dev.slne.surf.seasonalevents.paper.npc

import dev.jorel.commandapi.CommandAPI
import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.kotlindsl.argument
import dev.jorel.commandapi.kotlindsl.literalArgument
import dev.jorel.commandapi.kotlindsl.stringArgument
import dev.jorel.commandapi.executors.CommandArguments
import dev.slne.surf.api.core.command.args.awaiting
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.paper.command.executors.anyExecutorSuspend
import dev.slne.surf.npc.api.npc.Npc
import dev.slne.surf.seasonalevents.api.event.SeasonalEvent
import dev.slne.surf.seasonalevents.api.result.UpdateEventResult
import dev.slne.surf.seasonalevents.core.client.rpc.RpcOutcome
import dev.slne.surf.seasonalevents.core.client.rpc.eventRpc
import dev.slne.surf.seasonalevents.core.client.rpc.rpcCall
import dev.slne.surf.seasonalevents.paper.command.argument.SeasonalEventArgument
import dev.slne.surf.seasonalevents.paper.command.message
import dev.slne.surf.seasonalevents.paper.command.unavailableMessage
import dev.slne.surf.seasonalevents.paper.render.ViewService
import dev.slne.surf.seasonalevents.paper.util.permission.SeasonalEventsPermissions
import net.kyori.adventure.audience.Audience

/**
 * `npc <event> <npc> [remove]`: links a surf-npc NPC to an event, so clicking it talks to the player about
 * their progress, or removes the link again. While linked, players see the event's blocks only after
 * talking to the NPC once. Only registered when [EventNpcs.enabled].
 */
internal fun CommandTree.npcSubcommand() = literalArgument("npc") {
    withPermission(SeasonalEventsPermissions.COMMAND_NPC)
    argument(SeasonalEventArgument("event")) {
        stringArgument("npc") {
            replaceSuggestions(ArgumentSuggestions.strings { NpcLinks.names().toTypedArray() })

            anyExecutorSuspend { sender, args ->
                val npc = args.npc()
                val event: SeasonalEvent = args.awaiting("event")
                if (!sender.setVisitRequired(event, true)) return@anyExecutorSuspend
                NpcLinks.link(npc, event)
                sender.sendText {
                    appendSuccessPrefix()
                    success("Der NPC ")
                    variableValue(npc.uniqueName)
                    success(" gehört jetzt zum Event ")
                    variableValue(event.name)
                    success(". Spieler sehen die Blöcke erst, nachdem sie einmal mit ihm gesprochen haben.")
                }
            }

            literalArgument("remove") {
                anyExecutorSuspend { sender, args ->
                    val event: SeasonalEvent = args.awaiting("event")
                    sender.unlink(args.npc(), event)
                }
            }
        }
    }
}

/** Reads the `npc` argument; an unknown name becomes a normal command error. */
private fun CommandArguments.npc(): Npc {
    val name = this["npc"] as String
    return NpcLinks.find(name) ?: throw CommandAPI.failWithString("Unbekannter NPC '$name'.")
}

/**
 * Turns the NPC visit requirement of [event] on or off and reloads every view, so blocks appear or vanish
 * right away. Returns false and tells the sender if it failed.
 */
private suspend fun Audience.setVisitRequired(event: SeasonalEvent, required: Boolean): Boolean {
    val outcome = rpcCall { eventRpc.setNpcVisitRequired(event.id, required) }
    if (outcome !is RpcOutcome.Ok) {
        sendMessage(unavailableMessage())
        return false
    }
    if (outcome.value !is UpdateEventResult.Success) {
        sendMessage(outcome.value.message())
        return false
    }
    ViewService.reloadAll()
    return true
}

private suspend fun Audience.unlink(npc: Npc, event: SeasonalEvent) {
    if (NpcLinks.eventNameOf(npc) != event.name) {
        return sendText {
            appendErrorPrefix()
            error("Der NPC ${npc.uniqueName} gehört nicht zum Event ${event.name}.")
        }
    }
    if (!setVisitRequired(event, false)) return
    NpcLinks.unlink(npc)
    sendText {
        appendSuccessPrefix()
        success("Der NPC ")
        variableValue(npc.uniqueName)
        success(" gehört zu keinem Event mehr. Die Blöcke sind jetzt auch ohne NPC-Besuch sichtbar.")
    }
}
