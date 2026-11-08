package dev.slne.surf.seasonalevents.microservice.db.repository

import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.and
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.eq
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.insertIgnore
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.selectAll
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import dev.slne.surf.seasonalevents.api.event.EventMode
import dev.slne.surf.seasonalevents.microservice.db.table.CollectionsTable
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.toSet
import java.time.LocalDate
import java.util.UUID

/** Database access for [CollectionsTable]. Joins an outer transaction if one is open. */
object CollectionRepository {

    /**
     * The key that makes a collection unique per player: once per day in a `DAILY` event,
     * once per marker in a `COLLECT` event.
     */
    fun dedupKey(mode: EventMode, day: LocalDate, markerId: ULong): String = when (mode) {
        EventMode.DAILY -> "D:$day"
        EventMode.COLLECT -> "M:$markerId"
    }

    /** Ids of the packages [player] received in the event; deleted packages are left out. */
    suspend fun receivedPackageIds(eventId: ULong, player: UUID): Set<ULong> = suspendTransaction {
        CollectionsTable.selectAll()
            .where { (CollectionsTable.eventId eq eventId) and (CollectionsTable.player eq player) }
            .mapNotNull { it[CollectionsTable.packageId]?.value }
            .toSet()
    }

    /** Ids of the markers [player] collected in the event; unmarked markers are left out. */
    suspend fun collectedMarkerIds(eventId: ULong, player: UUID): Set<ULong> = suspendTransaction {
        CollectionsTable.selectAll()
            .where { (CollectionsTable.eventId eq eventId) and (CollectionsTable.player eq player) }
            .mapNotNull { it[CollectionsTable.markerId]?.value }
            .toSet()
    }

    /** Number of all collections of [player] in the event, including those of unmarked markers. */
    suspend fun count(eventId: ULong, player: UUID): Int = suspendTransaction {
        CollectionsTable.selectAll()
            .where { (CollectionsTable.eventId eq eventId) and (CollectionsTable.player eq player) }
            .count()
            .toInt()
    }

    suspend fun countOnDay(eventId: ULong, player: UUID, day: LocalDate): Int = suspendTransaction {
        CollectionsTable.selectAll()
            .where {
                (CollectionsTable.eventId eq eventId) and
                    (CollectionsTable.player eq player) and
                    (CollectionsTable.day eq day)
            }
            .count()
            .toInt()
    }

    /**
     * Books a collection.
     *
     * @return false if a collection with the same [dedupKey] already exists
     */
    suspend fun insert(
        eventId: ULong,
        player: UUID,
        markerId: ULong,
        packageId: ULong,
        day: LocalDate,
        dedupKey: String,
    ): Boolean = suspendTransaction {
        CollectionsTable.insertIgnore {
            it[CollectionsTable.eventId] = eventId
            it[CollectionsTable.player] = player
            it[CollectionsTable.markerId] = markerId
            it[CollectionsTable.packageId] = packageId
            it[CollectionsTable.day] = day
            it[CollectionsTable.dedupKey] = dedupKey
        }.insertedCount > 0
    }
}
