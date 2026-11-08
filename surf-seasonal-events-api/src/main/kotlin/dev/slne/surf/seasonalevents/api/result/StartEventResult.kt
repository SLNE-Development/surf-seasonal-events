package dev.slne.surf.seasonalevents.api.result

import dev.slne.surf.seasonalevents.api.event.SeasonalEvent
import kotlinx.serialization.Serializable

/** Outcome of starting an event. */
@Serializable
sealed interface StartEventResult {
    /** The event is now active. */
    @Serializable
    data class Success(val event: SeasonalEvent) : StartEventResult

    /** Another event is still running; only one event can be active at a time. */
    @Serializable
    data class AnotherEventActive(val activeEventName: String) : StartEventResult

    /** The event has already ended and cannot be started again. */
    @Serializable
    data object AlreadyEnded : StartEventResult

    /** The event has no marked blocks yet. */
    @Serializable
    data object NoMarkers : StartEventResult

    /** The event has no loot package with at least one item. */
    @Serializable
    data object NoPackages : StartEventResult

    /**
     * Too few filled packages, because a player receives each package only once: a `COLLECT` event needs
     * one per marker, a `DAILY` event with an end date one per event day.
     *
     * @param filled packages with at least one item
     * @param needed packages required
     */
    @Serializable
    data class NotEnoughPackages(val filled: Int, val needed: Int) : StartEventResult

    /** No event with the given id exists. */
    @Serializable
    data object NotFound : StartEventResult
}
