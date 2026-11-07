package dev.slne.surf.seasonalevents.microservice

import com.google.auto.service.AutoService
import dev.slne.surf.database.DatabaseApi
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.SchemaUtils
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import dev.slne.surf.microservice.api.microservice.Microservice
import dev.slne.surf.rabbitmq.api.ServerRabbitMQApi
import dev.slne.surf.seasonalevents.core.SeasonalEventsSerializerModule
import dev.slne.surf.seasonalevents.microservice.db.table.CollectionsTable
import dev.slne.surf.seasonalevents.microservice.db.table.DailyAssignmentsTable
import dev.slne.surf.seasonalevents.microservice.db.table.EventMarkersTable
import dev.slne.surf.seasonalevents.microservice.db.table.LootPackagesTable
import dev.slne.surf.seasonalevents.microservice.db.table.SeasonalEventsTable
import kotlin.io.path.Path

lateinit var seasonalEventsMicroservice: SeasonalEventsMicroservice

/**
 * Entry point of the seasonal events microservice. It owns the database and answers the RPC calls
 * of the Paper plugins via RabbitMQ.
 */
@AutoService(Microservice::class)
class SeasonalEventsMicroservice : Microservice() {
    override val dataPath = Path("config")
    val databaseApi = DatabaseApi.create(dataPath)
    val rabbitApi = ServerRabbitMQApi.create("surf-seasonal-events", dataPath, SeasonalEventsSerializerModule.module)

    init {
        seasonalEventsMicroservice = this
    }

    override suspend fun onBootstrap(args: List<String>) {
        createTables()

        // RPC services are registered here once they are implemented.

        rabbitApi.freezeAndConnect()
    }

    private suspend fun createTables() = suspendTransaction {
        SchemaUtils.create(
            SeasonalEventsTable,
            EventMarkersTable,
            LootPackagesTable,
            DailyAssignmentsTable,
            CollectionsTable,
        )
    }

    override suspend fun onDisable() {
        rabbitApi.disconnect()
        databaseApi.shutdown()
    }
}
