package dev.slne.surf.seasonalevents.api.progress

import dev.slne.surf.seasonalevents.api.event.SeasonalEvent
import dev.slne.surf.seasonalevents.api.marker.EventMarker
import kotlinx.serialization.Serializable

/** Everything a player should currently see as fake blocks in the active event. */
@Serializable
data class PlayerView(
    val event: SeasonalEvent,
    /** Markers visible to this player: today's assignment (DAILY) or all not yet found (COLLECT). */
    val markers: List<EventMarker>,
)
