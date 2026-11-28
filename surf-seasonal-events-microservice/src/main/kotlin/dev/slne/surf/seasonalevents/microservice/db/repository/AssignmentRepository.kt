package dev.slne.surf.seasonalevents.microservice.db.repository

import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.and
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.eq
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.deleteWhere
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.insertIgnore
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.selectAll
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import dev.slne.surf.seasonalevents.microservice.db.table.DailyAssignmentsTable
import kotlinx.coroutines.flow.singleOrNull
import java.time.LocalDate
import java.util.UUID

/** Database access for [DailyAssignmentsTable]. Joins an outer transaction if one is open. */
object AssignmentRepository {

    /** Returns the id of the marker assigned to [player] on [day], or null if there is none. */
    suspend fun find(eventId: ULong, player: UUID, day: LocalDate): ULong? = suspendTransaction {
        DailyAssignmentsTable.selectAll()
            .where {
                (DailyAssignmentsTable.eventId eq eventId) and
                    (DailyAssignmentsTable.player eq player) and
                    (DailyAssignmentsTable.day eq day)
            }
            .singleOrNull()
            ?.get(DailyAssignmentsTable.markerId)
            ?.value
    }

    /** Removes the assignment of [player] for [day], e.g. because its marker left the random pool. */
    suspend fun delete(eventId: ULong, player: UUID, day: LocalDate) {
        suspendTransaction {
            DailyAssignmentsTable.deleteWhere {
                (DailyAssignmentsTable.eventId eq eventId) and
                    (DailyAssignmentsTable.player eq player) and
                    (DailyAssignmentsTable.day eq day)
            }
        }
    }

    /**
     * Assigns [markerId] to [player] for [day]. Does nothing if the player already has an assignment
     * for that day, so two parallel first views cannot fail; read it back with [find].
     */
    suspend fun insert(eventId: ULong, player: UUID, day: LocalDate, markerId: ULong) {
        suspendTransaction {
            DailyAssignmentsTable.insertIgnore {
                it[DailyAssignmentsTable.eventId] = eventId
                it[DailyAssignmentsTable.player] = player
                it[DailyAssignmentsTable.day] = day
                it[DailyAssignmentsTable.markerId] = markerId
            }
        }
    }
}
