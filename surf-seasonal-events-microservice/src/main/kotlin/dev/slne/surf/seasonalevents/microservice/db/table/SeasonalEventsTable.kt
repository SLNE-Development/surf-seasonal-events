package dev.slne.surf.seasonalevents.microservice.db.table

import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.javatime.timestamp
import dev.slne.surf.database.table.AuditableLongIdTable
import dev.slne.surf.seasonalevents.api.event.EventMode
import dev.slne.surf.seasonalevents.api.event.EventState

/** All seasonal events. At most one row is `ACTIVE`; the microservice enforces that. */
object SeasonalEventsTable : AuditableLongIdTable("seasonal_events") {
    /** Unique technical name, e.g. `christmas-2026`. */
    val name = varchar("name", 64).uniqueIndex("seasonal_events_name_uq")

    /** MiniMessage name shown to players. */
    val displayName = varchar("display_name", 255)
    val mode = enumerationByName<EventMode>("mode", 16)
    val state = enumerationByName<EventState>("state", 16).default(EventState.DRAFT)
    val markerLimit = integer("marker_limit")

    /** Collections per player and day in a `COLLECT` event; 0 means unlimited. */
    val dailyLimit = integer("daily_limit").default(0)
    val endsAt = timestamp("ends_at").nullable()
    val startedAt = timestamp("started_at").nullable()
}
