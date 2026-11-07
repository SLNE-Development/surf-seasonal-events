package dev.slne.surf.seasonalevents.core.client.rpc

import dev.slne.surf.seasonalevents.core.client.rabbit.rabbitApi
import dev.slne.surf.seasonalevents.core.rpc.EventRpcService
import dev.slne.surf.seasonalevents.core.rpc.LootRpcService
import dev.slne.surf.seasonalevents.core.rpc.MarkerRpcService
import dev.slne.surf.seasonalevents.core.rpc.ProgressRpcService

// Proxies to the microservice. Created on first use, so only after RabbitService.connect.
// Wrap calls in rpcCall to handle an unreachable microservice.

val eventRpc by lazy { rabbitApi.createRpcService<EventRpcService>() }
val markerRpc by lazy { rabbitApi.createRpcService<MarkerRpcService>() }
val lootRpc by lazy { rabbitApi.createRpcService<LootRpcService>() }
val progressRpc by lazy { rabbitApi.createRpcService<ProgressRpcService>() }
