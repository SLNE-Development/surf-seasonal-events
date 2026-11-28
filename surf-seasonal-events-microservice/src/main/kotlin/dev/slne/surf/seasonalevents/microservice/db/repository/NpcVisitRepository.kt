package dev.slne.surf.seasonalevents.microservice.db.repository

import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.and
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.eq
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.insertIgnore
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.select
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import dev.slne.surf.seasonalevents.microservice.db.table.NpcVisitsTable
import kotlinx.coroutines.flow.firstOrNull
import java.time.Instant
import java.util.UUID

/** Database access for [NpcVisitsTable]. Joins an outer transaction if one is open. */
object NpcVisitRepository {

    /** Whether [player] talked to the NPC of [eventId] at least once. */
    suspend fun hasVisited(eventId: ULong, player: UUID): Boolean = suspendTransaction {
        NpcVisitsTable.select(NpcVisitsTable.player)
            .where { (NpcVisitsTable.eventId eq eventId) and (NpcVisitsTable.player eq player) }
            .limit(1)
            .firstOrNull() != null
    }

    /**
     * Records a talk of [player] to the NPC of [eventId].
     *
     * @return true if it was the player's first talk in this event
     */
    suspend fun recordVisit(eventId: ULong, player: UUID, at: Instant): Boolean = suspendTransaction {
        NpcVisitsTable.insertIgnore {
            it[NpcVisitsTable.eventId] = eventId
            it[NpcVisitsTable.player] = player
            it[firstVisitAt] = at
        }.insertedCount > 0
    }
}
