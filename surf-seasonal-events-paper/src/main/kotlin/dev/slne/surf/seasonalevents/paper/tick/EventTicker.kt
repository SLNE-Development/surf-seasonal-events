package dev.slne.surf.seasonalevents.paper.tick

import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.api.core.util.logger
import dev.slne.surf.seasonalevents.core.client.rpc.RpcOutcome
import dev.slne.surf.seasonalevents.core.client.rpc.eventRpc
import dev.slne.surf.seasonalevents.core.client.rpc.rpcCall
import dev.slne.surf.seasonalevents.paper.plugin
import dev.slne.surf.seasonalevents.paper.render.ViewService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.LocalDate
import java.time.ZoneId
import kotlin.time.Duration.Companion.seconds

/**
 * Checks every 15 seconds whether the day (Europe/Berlin) or the active event changed and then reloads
 * every view: a new day brings new `DAILY` blocks, and an end date that passed ends the event, because
 * the microservice ends it lazily when it is asked for the active event.
 */
class EventTicker(private val today: () -> LocalDate = { LocalDate.now(ZoneId.of("Europe/Berlin")) }) {

    private var job: Job? = null
    private var lastDay: LocalDate? = null
    private var lastActiveId: ULong? = null

    fun start() {
        job = plugin.launch {
            while (isActive) {
                tick()
                delay(INTERVAL)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    suspend fun tick() {
        // If the microservice is unreachable, rpcCall has logged it; the next tick tries again.
        val active = rpcCall { eventRpc.getActiveEvent() } as? RpcOutcome.Ok ?: return
        val day = today()
        val activeId = active.value?.id

        val firstTick = lastDay == null
        val changed = day != lastDay || activeId != lastActiveId
        lastDay = day
        lastActiveId = activeId

        // The first tick only remembers the state; the plugin reloads all views on enable anyway.
        if (changed && !firstTick) {
            log.atInfo().log("Day is %s, active event is %s: reloading all views", day, activeId ?: "none")
            ViewService.reloadAll()
        }
    }
}

/** Short enough that blocks vanish soon after an end date, cheap because it is a single RPC call. */
private val INTERVAL = 15.seconds

private val log = logger()
