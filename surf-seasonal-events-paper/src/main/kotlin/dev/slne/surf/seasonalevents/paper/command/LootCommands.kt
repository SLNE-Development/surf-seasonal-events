package dev.slne.surf.seasonalevents.paper.command

import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.seasonalevents.api.event.EventState
import dev.slne.surf.seasonalevents.api.event.SeasonalEvent
import dev.slne.surf.seasonalevents.api.result.UpdateLootResult
import dev.slne.surf.seasonalevents.core.client.rpc.RpcOutcome
import dev.slne.surf.seasonalevents.core.client.rpc.lootRpc
import dev.slne.surf.seasonalevents.core.client.rpc.rpcCall
import net.kyori.adventure.audience.Audience

// Logic of `copyloot`; the command tree is in SeasonalEventsCommand.kt.

/**
 * Copies every package of [from] into [to], e.g. to reuse last year's Christmas loot. Fixed days are not
 * copied, because they belong to a specific year; they have to be set again in the target event.
 */
internal suspend fun Audience.copyLoot(from: SeasonalEvent, to: SeasonalEvent) {
    if (from.id == to.id) return sendMessage(errorMessage("Quelle und Ziel sind dasselbe Event."))
    if (to.state == EventState.ENDED) return sendMessage(lootOfEndedEventMessage(to.name))

    val source = rpcCall { lootRpc.findByEvent(from.id) }
    if (source !is RpcOutcome.Ok) return sendMessage(unavailableMessage())
    if (source.value.isEmpty()) return sendMessage(errorMessage("${from.name} hat keine Pakete."))

    var copied = 0
    for (original in source.value) {
        val created = rpcCall { lootRpc.create(to.id, original.name) }
        if (created !is RpcOutcome.Ok) break
        val copy = (created.value as? UpdateLootResult.Success)?.lootPackage ?: break

        if (original.items.isNotEmpty()) {
            val saved = rpcCall { lootRpc.save(copy.id, expectedItems = copy.items, items = original.items) }
            if ((saved as? RpcOutcome.Ok)?.value !is UpdateLootResult.Success) break
        }
        copied++
    }

    val skippedDays = source.value.count { it.fixedDay != null }
    sendText {
        if (copied == source.value.size) appendSuccessPrefix() else appendWarningPrefix()
        variableValue("$copied/${source.value.size}")
        success(" Pakete von ")
        variableValue(from.name)
        success(" nach ")
        variableValue(to.name)
        success(" kopiert.")
        if (copied < source.value.size) warning(" Der Rest ist fehlgeschlagen, versuche es erneut.")
        if (skippedDays > 0) {
            appendNewWarningPrefixedLine {
                warning("$skippedDays feste Tage wurden nicht übernommen, setze sie im Ziel-Event neu.")
            }
        }
    }
}
