package dev.slne.surf.seasonalevents.paper.command.argument

import com.github.shynixn.mccoroutine.folia.scope
import dev.jorel.commandapi.CommandAPI
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.arguments.StringArgument
import dev.slne.surf.api.paper.command.args.SuspendCustomArgument
import dev.slne.surf.seasonalevents.api.event.SeasonalEvent
import dev.slne.surf.seasonalevents.core.client.rpc.RpcOutcome
import dev.slne.surf.seasonalevents.core.client.rpc.eventRpc
import dev.slne.surf.seasonalevents.core.client.rpc.rpcCall
import dev.slne.surf.seasonalevents.paper.plugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.future
import org.bukkit.command.CommandSender

/** A seasonal event, typed by its technical name; suggests all existing events. */
class SeasonalEventArgument(nodeName: String) :
    SuspendCustomArgument<SeasonalEvent, String>(StringArgument(nodeName)) {

    companion object {
        private val suggestions = ArgumentSuggestions<CommandSender> { info, builder ->
            plugin.scope.future {
                val events = rpcCall { eventRpc.findAll() }
                if (events is RpcOutcome.Ok) {
                    events.value
                        .filter { it.name.startsWith(info.currentArg, ignoreCase = true) }
                        .forEach { builder.suggest(it.name) }
                }
                builder.build()
            }
        }
    }

    init {
        replaceSuggestions(suggestions)
    }

    override suspend fun CoroutineScope.parse(info: CustomArgumentInfo<String>): SeasonalEvent {
        val name = info.currentInput
        return when (val outcome = rpcCall { eventRpc.findByName(name) }) {
            is RpcOutcome.Unavailable -> throw CommandAPI.failWithString("Das Event-System ist gerade nicht erreichbar.")
            is RpcOutcome.Ok -> outcome.value ?: throw CommandAPI.failWithString("Unbekanntes Event '$name'.")
        }
    }
}
