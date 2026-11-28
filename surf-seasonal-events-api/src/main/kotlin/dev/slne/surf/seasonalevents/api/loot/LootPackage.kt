package dev.slne.surf.seasonalevents.api.loot

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.LocalDate

/**
 * A reward a player can receive once per event. Two packages are equal when they have the same [id],
 * because [items] is a byte array without value equality.
 */
@Serializable
class LootPackage(
    val id: ULong,
    val eventId: ULong,
    val name: String,
    /** The items, serialized with `ItemStack.serializeItemsAsBytes`; opaque outside the Paper plugin. */
    val items: ByteArray,
    /** In [dev.slne.surf.seasonalevents.api.event.EventMode.DAILY], the day this package is given on; null for random packages. */
    val fixedDay: @Contextual LocalDate?,
    /**
     * Whether the package is the reward of the event's NPC instead of being found on the map. The NPC hands
     * it out from [fixedDay] on to players who found every pinned marker of the days before.
     */
    val npcReward: Boolean = false,
) {
    override fun equals(other: Any?) = other is LootPackage && other.id == id
    override fun hashCode() = id.hashCode()
}
