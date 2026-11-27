package dev.slne.surf.seasonalevents.paper.hud

import com.github.shynixn.mccoroutine.folia.entityDispatcher
import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.core.util.logger
import dev.slne.surf.seasonalevents.api.progress.PlayerStatus
import dev.slne.surf.seasonalevents.core.client.rpc.RpcOutcome
import dev.slne.surf.seasonalevents.core.client.rpc.progressRpc
import dev.slne.surf.seasonalevents.core.client.rpc.rpcCall
import dev.slne.surf.seasonalevents.paper.plugin
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds

/**
 * Shows a player their progress right after they received something, e.g. "Gefunden! 5/24": for a few
 * seconds in the surf-hud if it runs, otherwise in the action bar.
 */
object GiftHud {

    private const val HUD_PLUGIN = "surf-hud-paper"
    private val SHOWN_FOR = 5.seconds

    /** Pending hide of each player's HUD element, replaced when a newer one is shown. */
    private val hideJobs = ConcurrentHashMap<UUID, Job>()

    private val hudEnabled get() = Bukkit.getPluginManager().isPluginEnabled(HUD_PLUGIN)

    /** Loads the progress of [player] in the active event and shows it; can be called from any thread. */
    suspend fun showProgress(player: Player) {
        val status = (rpcCall { progressRpc.getStatus(player.uniqueId) } as? RpcOutcome.Ok)?.value ?: return
        val (found, total) = when (status) {
            PlayerStatus.NoActiveEvent -> return
            is PlayerStatus.Daily -> status.found to status.total
            is PlayerStatus.Collect -> status.found to status.total
        }
        val content = buildText {
            success("Gefunden! ")
            variableValue("$found/$total")
        }

        withContext(plugin.entityDispatcher(player)) {
            if (!hudEnabled || !showInHud(player, content)) player.sendActionBar(content)
        }
    }

    /** Shows [content] in the surf-hud and hides it after [SHOWN_FOR]; false if surf-hud failed. */
    private fun showInHud(player: Player, content: Component): Boolean {
        try {
            SurfHudBridge.show(player, content)
        } catch (e: Exception) {
            log.atWarning().withCause(e).log("Could not show the progress in surf-hud, using the action bar")
            return false
        }
        hideJobs.put(
            player.uniqueId,
            plugin.launch {
                delay(SHOWN_FOR)
                withContext(plugin.entityDispatcher(player)) { if (player.isOnline) SurfHudBridge.hide(player) }
            },
        )?.cancel()
        return true
    }

    /** Forgets everything about [player] when they leave. */
    fun forget(player: UUID) {
        hideJobs.remove(player)?.cancel()
    }
}

private val log = logger()
