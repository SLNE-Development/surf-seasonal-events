package dev.slne.surf.seasonalevents.microservice.rpc

import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import dev.slne.surf.seasonalevents.api.event.EventMode
import dev.slne.surf.seasonalevents.api.event.EventState
import dev.slne.surf.seasonalevents.api.event.SeasonalEvent
import dev.slne.surf.seasonalevents.api.result.CreateEventResult
import dev.slne.surf.seasonalevents.api.result.DeleteEventResult
import dev.slne.surf.seasonalevents.api.result.StartEventResult
import dev.slne.surf.seasonalevents.api.result.StopEventResult
import dev.slne.surf.seasonalevents.api.result.UpdateEventResult
import dev.slne.surf.seasonalevents.core.rpc.EventRpcService
import dev.slne.surf.seasonalevents.microservice.db.repository.EventRepository
import dev.slne.surf.seasonalevents.microservice.logic.EventRules
import java.time.Instant

/** Microservice side of [EventRpcService]: combines [EventRules] with [EventRepository]. */
object EventRpcServiceImpl : EventRpcService {

    override suspend fun create(name: String, mode: EventMode, markerLimit: Int, dailyLimit: Int): CreateEventResult {
        // a. Rule check: the limits must be valid.
        EventRules.checkCreate(markerLimit, dailyLimit)?.let { return it }

        // b. The technical name must be free.
        if (EventRepository.findByName(name) != null) return CreateEventResult.NameTaken

        // c. Insert and report success.
        val event = EventRepository.insert(name, mode, markerLimit, dailyLimit)
        return CreateEventResult.Success(event)
    }

    override suspend fun start(eventId: ULong, endsAt: Instant?): StartEventResult = suspendTransaction {
        // Inside the lambda, `return` needs the label `@suspendTransaction`.
        // a. The event must exist.
        val target = EventRepository.findById(eventId)
            ?: return@suspendTransaction StartEventResult.NotFound

        // b. Rule check: not ended, no other active event, markers and packages exist.
        EventRules.checkStart(
            target = target,
            active = EventRepository.findActive(),
            markerCount = EventRepository.countMarkers(eventId),
            packageCount = EventRepository.countPackages(eventId),
        )?.let { return@suspendTransaction it }

        // c. Activate and report success.
        val started = EventRepository.updateState(eventId, EventState.ACTIVE, startedAt = Instant.now(), endsAt = endsAt)
            ?: return@suspendTransaction StartEventResult.NotFound
        StartEventResult.Success(started)
    }

    override suspend fun findAll(): List<SeasonalEvent> = EventRepository.findAll()

    override suspend fun findByName(name: String): SeasonalEvent? = EventRepository.findByName(name)

    override suspend fun getActiveEvent(): SeasonalEvent? = suspendTransaction {
        val active = EventRepository.findActive() ?: return@suspendTransaction null

        // Lazy expiry: an event past its end is ended on the first read after that.
        if (EventRules.isExpired(active, Instant.now())) {
            EventRepository.updateState(active.id, EventState.ENDED, active.startedAt, active.endsAt)
            return@suspendTransaction null
        }
        active
    }

    override suspend fun setDisplayName(eventId: ULong, displayName: String): UpdateEventResult =
        suspendTransaction {
            val event = EventRepository.findById(eventId) ?: return@suspendTransaction UpdateEventResult.NotFound
            if (event.state == EventState.ENDED) return@suspendTransaction UpdateEventResult.AlreadyEnded

            val updated = EventRepository.updateDisplayName(eventId, displayName)
                ?: return@suspendTransaction UpdateEventResult.NotFound
            UpdateEventResult.Success(updated)
        }

    override suspend fun setEnd(eventId: ULong, endsAt: Instant?): UpdateEventResult = suspendTransaction {
        val event = EventRepository.findById(eventId) ?: return@suspendTransaction UpdateEventResult.NotFound
        if (event.state == EventState.ENDED) return@suspendTransaction UpdateEventResult.AlreadyEnded

        val updated = EventRepository.updateEndsAt(eventId, endsAt)
            ?: return@suspendTransaction UpdateEventResult.NotFound
        UpdateEventResult.Success(updated)
    }

    override suspend fun stop(): StopEventResult = suspendTransaction {
        val active = EventRepository.findActive() ?: return@suspendTransaction StopEventResult.NoActiveEvent

        // Keep startedAt; a manual stop ends the event now.
        val stopped = EventRepository.updateState(active.id, EventState.ENDED, active.startedAt, Instant.now())
            ?: return@suspendTransaction StopEventResult.NoActiveEvent
        StopEventResult.Success(stopped)
    }

    override suspend fun delete(eventId: ULong): DeleteEventResult = suspendTransaction {
        val event = EventRepository.findById(eventId) ?: return@suspendTransaction DeleteEventResult.NotFound
        if (event.state == EventState.ACTIVE) return@suspendTransaction DeleteEventResult.IsActive

        EventRepository.delete(eventId)
        DeleteEventResult.Success
    }
}
