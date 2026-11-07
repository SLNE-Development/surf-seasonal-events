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

    /** The event has ended; its packages can no longer be changed. */
    @Serializable
    data object EventEnded : UpdateLootResult

    /**
     * Emptying the package would leave the running event with fewer filled packages than it needs
     * ([StartEventResult.NotEnoughPackages] explains the rule); nothing was changed.
     */
    @Serializable
    data class NotEnoughPackages(val filled: Int, val needed: Int) : UpdateLootResult

    /** Someone else saved the package since it was opened; nothing was changed. */
    @Serializable
    data object Conflict : UpdateLootResult
}
