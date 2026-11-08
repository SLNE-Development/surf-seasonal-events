package dev.slne.surf.seasonalevents.core.client.rabbit

import dev.slne.surf.rabbitmq.api.ClientRabbitMQApi
import dev.slne.surf.seasonalevents.core.SeasonalEventsSerializerModule
import java.nio.file.Path

/** RabbitMQ connection of a plugin to the seasonal events microservice. */
object RabbitService {

    /** Available after [connect]; the RPC proxies in `RpcServices.kt` use it. */
    lateinit var rabbitApi: ClientRabbitMQApi
        private set

    /**
     * Creates the client and connects it. Reads `rabbitmq.yml` from [dataPath],
     * which is created with defaults on the first start.
     */
    suspend fun connect(dataPath: Path) {
        rabbitApi = ClientRabbitMQApi.create(
            "surf-seasonal-events",
            dataPath,
            SeasonalEventsSerializerModule.module,
        )
        rabbitApi.freezeAndConnect()
    }

    suspend fun disconnect() {
        if (::rabbitApi.isInitialized) rabbitApi.disconnect()
    }
}

val rabbitApi get() = RabbitService.rabbitApi
