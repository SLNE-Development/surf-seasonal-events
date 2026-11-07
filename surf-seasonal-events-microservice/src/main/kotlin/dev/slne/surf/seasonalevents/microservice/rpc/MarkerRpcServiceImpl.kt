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

/** Microservice side of [MarkerRpcService]: combines [MarkerRules] with [MarkerRepository]. */
object MarkerRpcServiceImpl : MarkerRpcService {

    override suspend fun mark(
        eventId: ULong,
        position: MarkerPosition,
        blockData: String,
        skullTexture: String?,
        markedBy: UUID,
    ): MarkResult = suspendTransaction {
        val event = EventRepository.findById(eventId) ?: return@suspendTransaction MarkResult.NotFound

        MarkerRules.checkMark(
            event = event,
            markerCount = MarkerRepository.count(eventId),
            positionTaken = MarkerRepository.findByPosition(position) != null,
        )?.let { return@suspendTransaction it }

        MarkResult.Success(MarkerRepository.insert(eventId, position, blockData, skullTexture, markedBy))
    }

    override suspend fun unmark(position: MarkerPosition): UnmarkResult = suspendTransaction {
        val marker = MarkerRepository.findByPosition(position) ?: return@suspendTransaction UnmarkResult.NotMarked

        // Assignments to the marker are removed by ON DELETE CASCADE.
        MarkerRepository.delete(marker.id)
        UnmarkResult.Success(marker)
    }

    override suspend fun findByEvent(eventId: ULong): List<EventMarker> = MarkerRepository.findByEvent(eventId)

    override suspend fun restore(eventId: ULong): RestoreResult = suspendTransaction {
        val event = EventRepository.findById(eventId) ?: return@suspendTransaction RestoreResult.NotFound
        if (event.state == EventState.ACTIVE) return@suspendTransaction RestoreResult.IsActive

        RestoreResult.Success(MarkerRepository.deleteByEvent(eventId))
    }
}
