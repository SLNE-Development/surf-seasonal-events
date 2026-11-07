package dev.slne.surf.seasonalevents.microservice.db.table

import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.ReferenceOption
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.javatime.date
import dev.slne.surf.database.table.AuditableLongIdTable

/** Loot packages of an event. Each collection hands out one of them. */
object LootPackagesTable : AuditableLongIdTable("loot_packages") {
    val eventId = reference("event_id", SeasonalEventsTable, onDelete = ReferenceOption.CASCADE).index()
    val name = varchar("name", 64)

    /** Items serialized with `ItemStack.serializeItemsAsBytes`; opaque to the microservice. */
    val items = blob("items")

    /** Day on which the package is handed out in a `DAILY` event; null for random packages. */
    val fixedDay = date("fixed_day").nullable()

    init {
        // NULL does not count as a duplicate, so any number of random packages is allowed.
        uniqueIndex("loot_packages_fixed_day_uq", eventId, fixedDay)
    }
}
