package dev.slne.surf.seasonalevents.api.event

import kotlinx.serialization.Serializable

/** How players take part in a seasonal event. */
@Serializable
enum class EventMode {
    /** Each player gets one randomly assigned marker per day, like an advent calendar. */
    DAILY,

    /** Each player can find every marker once, optionally limited per day, like an egg hunt. */
    COLLECT
}
