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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
        val old = cache.markers(player.uniqueId).associateBy { it.position }
        val new = markers.associateBy { it.position }
        cache.set(player.uniqueId, markers)

        withContext(plugin.entityDispatcher(player)) {
            old.keys.filter { it !in new }.forEach { FakeBlockRenderer.hide(player, it) }
            new.values.filter { old[it.position] != it }.forEach { FakeBlockRenderer.show(player, it) }
        }
    }

    /** Reloads the protected positions and the view of every online player, e.g. after start or stop. */
    suspend fun reloadAll() {
        reloadProtected()
        Bukkit.getOnlinePlayers().forEach { reload(it) }
    }

    /**
     * After a marker of [eventId] was added or removed: reloads the protected positions and the view of
     * everyone it can affect, i.e. staff editing that event and, if it is running, all players.
     */
    suspend fun reloadAfterMarkerChange(eventId: ULong) {
        reloadProtected()
        val activeId = (rpcCall { eventRpc.getActiveEvent() } as? RpcOutcome.Ok)?.value?.id
        Bukkit.getOnlinePlayers()
            .filter { activeId == eventId || editing[it.uniqueId] == eventId }
            .forEach { reload(it) }
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

    private suspend fun reloadProtected() {
        val events = rpcCall { eventRpc.findAll() } as? RpcOutcome.Ok ?: return
        val positions = HashSet<MarkerPosition>()
        for (event in events.value) {
            val markers = rpcCall { markerRpc.findByEvent(event.id) } as? RpcOutcome.Ok ?: return
            markers.value.mapTo(positions) { it.position }
        }
        protectedPositions = positions
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
