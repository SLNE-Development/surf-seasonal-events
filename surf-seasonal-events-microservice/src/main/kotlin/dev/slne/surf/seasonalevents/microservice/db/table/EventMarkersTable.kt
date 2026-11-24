package dev.slne.surf.seasonalevents.microservice.db.table

import dev.slne.surf.database.columns.nativeUuid
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.ReferenceOption
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

    init {
        uniqueIndex("event_markers_position_uq", world, x, y, z)
    }
}
