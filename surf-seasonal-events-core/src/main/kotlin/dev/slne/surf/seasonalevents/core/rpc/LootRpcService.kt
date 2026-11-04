package dev.slne.surf.seasonalevents.core.rpc

import dev.slne.surf.rabbitmq.api.rpc.RpcService
import dev.slne.surf.seasonalevents.api.loot.LootPackage
import dev.slne.surf.seasonalevents.api.result.UpdateLootResult
import java.time.LocalDate

/**
 * Manages the loot packages of an event. Each collection hands out one package.
 * Items are opaque bytes to the microservice; only the plugin (de)serializes them.
 */
@RpcService
interface LootRpcService {

    /** Returns all packages of an event, empty if the event does not exist. */
    suspend fun findByEvent(eventId: ULong): List<LootPackage>

    /**
     * Creates an empty package without a fixed day.
     *
     * @return [UpdateLootResult.NotFound] if the event does not exist
     */
    suspend fun create(eventId: ULong, name: String): UpdateLootResult

    /**
     * Replaces the items of a package.
     *
     * @param items the contents, serialized with `ItemStack.serializeItemsAsBytes`
     * @return [UpdateLootResult.NotFound] if the package does not exist
     */
    suspend fun save(packageId: ULong, items: ByteArray): UpdateLootResult

    /**
     * Sets or clears the day on which a package is handed out in a `DAILY` event.
     * On that day the package takes precedence, as long as the player has not received it yet.
     *
     * @param day the day in Europe/Berlin, or null to hand the package out randomly
     * @return [UpdateLootResult.FixedDayTaken] if another package of the event already has this day,
     * [UpdateLootResult.NotDailyEvent] if the event is a `COLLECT` event
     */
    suspend fun setFixedDay(packageId: ULong, day: LocalDate?): UpdateLootResult

    /**
     * Deletes a package. Collections that handed it out are kept without a package reference.
     *
     * @return false if the package does not exist
     */
    suspend fun delete(packageId: ULong): Boolean
}
