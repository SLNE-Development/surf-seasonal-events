package dev.slne.surf.seasonalevents.api.event

import kotlinx.serialization.Serializable

/** Lifecycle of a seasonal event; it only moves forward from [DRAFT] to [ACTIVE] to [ENDED]. */
@Serializable
enum class EventState {
    /** Being prepared: staff can mark blocks and fill loot packages, players see nothing. */
    DRAFT,

    /** Running: players see and collect markers. At most one event is active at a time. */
    ACTIVE,

    /** Finished for good: nothing is shown anymore and the event cannot be started again. */
    ENDED
}
