package dev.slne.surf.seasonalevents.microservice.rpc

import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import dev.slne.surf.seasonalevents.api.event.EventMode
import dev.slne.surf.seasonalevents.api.loot.LootPackage
import dev.slne.surf.seasonalevents.api.result.DeleteLootResult
import dev.slne.surf.seasonalevents.api.result.UpdateLootResult
import dev.slne.surf.seasonalevents.core.rpc.LootRpcService
import dev.slne.surf.seasonalevents.microservice.db.repository.EventRepository
import dev.slne.surf.seasonalevents.microservice.db.repository.LootRepository
import dev.slne.surf.seasonalevents.microservice.logic.LootRules
import java.time.LocalDate

/**
 * Microservice side of [LootRpcService]: combines [LootRules] with [LootRepository].
 * Every write runs under [StaffWriteLock].
 */
object LootRpcServiceImpl : LootRpcService {

    override suspend fun findByEvent(eventId: ULong): List<LootPackage> = LootRepository.findByEvent(eventId)

    override suspend fun create(eventId: ULong, name: String): UpdateLootResult = StaffWriteLock.withLock {
        suspendTransaction {
            val event = EventRepository.findById(eventId) ?: return@suspendTransaction UpdateLootResult.NotFound
            LootRules.checkChange(event)?.let { return@suspendTransaction it }

            UpdateLootResult.Success(LootRepository.insert(eventId, name))
        }
    }

    override suspend fun save(packageId: ULong, expectedItems: ByteArray, items: ByteArray): UpdateLootResult =
        StaffWriteLock.withLock {
            suspendTransaction {
                val current = LootRepository.findById(packageId) ?: return@suspendTransaction UpdateLootResult.NotFound
                val event = EventRepository.findById(current.eventId) ?: return@suspendTransaction UpdateLootResult.NotFound
                LootRules.checkSave(event, current.items, expectedItems)?.let { return@suspendTransaction it }
                val emptiesPackage = current.items.isNotEmpty() && items.isEmpty()
                if (emptiesPackage) {
                    PackageGuard.shortageAfter(event, removedPackages = 1)?.let {
                        return@suspendTransaction UpdateLootResult.NotEnoughPackages(it.filled, it.needed)
                    }
                }

                val updated = LootRepository.updateItems(packageId, items)
                    ?: return@suspendTransaction UpdateLootResult.NotFound
                UpdateLootResult.Success(updated)
            }
        }

    override suspend fun setFixedDay(packageId: ULong, day: LocalDate?): UpdateLootResult = StaffWriteLock.withLock {
        suspendTransaction {
            val lootPackage = LootRepository.findById(packageId) ?: return@suspendTransaction UpdateLootResult.NotFound
            val event = EventRepository.findById(lootPackage.eventId) ?: return@suspendTransaction UpdateLootResult.NotFound
            LootRules.checkChange(event)?.let { return@suspendTransaction it }
            if (event.mode != EventMode.DAILY) return@suspendTransaction UpdateLootResult.NotDailyEvent

            if (day != null && LootRepository.isFixedDayTaken(event.id, day, exceptId = packageId)) {
                return@suspendTransaction UpdateLootResult.FixedDayTaken
            }

            val updated = LootRepository.updateFixedDay(packageId, day)
                ?: return@suspendTransaction UpdateLootResult.NotFound
            UpdateLootResult.Success(updated)
        }
    }

    override suspend fun delete(packageId: ULong): DeleteLootResult = StaffWriteLock.withLock {
        suspendTransaction {
            val lootPackage = LootRepository.findById(packageId) ?: return@suspendTransaction DeleteLootResult.NotFound
            val event = EventRepository.findById(lootPackage.eventId) ?: return@suspendTransaction DeleteLootResult.NotFound
            if (LootRules.checkChange(event) != null) return@suspendTransaction DeleteLootResult.EventEnded
            if (lootPackage.items.isNotEmpty()) {
                PackageGuard.shortageAfter(event, removedPackages = 1)?.let {
                    return@suspendTransaction DeleteLootResult.NotEnoughPackages(it.filled, it.needed)
                }
            }

            if (LootRepository.delete(packageId)) DeleteLootResult.Success else DeleteLootResult.NotFound
        }
    }
}
