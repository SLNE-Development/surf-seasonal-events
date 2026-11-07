package dev.slne.surf.seasonalevents.microservice.db.repository

import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.ResultRow
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.eq
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.deleteWhere
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.insertReturning
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.selectAll
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.update
import dev.slne.surf.seasonalevents.api.event.EventMode
import dev.slne.surf.seasonalevents.api.event.EventState
import dev.slne.surf.seasonalevents.api.event.SeasonalEvent
import dev.slne.surf.seasonalevents.microservice.db.table.EventMarkersTable
import dev.slne.surf.seasonalevents.microservice.db.table.LootPackagesTable
import dev.slne.surf.seasonalevents.microservice.db.table.SeasonalEventsTable
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.toList
import java.time.Instant

/** Database access for [SeasonalEventsTable]. Joins an outer transaction if one is open. */
object EventRepository {

    private fun ResultRow.toEvent() = SeasonalEvent(
        id = this[SeasonalEventsTable.id].value,
        name = this[SeasonalEventsTable.name],
        displayName = this[SeasonalEventsTable.displayName],
        mode = this[SeasonalEventsTable.mode],
        state = this[SeasonalEventsTable.state],
        markerLimit = this[SeasonalEventsTable.markerLimit],
        dailyLimit = this[SeasonalEventsTable.dailyLimit],
        endsAt = this[SeasonalEventsTable.endsAt],
        startedAt = this[SeasonalEventsTable.startedAt],
        createdAt = this[SeasonalEventsTable.createdAt].toInstant(),
    )

    suspend fun findById(id: ULong): SeasonalEvent? = suspendTransaction {
        SeasonalEventsTable.selectAll()
            .where { SeasonalEventsTable.id eq id }
            .singleOrNull()
            ?.toEvent()
    }

    suspend fun findByName(name: String): SeasonalEvent? = suspendTransaction {
        SeasonalEventsTable.selectAll()
            .where { SeasonalEventsTable.name eq name }
            .singleOrNull()
            ?.toEvent()
    }

    /** Returns the event in state `ACTIVE`, regardless of whether its end has passed. */
    suspend fun findActive(): SeasonalEvent? = suspendTransaction {
        SeasonalEventsTable.selectAll()
            .where { SeasonalEventsTable.state eq EventState.ACTIVE }
            .singleOrNull()
            ?.toEvent()
    }

    /** Inserts a new `DRAFT` event; the display name starts as the technical name. */
    suspend fun insert(name: String, mode: EventMode, markerLimit: Int, dailyLimit: Int): SeasonalEvent =
        suspendTransaction {
            SeasonalEventsTable.insertReturning {
                it[SeasonalEventsTable.name] = name
                it[SeasonalEventsTable.displayName] = name
                it[SeasonalEventsTable.mode] = mode
                it[SeasonalEventsTable.markerLimit] = markerLimit
                it[SeasonalEventsTable.dailyLimit] = dailyLimit
            }.single().toEvent()
        }

    /** Sets state and dates of an event and returns it updated, or null if it does not exist. */
    suspend fun updateState(id: ULong, state: EventState, startedAt: Instant?, endsAt: Instant?): SeasonalEvent? =
        suspendTransaction {
            SeasonalEventsTable.update({ SeasonalEventsTable.id eq id }) {
                it[SeasonalEventsTable.state] = state
                it[SeasonalEventsTable.startedAt] = startedAt
                it[SeasonalEventsTable.endsAt] = endsAt
            }
            findById(id)
        }

    suspend fun countMarkers(eventId: ULong): Int = suspendTransaction {
        EventMarkersTable.selectAll().where { EventMarkersTable.eventId eq eventId }.count().toInt()
    }

    suspend fun countPackages(eventId: ULong): Int = suspendTransaction {
        LootPackagesTable.selectAll().where { LootPackagesTable.eventId eq eventId }.count().toInt()
    }

    suspend fun findAll(): List<SeasonalEvent> = suspendTransaction {
        SeasonalEventsTable.selectAll()
            .map { it.toEvent() }
            .toList()
    }

    /** Sets the display name and returns the event updated, or null if it does not exist. */
    suspend fun updateDisplayName(id: ULong, displayName: String): SeasonalEvent? = suspendTransaction {
        SeasonalEventsTable.update({ SeasonalEventsTable.id eq id }) {
            it[SeasonalEventsTable.displayName] = displayName
        }
        findById(id)
    }

    /** Sets or clears the end and returns the event updated, or null if it does not exist. */
    suspend fun updateEndsAt(id: ULong, endsAt: Instant?): SeasonalEvent? = suspendTransaction {
        SeasonalEventsTable.update({ SeasonalEventsTable.id eq id }) {
            it[SeasonalEventsTable.endsAt] = endsAt
        }
        findById(id)
    }

    /** Deletes the event; dependent rows go with it through `ON DELETE CASCADE`. */
    suspend fun delete(id: ULong): Boolean = suspendTransaction {
        SeasonalEventsTable.deleteWhere { SeasonalEventsTable.id eq id } > 0
    }
}
