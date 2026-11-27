package dev.slne.surf.seasonalevents.microservice.db.table

import dev.slne.surf.database.columns.nativeUuid
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.ReferenceOption
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.javatime.date
import dev.slne.surf.database.table.AuditableLongIdTable

/** Blocks marked as event objects. A position belongs to at most one event. */
object EventMarkersTable : AuditableLongIdTable("event_markers") {
    val eventId = reference("event_id", SeasonalEventsTable, onDelete = ReferenceOption.CASCADE).index()
    val world = nativeUuid("world")
    val x = integer("x")
    val y = integer("y")
    val z = integer("z")

    /** The original block, as `BlockData#getAsString()`. */
    val blockData = text("block_data")

    /** Texture property of a player head; null for every other block. */
    val skullTexture = text("skull_texture").nullable()
    val createdBy = nativeUuid("created_by")

    /** Day the marker is the hiding spot for in a `DAILY` event; null for markers of the random pool. */
    val day = date("day").nullable()

    init {
        uniqueIndex("event_markers_position_uq", world, x, y, z)
        // NULL does not count as a duplicate, so any number of random-pool markers is allowed.
        uniqueIndex("event_markers_day_uq", eventId, day)
    }
}
