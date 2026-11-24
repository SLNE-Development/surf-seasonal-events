package dev.slne.surf.seasonalevents.paper.render

import dev.slne.surf.seasonalevents.api.marker.EventMarker
import dev.slne.surf.seasonalevents.api.marker.MarkerPosition
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * The markers each online player currently sees. Read from Netty and region threads, so every player
 * holds an immutable snapshot that is replaced as a whole.
 */
class PlayerViewCache {

    private data class ChunkKey(val world: UUID, val x: Int, val z: Int)

    private class Snapshot(markers: Collection<EventMarker>) {
        val byPosition: Map<MarkerPosition, EventMarker> = markers.associateBy { it.position }
        val byChunk: Map<ChunkKey, List<EventMarker>> = markers.groupBy {
            ChunkKey(it.position.world, it.position.x shr 4, it.position.z shr 4)
        }
    }

    private val views = ConcurrentHashMap<UUID, Snapshot>()

    fun set(player: UUID, markers: Collection<EventMarker>) {
        views[player] = Snapshot(markers)
    }

    fun markers(player: UUID): Collection<EventMarker> = views[player]?.byPosition?.values.orEmpty()

    fun markerAt(player: UUID, position: MarkerPosition): EventMarker? = views[player]?.byPosition?.get(position)

    fun markersInChunk(player: UUID, world: UUID, chunkX: Int, chunkZ: Int): List<EventMarker> =
        views[player]?.byChunk?.get(ChunkKey(world, chunkX, chunkZ)).orEmpty()

    fun remove(player: UUID, markerId: ULong) {
        views.computeIfPresent(player) { _, snapshot ->
            Snapshot(snapshot.byPosition.values.filter { it.id != markerId })
        }
    }

    fun clear(player: UUID) {
        views.remove(player)
    }
}
