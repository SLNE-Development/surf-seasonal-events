package dev.slne.surf.seasonalevents.core

import kotlinx.serialization.modules.SerializersModule

/**
 * Serializers specific to surf-seasonal-events, added on top of surf-api's defaults
 * (which already cover UUID, Instant and LocalDate).
 */
object SeasonalEventsSerializerModule {
    val module = SerializersModule { }
}
