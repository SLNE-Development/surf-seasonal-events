package dev.slne.surf.seasonalevents.microservice.db.table

import dev.slne.surf.database.columns.nativeUuid
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.ReferenceOption
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.javatime.date
import dev.slne.surf.database.table.AuditableLongIdTable

/** Every collected marker. `created_at` is the moment of collection. */
object CollectionsTable : AuditableLongIdTable("collections") {
    val eventId = reference("event_id", SeasonalEventsTable, onDelete = ReferenceOption.CASCADE)
    val player = nativeUuid("player")

    /** Kept as null when the marker is unmarked, so the collection still counts. */
    val markerId = optReference("marker_id", EventMarkersTable, onDelete = ReferenceOption.SET_NULL)

    /** Kept as null when the package is deleted. */
    val packageId = optReference("package_id", LootPackagesTable, onDelete = ReferenceOption.SET_NULL)

    /** Calendar day in Europe/Berlin, used for the daily limit. */
    val day = date("day")

    /**
     * Prevents collecting twice: `D:<day>` in a `DAILY` event (once per day),
     * `M:<markerId>` in a `COLLECT` event (once per marker).
     */
    val dedupKey = varchar("dedup_key", 32)

    init {
        uniqueIndex("collections_dedup_uq", eventId, player, dedupKey)
    }
}
