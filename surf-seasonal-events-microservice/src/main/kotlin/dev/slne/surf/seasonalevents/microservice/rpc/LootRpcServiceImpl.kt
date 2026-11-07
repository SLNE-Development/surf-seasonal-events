package dev.slne.surf.seasonalevents.microservice.rpc

import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import dev.slne.surf.seasonalevents.api.event.EventMode
import dev.slne.surf.seasonalevents.api.loot.LootPackage
import dev.slne.surf.seasonalevents.api.result.UpdateLootResult
import dev.slne.surf.seasonalevents.core.rpc.LootRpcService
import dev.slne.surf.seasonalevents.microservice.db.repository.EventRepository
import dev.slne.surf.seasonalevents.microservice.db.repository.LootRepository
import java.time.LocalDate

/** Microservice side of [LootRpcService], backed by [LootRepository]. */
object LootRpcServiceImpl : LootRpcService {

    override suspend fun findByEvent(eventId: ULong): List<LootPackage> = LootRepository.findByEvent(eventId)

    override suspend fun create(eventId: ULong, name: String): UpdateLootResult = suspendTransaction {
        EventRepository.findById(eventId) ?: return@suspendTransaction UpdateLootResult.NotFound
        UpdateLootResult.Success(LootRepository.insert(eventId, name))
    }

    override suspend fun save(packageId: ULong, items: ByteArray): UpdateLootResult = suspendTransaction {
        val updated = LootRepository.updateItems(packageId, items)
            ?: return@suspendTransaction UpdateLootResult.NotFound
        UpdateLootResult.Success(updated)
    }

    override suspend fun setFixedDay(packageId: ULong, day: LocalDate?): UpdateLootResult = suspendTransaction {
        val lootPackage = LootRepository.findById(packageId) ?: return@suspendTransaction UpdateLootResult.NotFound
        val event = EventRepository.findById(lootPackage.eventId) ?: return@suspendTransaction UpdateLootResult.NotFound
        if (event.mode != EventMode.DAILY) return@suspendTransaction UpdateLootResult.NotDailyEvent

        if (day != null && LootRepository.isFixedDayTaken(event.id, day, exceptId = packageId)) {
            return@suspendTransaction UpdateLootResult.FixedDayTaken
        }

        val updated = LootRepository.updateFixedDay(packageId, day)
            ?: return@suspendTransaction UpdateLootResult.NotFound
        UpdateLootResult.Success(updated)
    }

    override suspend fun delete(packageId: ULong): Boolean = LootRepository.delete(packageId)
}
