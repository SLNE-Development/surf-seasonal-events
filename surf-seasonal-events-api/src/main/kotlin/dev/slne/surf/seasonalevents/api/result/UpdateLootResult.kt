package dev.slne.surf.seasonalevents.api.result

import dev.slne.surf.seasonalevents.api.loot.LootPackage
import kotlinx.serialization.Serializable

/** Outcome of creating or changing a loot package. */
@Serializable
sealed interface UpdateLootResult {
    /** The package was saved; contains its new state. */
    @Serializable
    data class Success(val lootPackage: LootPackage) : UpdateLootResult

    /** The package or its event does not exist. */
    @Serializable
    data object NotFound : UpdateLootResult

    /** Another package of this event already has this fixed day. */
    @Serializable
    data object FixedDayTaken : UpdateLootResult

    /** Fixed days only exist in DAILY events. */
    @Serializable
    data object NotDailyEvent : UpdateLootResult
}
