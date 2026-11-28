package dev.slne.surf.seasonalevents.microservice.db.repository

import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.and
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.eq
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.insertIgnore
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.select
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.selectAll
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import dev.slne.surf.seasonalevents.microservice.db.table.CollectionsTable
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.toSet
import java.time.LocalDate
import java.util.UUID

/** Database access for [CollectionsTable]. Joins an outer transaction if one is open. */
object CollectionRepository {

    /** The dedup keys of every collection of [player] in the event; tells which days and rewards are done. */
    suspend fun dedupKeys(eventId: ULong, player: UUID): Set<String> = suspendTransaction {
        CollectionsTable.select(CollectionsTable.dedupKey)
            .where { (CollectionsTable.eventId eq eventId) and (CollectionsTable.player eq player) }
            .map { it[CollectionsTable.dedupKey] }
            .toSet()
    }

    /** Ids of the packages [player] received in the event; deleted packages are left out. */
    suspend fun receivedPackageIds(eventId: ULong, player: UUID): Set<ULong> = suspendTransaction {
        CollectionsTable.select(CollectionsTable.packageId)
            .where { (CollectionsTable.eventId eq eventId) and (CollectionsTable.player eq player) }
            .mapNotNull { it[CollectionsTable.packageId]?.value }
            .toSet()
    }

    /** Ids of the markers [player] collected in the event; unmarked markers are left out. */
    suspend fun collectedMarkerIds(eventId: ULong, player: UUID): Set<ULong> = suspendTransaction {
        CollectionsTable.select(CollectionsTable.markerId)
            .where { (CollectionsTable.eventId eq eventId) and (CollectionsTable.player eq player) }
            .mapNotNull { it[CollectionsTable.markerId]?.value }
            .toSet()
    }

    /** Whether any player already received package [packageId]. */
    suspend fun wasHandedOut(packageId: ULong): Boolean = suspendTransaction {
        CollectionsTable.select(CollectionsTable.id)
            .where { CollectionsTable.packageId eq packageId }
            .limit(1)
            .firstOrNull() != null
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
     * @param markerId the collected marker; null for the NPC reward, which is not found on the map
     * @return false if a collection with the same [dedupKey] already exists
     */
    suspend fun insert(
        eventId: ULong,
        player: UUID,
        markerId: ULong?,
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
