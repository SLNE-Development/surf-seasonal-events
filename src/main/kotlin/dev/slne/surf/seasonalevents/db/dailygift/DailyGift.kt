package dev.slne.surf.seasonalevents.db.dailygift

import org.bukkit.Location
import java.time.OffsetDateTime
import java.util.UUID

data class DailyGift(
    val playerId: UUID,
    val giftId: UUID,
    val location: Location,
    val date: OffsetDateTime,
    val claimed: Boolean
)