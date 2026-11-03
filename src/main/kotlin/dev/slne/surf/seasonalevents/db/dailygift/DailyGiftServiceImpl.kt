package dev.slne.surf.seasonalevents.db.dailygift

import com.google.auto.service.AutoService
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.and
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.eq
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.greaterEq
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.less
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.insert
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.select
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.selectAll
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.update
import dev.slne.surf.seasonalevents.config.gift.Gift
import dev.slne.surf.seasonalevents.config.gift.GiftService
import dev.slne.surf.seasonalevents.config.location.locationConfig
import dev.slne.surf.surfapi.bukkit.api.extensions.server
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.bukkit.Location
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*

sealed class DailyGiftCreateResult { //TODO: implement
    data class Created(val gift: DailyGift) : DailyGiftCreateResult()
    data class AlreadyExists(val gift: DailyGift) : DailyGiftCreateResult()
    data object NoGiftsAvailable : DailyGiftCreateResult()
    data object Error : DailyGiftCreateResult()
}

@AutoService(DailyGiftService::class)
class DailyGiftServiceImpl : DailyGiftService {

    private fun today(): OffsetDateTime =
        OffsetDateTime.now(ZoneOffset.UTC)
            .withHour(0)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)

    override suspend fun getOrCreateTodayGift(playerUuid: UUID): DailyGift? {

        val existing = getTodayGift(playerUuid)
        if (existing != null) return existing

        val gift = getUncollectedRandomGift(playerUuid) ?: return null
        val location = if (locationConfig.locations.isEmpty()) {
            return null
        } else {
            locationConfig.locations.random().toBukkitLocation()
        }
        val date = today()

        suspendTransaction {
            PlayerDailyGiftTable.insert {
                it[playerId] = playerUuid
                it[giftId] = gift.giftId
                it[world] = location.world!!.uid
                it[x] = location.x
                it[y] = location.y
                it[z] = location.z
                it[PlayerDailyGiftTable.date] = date
                it[claimed] = false
            }
        }

        return DailyGift(
            playerUuid,
            gift.giftId,
            location,
            date,
            false
        )
    }

    override suspend fun getTodayGift(playerUuid: UUID): DailyGift? {
        val startOfToday = today()
        val endOfToday = startOfToday.plusDays(1)

        val row = suspendTransaction {
            PlayerDailyGiftTable
                .selectAll()
                .where {
                    (PlayerDailyGiftTable.playerId eq playerUuid) and
                            (PlayerDailyGiftTable.date greaterEq startOfToday) and
                            (PlayerDailyGiftTable.date less endOfToday)
                }
                .firstOrNull()
        } ?: return null

        val world = server.getWorld(row[PlayerDailyGiftTable.world]) ?: return null

        val location = Location(
            world,
            row[PlayerDailyGiftTable.x],
            row[PlayerDailyGiftTable.y],
            row[PlayerDailyGiftTable.z]
        )

        return DailyGift(
            row[PlayerDailyGiftTable.playerId],
            row[PlayerDailyGiftTable.giftId],
            location,
            row[PlayerDailyGiftTable.date],
            row[PlayerDailyGiftTable.claimed]
        )
    }

    override suspend fun markTodaysAsClaimed(playerUuid: UUID) {
        suspendTransaction {
            PlayerDailyGiftTable.update(
                where = {
                    (PlayerDailyGiftTable.playerId eq playerUuid) and (PlayerDailyGiftTable.date eq today())
                }
            ) {
                it[claimed] = true
            }
        }
    }

    private suspend fun getUncollectedRandomGift(playerUuid: UUID): Gift? {
        val collectedIds = suspendTransaction {
            PlayerDailyGiftTable
                .select(PlayerDailyGiftTable.giftId)
                .where {
                    (PlayerDailyGiftTable.playerId eq playerUuid) and (PlayerDailyGiftTable.claimed eq true)
                }
                .map { it[PlayerDailyGiftTable.giftId] }
                .toList()
        }
        val uncollected = GiftService.gifts.filter { it.giftId !in collectedIds }

        return uncollected.randomOrNull()
    }
}