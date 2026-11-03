package dev.slne.surf.seasonalevents.db

import dev.slne.surf.database.DatabaseApi
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.SchemaUtils
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import dev.slne.surf.seasonalevents.db.dailygift.PlayerDailyGiftTable
import java.nio.file.Path

object DatabaseLoader {
    lateinit var databaseApi: DatabaseApi

    fun connect(dataPath: Path) {
        databaseApi = DatabaseApi.create(dataPath)
    }

    suspend fun createTables() {
        suspendTransaction {
            SchemaUtils.create(PlayerDailyGiftTable)
        }
    }

    fun disconnect() {
        databaseApi.shutdown()
    }
}