package dev.slne.surf.seasonalevents.core.rpc

import dev.slne.surf.rabbitmq.api.rpc.RpcService
import dev.slne.surf.seasonalevents.api.marker.EventMarker
import dev.slne.surf.seasonalevents.api.marker.MarkerPosition
import dev.slne.surf.seasonalevents.api.result.MarkResult
import dev.slne.surf.seasonalevents.api.result.MarkerDayResult
import dev.slne.surf.seasonalevents.api.result.RestoreResult
import dev.slne.surf.seasonalevents.api.result.UnmarkResult
import java.time.LocalDate
import java.util.UUID

/**
 * Manages the blocks marked as event objects. The microservice only stores them;
 * removing and restoring the real blocks in the world is up to the plugin.
 */
@RpcService
interface MarkerRpcService {

    /**
     * Marks a block as an event object.
     *
     * @param blockData the original block, as `BlockData#getAsString()`
     * @param skullTexture texture property of a player head, null for every other block
     * @param markedBy the staff member who marked the block
     * @return [MarkResult.LimitReached] if the event has reached its marker limit,
     * [MarkResult.AlreadyMarked] if the position belongs to any event,
     * [MarkResult.EventEnded] if the event is `ENDED`
     */
    suspend fun mark(
        eventId: ULong,
        position: MarkerPosition,
        blockData: String,
        skullTexture: String?,
        markedBy: UUID,
    ): MarkResult

    /**
     * Removes the marker at [position]. Today's assignments to it are dropped, so affected players get
     * a new block on their next view; past collections are kept.
     *
     * @return the removed marker, so the plugin can put the real block back, or [UnmarkResult.NotMarked]
     */
    suspend fun unmark(position: MarkerPosition): UnmarkResult

    /**
     * Pins a marker of a `DAILY` event to [day], making it that day's hiding spot for every player, or
     * releases it into the random pool with null.
     *
     * Allowed while the event runs, so mistakes can be fixed. Progress belongs to the calendar day, not to the
     * block: a day a player found stays found, and a marker moved to another day is that day's hiding spot,
     * found or not. A random assignment whose marker gets pinned is replaced on the player's next view.
     *
     * @return [MarkerDayResult.DayTaken] if another marker of the event is pinned to [day],
     * [MarkerDayResult.NotDailyEvent] for a `COLLECT` event, [MarkerDayResult.EventEnded] if it has ended,
     * [MarkerDayResult.NotEnoughPackages] if a day with a block would be left without a package
     */
    suspend fun setDay(markerId: ULong, day: LocalDate?): MarkerDayResult

    /** Returns all markers of an event, empty if the event does not exist. */
    suspend fun findByEvent(eventId: ULong): List<EventMarker>

    /**
     * Returns the position of every stored marker of every event. These positions stay air in the world
     * until their event is restored, so the plugin keeps them free.
     */
    suspend fun findAllPositions(): List<MarkerPosition>

    /**
     * Deletes all markers of an event and returns them, so the plugin can put the real blocks back.
     *
     * @return [RestoreResult.IsActive] if the event is running
     */
    suspend fun restore(eventId: ULong): RestoreResult
}
