package dev.slne.surf.seasonalevents.microservice

import com.google.auto.service.AutoService
import dev.slne.surf.database.DatabaseApi
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.SchemaUtils
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import dev.slne.surf.microservice.api.microservice.Microservice
import dev.slne.surf.rabbitmq.api.ServerRabbitMQApi
import dev.slne.surf.seasonalevents.core.SeasonalEventsSerializerModule
import dev.slne.surf.seasonalevents.core.rpc.EventRpcService
import dev.slne.surf.seasonalevents.core.rpc.LootRpcService
import dev.slne.surf.seasonalevents.core.rpc.MarkerRpcService
import dev.slne.surf.seasonalevents.core.rpc.ProgressRpcService
import dev.slne.surf.seasonalevents.microservice.db.table.CollectionsTable
import dev.slne.surf.seasonalevents.microservice.db.table.DailyAssignmentsTable
import dev.slne.surf.seasonalevents.microservice.db.table.EventMarkersTable
import dev.slne.surf.seasonalevents.microservice.db.table.LootPackagesTable
import dev.slne.surf.seasonalevents.microservice.db.table.SeasonalEventsTable
import dev.slne.surf.seasonalevents.microservice.logic.SeasonalClock
import dev.slne.surf.seasonalevents.microservice.rpc.EventRpcServiceImpl
import dev.slne.surf.seasonalevents.microservice.rpc.LootRpcServiceImpl
import dev.slne.surf.seasonalevents.microservice.rpc.MarkerRpcServiceImpl
import dev.slne.surf.seasonalevents.microservice.rpc.ProgressRpcServiceImpl
import kotlin.io.path.Path

/**
 * Entry point of the seasonal events microservice. It owns the database and answers the RPC calls
 * of the Paper plugins via RabbitMQ. Exactly one instance may run, because write locks are in memory.
 */
@AutoService(Microservice::class)
class SeasonalEventsMicroservice : Microservice() {
    override val dataPath = Path("config")
    val databaseApi = DatabaseApi.create(dataPath)
    val rabbitApi = ServerRabbitMQApi.create("surf-seasonal-events", dataPath, SeasonalEventsSerializerModule.module)

    override suspend fun onBootstrap(args: List<String>) {
        createTables()

        val clock = SeasonalClock()
        val events = EventRpcServiceImpl(clock)
        rabbitApi.registerRpcService<EventRpcService>(events)
        rabbitApi.registerRpcService<MarkerRpcService>(MarkerRpcServiceImpl)
        rabbitApi.registerRpcService<LootRpcService>(LootRpcServiceImpl)
        rabbitApi.registerRpcService<ProgressRpcService>(ProgressRpcServiceImpl(events, clock))
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
