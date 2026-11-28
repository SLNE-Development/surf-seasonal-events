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
import dev.slne.surf.api.core.util.logger
import dev.slne.surf.api.paper.command.executors.anyExecutorSuspend
import dev.slne.surf.npc.api.npc.Npc
import dev.slne.surf.seasonalevents.api.event.SeasonalEvent
import dev.slne.surf.seasonalevents.api.result.UpdateEventResult
import dev.slne.surf.seasonalevents.core.client.rpc.RpcOutcome
import dev.slne.surf.seasonalevents.core.client.rpc.eventRpc
import dev.slne.surf.seasonalevents.core.client.rpc.rpcCall
import dev.slne.surf.seasonalevents.paper.command.argument.SeasonalEventArgument
import dev.slne.surf.seasonalevents.paper.command.errorMessage
import dev.slne.surf.seasonalevents.paper.command.message
import dev.slne.surf.seasonalevents.paper.command.unavailableMessage
import dev.slne.surf.seasonalevents.paper.render.ViewService
import dev.slne.surf.seasonalevents.paper.util.permission.SeasonalEventsPermissions
import net.kyori.adventure.audience.Audience

/**
 * `npc <event> <npc> [remove]`: links a surf-npc NPC to an event, so clicking it talks to the player about
 * their progress, or removes the link again. An event has at most one NPC; linking another one replaces it.
 * While linked, players see the event's blocks only after talking to the NPC once. Only registered when
 * [EventNpcs.enabled].
 */
internal fun CommandTree.npcSubcommand() = literalArgument("npc") {
    withPermission(SeasonalEventsPermissions.COMMAND_NPC)
    argument(SeasonalEventArgument("event")) {
        stringArgument("npc") {
            replaceSuggestions(ArgumentSuggestions.strings { NpcLinks.names().toTypedArray() })

            anyExecutorSuspend { sender, args -> sender.link(args.npc(), args.awaiting("event")) }

            literalArgument("remove") {
                anyExecutorSuspend { sender, args -> sender.unlink(args.npc(), args.awaiting("event")) }
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
 * Stores the link at the event first, which is the authority, then on the NPC. If the NPC cannot be
 * written, the event gets its previous NPC back, so it is never gated behind an NPC that does not answer.
 */
private suspend fun Audience.link(npc: Npc, event: SeasonalEvent) {
    val previous = event.npcName
    if (!setNpc(event, npc.uniqueName)) return

    try {
        NpcLinks.link(npc, event)
    } catch (e: Exception) {
        log.atWarning().withCause(e).log("Could not link NPC %s to event %s", npc.uniqueName, event.name)
        setNpc(event, previous)
        return sendMessage(errorMessage("Der NPC konnte nicht verknüpft werden, es bleibt alles wie vorher."))
    }
    // The previous NPC no longer talks for the event; its leftover property is only cleaned up.
    if (previous != null && previous != npc.uniqueName) NpcLinks.find(previous)?.let { runCatching { NpcLinks.unlink(it) } }

    sendText {
        appendSuccessPrefix()
        success("Der NPC ")
        variableValue(npc.uniqueName)
        success(" gehört jetzt zum Event ")
        variableValue(event.name)
        success(". Spieler sehen die Blöcke erst, nachdem sie einmal mit ihm gesprochen haben.")
        if (previous != null && previous != npc.uniqueName) {
            appendNewInfoPrefixedLine {
                info("Er ersetzt ")
                variableValue(previous)
                info(", der nicht mehr für das Event spricht.")
            }
        }
    }
}

private suspend fun Audience.unlink(npc: Npc, event: SeasonalEvent) {
    if (event.npcName != npc.uniqueName) {
        return sendMessage(errorMessage("Der NPC ${npc.uniqueName} gehört nicht zum Event ${event.name}."))
    }
    if (!setNpc(event, null)) return
    // The event no longer points to the NPC, so a failure here leaves only a harmless leftover property.
    runCatching { NpcLinks.unlink(npc) }
    sendText {
        appendSuccessPrefix()
        success("Der NPC ")
        variableValue(npc.uniqueName)
        success(" gehört zu keinem Event mehr. Die Blöcke sind jetzt auch ohne NPC-Besuch sichtbar.")
    }
}

/**
 * Stores [npcName] as the NPC of [event] and reloads every view, so blocks appear or vanish right away.
 * Returns false and tells the sender if it failed.
 */
private suspend fun Audience.setNpc(event: SeasonalEvent, npcName: String?): Boolean {
    val outcome = rpcCall { eventRpc.setNpc(event.id, npcName) }
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

private val log = logger()
