package dev.slne.surf.seasonalevents.paper.render

import com.github.shynixn.mccoroutine.folia.entityDispatcher
import dev.slne.surf.seasonalevents.api.marker.EventMarker
import dev.slne.surf.seasonalevents.api.marker.MarkerPosition
import dev.slne.surf.seasonalevents.core.client.rpc.RpcOutcome
import dev.slne.surf.seasonalevents.core.client.rpc.eventRpc
import dev.slne.surf.seasonalevents.core.client.rpc.markerRpc
import dev.slne.surf.seasonalevents.core.client.rpc.progressRpc
import dev.slne.surf.seasonalevents.core.client.rpc.rpcCall
import dev.slne.surf.seasonalevents.paper.plugin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.bukkit.Bukkit
import org.bukkit.block.Block
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Decides which markers each player sees and keeps their client in sync: players see their view from
 * the microservice, staff in edit mode see every marker of the edited event.
 */
object ViewService {

    /** Upper bound of view reloads running at the same time in [reloadAll]. */
    private const val PARALLEL_RELOADS = 16

    val cache = PlayerViewCache()

    private val editing = ConcurrentHashMap<UUID, ULong>()
    private val reloadLocks = ConcurrentHashMap<UUID, Mutex>()

    /**
     * Positions of every marker still stored, of any event. They stay air on the server until restored,
     * so nothing may be placed there. Replaced as a whole by [reloadProtected].
     */
    @Volatile
    private var protectedPositions: Set<MarkerPosition> = emptySet()

    fun isProtected(block: Block) = MarkerPosition(block.world.uid, block.x, block.y, block.z) in protectedPositions

    /**
     * Loads the view of [player] and sends only the difference: markers that disappeared are hidden,
     * new ones are shown. If the microservice is unreachable, the current view is kept.
     */
    suspend fun reload(player: Player) = reloadLocks.computeIfAbsent(player.uniqueId) { Mutex() }.withLock {
        val markers = loadMarkers(player.uniqueId) ?: return@withLock
        // The player may have left during the call; then nothing is cached for them again.
        if (!player.isOnline) return@withLock

        val old = cache.markers(player.uniqueId).associateBy { it.position }
        val new = markers.associateBy { it.position }
        cache.set(player.uniqueId, markers)

        withContext(plugin.entityDispatcher(player)) {
            old.keys.filter { it !in new }.forEach { FakeBlockRenderer.hide(player, it) }
            new.values.filter { old[it.position] != it }.forEach { FakeBlockRenderer.show(player, it) }
        }
    }

    /**
     * Reloads the protected positions and the view of every online player, e.g. after start or stop
     * or at a new day. Players are reloaded in parallel, at most [PARALLEL_RELOADS] at a time, so a full
     * server does not wait for hundreds of calls in a row and the microservice is not flooded either.
     */
    suspend fun reloadAll() {
        reloadProtected()
        reloadParallel(Bukkit.getOnlinePlayers().toList())
    }

    private suspend fun reloadParallel(players: List<Player>) = coroutineScope {
        val permits = Semaphore(PARALLEL_RELOADS)
        players.forEach { player -> launch { permits.withPermit { reload(player) } } }
    }

    /**
     * After a marker of [eventId] was added or removed: reloads the protected positions and the view of
     * everyone it can affect, i.e. staff editing that event and, if it is running, all players.
     */
    suspend fun reloadAfterMarkerChange(eventId: ULong) {
        reloadProtected()
        val activeId = (rpcCall { eventRpc.getActiveEvent() } as? RpcOutcome.Ok)?.value?.id
        reloadParallel(Bukkit.getOnlinePlayers().filter { activeId == eventId || editing[it.uniqueId] == eventId })
    }

    private suspend fun loadMarkers(player: UUID): List<EventMarker>? {
        val eventId = editing[player]
        val outcome = if (eventId != null) {
            rpcCall { progressRpc.getEditView(eventId) }
        } else {
            rpcCall { progressRpc.getPlayerView(player)?.markers.orEmpty() }
        }
        return (outcome as? RpcOutcome.Ok)?.value
    }

    /** Keeps the old positions if the microservice is unreachable. */
    private suspend fun reloadProtected() {
        val positions = rpcCall { markerRpc.findAllPositions() } as? RpcOutcome.Ok ?: return
        protectedPositions = positions.value.toHashSet()
    }

    /** Switches [player] to the edit view of [eventId]; call [reload] afterwards to send it. */
    fun enterEditMode(player: Player, eventId: ULong) {
        editing[player.uniqueId] = eventId
    }

    /** Switches [player] back to the normal view; call [reload] afterwards to send it. */
    fun leaveEditMode(player: Player) {
        editing.remove(player.uniqueId)
    }

    fun editingEventId(player: UUID): ULong? = editing[player]

    /** Forgets everything about [player] when they leave. */
    fun forget(player: UUID) {
        cache.clear(player)
        editing.remove(player)
        reloadLocks.remove(player)
    }
}
