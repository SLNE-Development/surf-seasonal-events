package dev.slne.surf.seasonalevents.microservice.db.table

import dev.slne.surf.database.columns.nativeUuid
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.ReferenceOption
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.Table
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.javatime.date

/**
 * The marker each player gets per day in a `DAILY` event. Created on the player's first view of the day;
 * removed together with its marker when that is unmarked.
 */
object DailyAssignmentsTable : Table("daily_assignments") {
    val eventId = reference("event_id", SeasonalEventsTable, onDelete = ReferenceOption.CASCADE)
    val player = nativeUuid("player")

    /** Calendar day in Europe/Berlin. */
    val day = date("day")
    val markerId = reference("marker_id", EventMarkersTable, onDelete = ReferenceOption.CASCADE).index()

    override val primaryKey = PrimaryKey(eventId, player, day)
}
