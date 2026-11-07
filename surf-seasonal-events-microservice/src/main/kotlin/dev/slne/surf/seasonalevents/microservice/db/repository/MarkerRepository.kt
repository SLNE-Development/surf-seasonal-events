package dev.slne.surf.seasonalevents.microservice.db.repository

import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.ResultRow
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.and
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.eq
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.deleteWhere
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.insertReturning
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.selectAll
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import dev.slne.surf.seasonalevents.api.marker.EventMarker
import dev.slne.surf.seasonalevents.api.marker.MarkerPosition
import dev.slne.surf.seasonalevents.microservice.db.table.EventMarkersTable
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.toList
import java.util.UUID

/** Database access for [EventMarkersTable]. Joins an outer transaction if one is open. */
object MarkerRepository {

    private fun ResultRow.toMarker() = EventMarker(
        id = this[EventMarkersTable.id].value,
        eventId = this[EventMarkersTable.eventId].value,
        position = MarkerPosition(
            world = this[EventMarkersTable.world],
            x = this[EventMarkersTable.x],
            y = this[EventMarkersTable.y],
            z = this[EventMarkersTable.z],
        ),
        blockData = this[EventMarkersTable.blockData],
        skullTexture = this[EventMarkersTable.skullTexture],
    )

    suspend fun insert(
        eventId: ULong,
        position: MarkerPosition,
        blockData: String,
        skullTexture: String?,
        by: UUID,
    ): EventMarker = suspendTransaction {
        EventMarkersTable.insertReturning {
            it[EventMarkersTable.eventId] = eventId
            it[world] = position.world
            it[x] = position.x
            it[y] = position.y
            it[z] = position.z
            it[EventMarkersTable.blockData] = blockData
            it[EventMarkersTable.skullTexture] = skullTexture
            it[createdBy] = by
        }.single().toMarker()
    }

    suspend fun findById(id: ULong): EventMarker? = suspendTransaction {
        EventMarkersTable.selectAll()
            .where { EventMarkersTable.id eq id }
            .singleOrNull()
            ?.toMarker()
    }

    /** Returns the marker at [position], regardless of which event it belongs to. */
    suspend fun findByPosition(position: MarkerPosition): EventMarker? = suspendTransaction {
        EventMarkersTable.selectAll()
            .where {
                (EventMarkersTable.world eq position.world) and
                    (EventMarkersTable.x eq position.x) and
                    (EventMarkersTable.y eq position.y) and
                    (EventMarkersTable.z eq position.z)
            }
            .singleOrNull()
            ?.toMarker()
    }

    suspend fun findByEvent(eventId: ULong): List<EventMarker> = suspendTransaction {
        EventMarkersTable.selectAll()
            .where { EventMarkersTable.eventId eq eventId }
            .map { it.toMarker() }
            .toList()
    }

    suspend fun count(eventId: ULong): Int = suspendTransaction {
        EventMarkersTable.selectAll().where { EventMarkersTable.eventId eq eventId }.count().toInt()
    }

    /** Deletes a marker; its assignments go with it, collections keep a null reference. */
    suspend fun delete(markerId: ULong): Boolean = suspendTransaction {
        EventMarkersTable.deleteWhere { EventMarkersTable.id eq markerId } > 0
    }

    /** Deletes all markers of an event and returns them as they were. */
    suspend fun deleteByEvent(eventId: ULong): List<EventMarker> = suspendTransaction {
        val markers = findByEvent(eventId)
        EventMarkersTable.deleteWhere { EventMarkersTable.eventId eq eventId }
        markers
    }
}
