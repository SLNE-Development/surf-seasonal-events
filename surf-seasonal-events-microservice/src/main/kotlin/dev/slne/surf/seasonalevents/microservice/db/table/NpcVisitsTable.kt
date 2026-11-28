package dev.slne.surf.seasonalevents.microservice.db.table

import dev.slne.surf.database.columns.nativeUuid
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.ReferenceOption
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.Table
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.javatime.timestamp

/** Players who talked to the event's NPC, so its introduction is only shown on the first talk. */
object NpcVisitsTable : Table("npc_visits") {
    val eventId = reference("event_id", SeasonalEventsTable, onDelete = ReferenceOption.CASCADE)
    val player = nativeUuid("player")

    /** Moment of the first talk. */
    val firstVisitAt = timestamp("first_visit_at")

    override val primaryKey = PrimaryKey(eventId, player)
}
