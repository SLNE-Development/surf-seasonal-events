package dev.slne.surf.seasonalevents.microservice.db.repository

import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.ResultRow
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.and
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.eq
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.neq
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.statements.api.ExposedBlob
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.deleteWhere
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.insertReturning
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.selectAll
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.update
import dev.slne.surf.seasonalevents.api.loot.LootPackage
import dev.slne.surf.seasonalevents.microservice.db.table.LootPackagesTable
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.toList
import java.time.LocalDate

/** Database access for [LootPackagesTable]. Joins an outer transaction if one is open. */
object LootRepository {

    private fun ResultRow.toPackage() = LootPackage(
        id = this[LootPackagesTable.id].value,
        eventId = this[LootPackagesTable.eventId].value,
        name = this[LootPackagesTable.name],
        items = this[LootPackagesTable.items].bytes,
        fixedDay = this[LootPackagesTable.fixedDay],
    )

    /** Inserts an empty package without a fixed day. */
    suspend fun insert(eventId: ULong, name: String): LootPackage = suspendTransaction {
        LootPackagesTable.insertReturning {
            it[LootPackagesTable.eventId] = eventId
            it[LootPackagesTable.name] = name
            it[items] = ExposedBlob(ByteArray(0))
        }.single().toPackage()
    }

    suspend fun findById(id: ULong): LootPackage? = suspendTransaction {
        LootPackagesTable.selectAll()
            .where { LootPackagesTable.id eq id }
            .singleOrNull()
            ?.toPackage()
    }

    suspend fun findByEvent(eventId: ULong): List<LootPackage> = suspendTransaction {
        LootPackagesTable.selectAll()
            .where { LootPackagesTable.eventId eq eventId }
            .map { it.toPackage() }
            .toList()
    }

    /** Returns whether a package of [eventId] other than [exceptId] already has [day] as its fixed day. */
    suspend fun isFixedDayTaken(eventId: ULong, day: LocalDate, exceptId: ULong): Boolean = suspendTransaction {
        LootPackagesTable.selectAll()
            .where {
                (LootPackagesTable.eventId eq eventId) and
                    (LootPackagesTable.fixedDay eq day) and
                    (LootPackagesTable.id neq exceptId)
            }
            .count() > 0
    }

    suspend fun updateItems(id: ULong, items: ByteArray): LootPackage? = suspendTransaction {
        LootPackagesTable.update({ LootPackagesTable.id eq id }) {
            it[LootPackagesTable.items] = ExposedBlob(items)
        }
        findById(id)
    }

    suspend fun updateFixedDay(id: ULong, day: LocalDate?): LootPackage? = suspendTransaction {
        LootPackagesTable.update({ LootPackagesTable.id eq id }) {
            it[fixedDay] = day
        }
        findById(id)
    }

    /** Deletes a package; collections that handed it out keep a null reference. */
    suspend fun delete(id: ULong): Boolean = suspendTransaction {
        LootPackagesTable.deleteWhere { LootPackagesTable.id eq id } > 0
    }
}
