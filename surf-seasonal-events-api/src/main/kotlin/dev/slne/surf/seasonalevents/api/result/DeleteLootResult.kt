package dev.slne.surf.seasonalevents.api.result

import kotlinx.serialization.Serializable

/** Outcome of deleting a loot package. */
@Serializable
sealed interface DeleteLootResult {
    /** The package was deleted; players who received it keep their items. */
    @Serializable
    data object Success : DeleteLootResult

    /** The package does not exist (anymore). */
    @Serializable
    data object NotFound : DeleteLootResult

    /**
     * Deleting the package would leave the running event with fewer filled packages than it needs
     * ([StartEventResult.NotEnoughPackages] explains the rule).
     */
    @Serializable
    data class NotEnoughPackages(val filled: Int, val needed: Int) : DeleteLootResult

    /** The event has ended; its packages can no longer be changed. */
    @Serializable
    data object EventEnded : DeleteLootResult
}
