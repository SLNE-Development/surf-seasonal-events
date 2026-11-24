package dev.slne.surf.seasonalevents.microservice.rpc

import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import dev.slne.surf.seasonalevents.api.event.EventState
import dev.slne.surf.seasonalevents.api.marker.EventMarker
import dev.slne.surf.seasonalevents.api.marker.MarkerPosition
import dev.slne.surf.seasonalevents.api.result.MarkResult
import dev.slne.surf.seasonalevents.api.result.RestoreResult
import dev.slne.surf.seasonalevents.api.result.UnmarkResult
import dev.slne.surf.seasonalevents.core.rpc.MarkerRpcService
import dev.slne.surf.seasonalevents.microservice.db.repository.EventRepository
import dev.slne.surf.seasonalevents.microservice.db.repository.MarkerRepository
import dev.slne.surf.seasonalevents.microservice.logic.MarkerRules
import java.util.UUID

/**
 * Microservice side of [MarkerRpcService]: combines [MarkerRules] with [MarkerRepository].
 * Every write runs under [StaffWriteLock], so parallel clicks cannot exceed the marker limit.
 */
object MarkerRpcServiceImpl : MarkerRpcService {

    override suspend fun mark(
        eventId: ULong,
        position: MarkerPosition,
        blockData: String,
        skullTexture: String?,
        markedBy: UUID,
    ): MarkResult = StaffWriteLock.withLock {
        suspendTransaction {
            val event = EventRepository.findById(eventId) ?: return@suspendTransaction MarkResult.NotFound

            val markerCount = MarkerRepository.count(eventId)
            MarkerRules.checkMark(
                event = event,
                markerCount = markerCount,
                positionTaken = MarkerRepository.findByPosition(position) != null,
            )?.let { return@suspendTransaction it }
            PackageGuard.shortageAfter(event, addedMarkers = 1)?.let {
                return@suspendTransaction MarkResult.NotEnoughPackages(it.filled, it.needed)
            }

            val marker = MarkerRepository.insert(eventId, position, blockData, skullTexture, markedBy)
            MarkResult.Success(marker, markerCount = markerCount + 1, markerLimit = event.markerLimit)
        }
    }

    override suspend fun unmark(position: MarkerPosition): UnmarkResult = StaffWriteLock.withLock {
        suspendTransaction {
            val marker = MarkerRepository.findByPosition(position) ?: return@suspendTransaction UnmarkResult.NotMarked

            // Assignments to the marker are removed by ON DELETE CASCADE.
            MarkerRepository.delete(marker.id)
            UnmarkResult.Success(
                marker = marker,
                markerCount = MarkerRepository.count(marker.eventId),
                markerLimit = EventRepository.findById(marker.eventId)?.markerLimit ?: 0,
            )
        }
    }

    override suspend fun findByEvent(eventId: ULong): List<EventMarker> = MarkerRepository.findByEvent(eventId)

    override suspend fun findAllPositions(): List<MarkerPosition> = MarkerRepository.findAllPositions()

    override suspend fun restore(eventId: ULong): RestoreResult = StaffWriteLock.withLock {
        suspendTransaction {
            val event = EventRepository.findById(eventId) ?: return@suspendTransaction RestoreResult.NotFound
            if (event.state == EventState.ACTIVE) return@suspendTransaction RestoreResult.IsActive

            RestoreResult.Success(MarkerRepository.deleteByEvent(eventId))
        }
    }
}
