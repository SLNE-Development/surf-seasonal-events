package dev.slne.surf.seasonalevents.db.dailygift

import dev.slne.surf.database.columns.nativeUuid
import dev.slne.surf.database.columns.time.offsetDateTime
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.Table

object PlayerDailyGiftTable : Table("player_daily_gift") {

    val playerId = nativeUuid("player_id")
    val giftId = nativeUuid("gift_id")

    val world = nativeUuid("world")
    val x = double("x")
    val y = double("y")
    val z = double("z")

    val date = offsetDateTime("date")
    val claimed = bool("claimed").default(false)

    override val primaryKey = PrimaryKey(playerId, date)
}