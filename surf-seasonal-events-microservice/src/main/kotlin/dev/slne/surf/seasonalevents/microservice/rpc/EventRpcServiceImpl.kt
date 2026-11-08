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
import dev.slne.surf.seasonalevents.microservice.db.repository.LootRepository
import dev.slne.surf.seasonalevents.microservice.db.repository.MarkerRepository
import dev.slne.surf.seasonalevents.microservice.logic.EventRules
import dev.slne.surf.seasonalevents.microservice.logic.PackageRequirement
import dev.slne.surf.seasonalevents.microservice.logic.SeasonalClock
import java.time.Instant

/**
 * Microservice side of [EventRpcService]: combines [EventRules] with [EventRepository].
 * Every write runs under [StaffWriteLock].
 *
 * @param clock source of "now"; replaceable to test end dates
 */
class EventRpcServiceImpl(private val clock: SeasonalClock = SeasonalClock()) : EventRpcService {

    override suspend fun create(name: String, mode: EventMode, markerLimit: Int, dailyLimit: Int): CreateEventResult =
        StaffWriteLock.withLock {
            suspendTransaction {
                EventRules.checkCreate(name, markerLimit, dailyLimit)?.let { return@suspendTransaction it }
                if (EventRepository.findByName(name) != null) return@suspendTransaction CreateEventResult.NameTaken

                CreateEventResult.Success(EventRepository.insert(name, mode, markerLimit, dailyLimit))
            }
        }

    override suspend fun start(eventId: ULong, endsAt: Instant?): StartEventResult = StaffWriteLock.withLock {
        suspendTransaction {
            val target = EventRepository.findById(eventId)
                ?: return@suspendTransaction StartEventResult.NotFound

            val markerCount = MarkerRepository.count(eventId)
            EventRules.checkStart(
                target = target,
                active = getActiveEvent(),
                markerCount = markerCount,
                packageCount = LootRepository.countFilled(eventId),
                requiredPackages = PackageRequirement.required(target.mode, markerCount, clock.today(), endsAt),
            )?.let { return@suspendTransaction it }

            val started = EventRepository.updateState(eventId, EventState.ACTIVE, startedAt = clock.now(), endsAt = endsAt)
                ?: return@suspendTransaction StartEventResult.NotFound
            StartEventResult.Success(started)
        }
    }

    override suspend fun findAll(): List<SeasonalEvent> = EventRepository.findAll()

    override suspend fun findByName(name: String): SeasonalEvent? = EventRepository.findByName(name)

    override suspend fun getActiveEvent(): SeasonalEvent? = suspendTransaction {
        val active = EventRepository.findActive() ?: return@suspendTransaction null

        // Lazy expiry: an event past its end is ended on the first read after that. Ending it twice in
        // parallel writes the same values, so this read needs no lock.
        if (EventRules.isExpired(active, clock.now())) {
            EventRepository.updateState(active.id, EventState.ENDED, active.startedAt, active.endsAt)
            return@suspendTransaction null
        }
        active
    }

    override suspend fun setDisplayName(eventId: ULong, displayName: String): UpdateEventResult =
        StaffWriteLock.withLock {
            suspendTransaction {
                val event = EventRepository.findById(eventId) ?: return@suspendTransaction UpdateEventResult.NotFound
                EventRules.checkDisplayName(event, displayName)?.let { return@suspendTransaction it }

                val updated = EventRepository.updateDisplayName(eventId, displayName)
                    ?: return@suspendTransaction UpdateEventResult.NotFound
                UpdateEventResult.Success(updated)
            }
        }

    override suspend fun setEnd(eventId: ULong, endsAt: Instant?): UpdateEventResult = StaffWriteLock.withLock {
        suspendTransaction {
            val event = EventRepository.findById(eventId) ?: return@suspendTransaction UpdateEventResult.NotFound
            if (event.state == EventState.ENDED) return@suspendTransaction UpdateEventResult.AlreadyEnded
            PackageGuard.shortageAfter(event, endsAt = endsAt)?.let {
                return@suspendTransaction UpdateEventResult.NotEnoughPackages(it.filled, it.needed)
            }

            val updated = EventRepository.updateEndsAt(eventId, endsAt)
                ?: return@suspendTransaction UpdateEventResult.NotFound
            UpdateEventResult.Success(updated)
        }
    }

    override suspend fun stop(): StopEventResult = StaffWriteLock.withLock {
        suspendTransaction {
            val active = getActiveEvent() ?: return@suspendTransaction StopEventResult.NoActiveEvent

            // Keep startedAt; a manual stop ends the event now.
            val stopped = EventRepository.updateState(active.id, EventState.ENDED, active.startedAt, clock.now())
                ?: return@suspendTransaction StopEventResult.NoActiveEvent
            StopEventResult.Success(stopped)
        }
    }

    override suspend fun delete(eventId: ULong): DeleteEventResult = StaffWriteLock.withLock {
        suspendTransaction {
            val event = EventRepository.findById(eventId) ?: return@suspendTransaction DeleteEventResult.NotFound
            EventRules.checkDelete(event, MarkerRepository.count(eventId))?.let { return@suspendTransaction it }

            EventRepository.delete(eventId)
            DeleteEventResult.Success
        }
    }
}
