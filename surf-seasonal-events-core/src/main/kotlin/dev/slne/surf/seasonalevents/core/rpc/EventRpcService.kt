package dev.slne.surf.seasonalevents.core.rpc

import dev.slne.surf.rabbitmq.api.rpc.RpcService
import dev.slne.surf.seasonalevents.api.event.EventMode
import dev.slne.surf.seasonalevents.api.event.SeasonalEvent
import dev.slne.surf.seasonalevents.api.result.CreateEventResult
import dev.slne.surf.seasonalevents.api.result.DeleteEventResult
import dev.slne.surf.seasonalevents.api.result.StartEventResult
import dev.slne.surf.seasonalevents.api.result.StopEventResult
import dev.slne.surf.seasonalevents.api.result.UpdateEventResult
import java.time.Instant

/**
 * Manages the lifecycle of seasonal events (`DRAFT` → `ACTIVE` → `ENDED`).
 * Implemented by the microservice, which guarantees that at most one event is active at a time.
 */
@RpcService
interface EventRpcService {

    /**
     * Creates a new event in state `DRAFT`.
     *
     * @param name unique technical name, e.g. `christmas-2026`
     * @param markerLimit maximum number of markers, must be greater than 0
     * @param dailyLimit collections per player and day in a `COLLECT` event, 0 for unlimited;
     * ignored for `DAILY`
     * @return [CreateEventResult.NameTaken] if the name exists,
     * [CreateEventResult.InvalidLimit] if a limit is out of range
     */
    suspend fun create(name: String, mode: EventMode, markerLimit: Int, dailyLimit: Int): CreateEventResult

    /** Returns all events in every state. */
    suspend fun findAll(): List<SeasonalEvent>

    /** Returns the event with the technical [name], or null if there is none. */
    suspend fun findByName(name: String): SeasonalEvent?

    /**
     * Returns the currently active event, or null if none is running.
     * An active event whose `endsAt` has passed is ended by this call and is not returned.
     */
    suspend fun getActiveEvent(): SeasonalEvent?

    /**
     * Sets the MiniMessage display name of an event.
     *
     * @return [UpdateEventResult.AlreadyEnded] if the event is `ENDED`
     */
    suspend fun setDisplayName(eventId: ULong, displayName: String): UpdateEventResult

    /**
     * Activates a `DRAFT` event.
     *
     * @param endsAt when the event ends automatically, or null to run until stopped
     * @return [StartEventResult.AnotherEventActive] if another event is running,
     * [StartEventResult.NoMarkers] or [StartEventResult.NoPackages] if the event is not set up yet
     */
    suspend fun start(eventId: ULong, endsAt: Instant?): StartEventResult

    /**
     * Sets or clears the automatic end of an event.
     *
     * @param endsAt the new end, or null to remove it
     * @return [UpdateEventResult.AlreadyEnded] if the event is `ENDED`
     */
    suspend fun setEnd(eventId: ULong, endsAt: Instant?): UpdateEventResult

    /**
     * Ends the active event for good. Its markers stay in the database until they are restored.
     *
     * @return [StopEventResult.NoActiveEvent] if no event is running
     */
    suspend fun stop(): StopEventResult

    /**
     * Deletes an event with all its markers, packages, assignments and collections.
     *
     * @return [DeleteEventResult.IsActive] if the event is running; only `DRAFT` and `ENDED` can be deleted
     */
    suspend fun delete(eventId: ULong): DeleteEventResult
}
