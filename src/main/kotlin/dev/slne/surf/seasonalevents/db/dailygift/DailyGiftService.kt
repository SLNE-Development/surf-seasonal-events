package dev.slne.surf.seasonalevents.db.dailygift

import dev.slne.surf.surfapi.core.api.util.requiredService
import java.util.*

private val dailyGiftService = requiredService<DailyGiftService>()

interface DailyGiftService {

    suspend fun getOrCreateTodayGift(playerUuid: UUID): DailyGift?
    suspend fun getTodayGift(playerUuid: UUID): DailyGift?
    suspend fun markTodaysAsClaimed(playerUuid: UUID)

    companion object : DailyGiftService by dailyGiftService
}