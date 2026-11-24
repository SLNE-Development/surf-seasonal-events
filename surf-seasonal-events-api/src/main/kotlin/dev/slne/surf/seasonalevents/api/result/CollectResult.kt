package dev.slne.surf.seasonalevents.api.result

import dev.slne.surf.seasonalevents.api.loot.LootPackage
import kotlinx.serialization.Serializable

/** Outcome of collecting a marker; only [Success] means items may be handed out. */
@Serializable
sealed interface CollectResult {
    /** The collection is booked; the caller gives the items of [lootPackage]. */
    @Serializable
    data class Success(val lootPackage: LootPackage) : CollectResult

    /** The marker belongs to another player's daily assignment. */
    @Serializable
    data object NotYours : CollectResult

    /** The player already collected this marker (COLLECT) or today's marker (DAILY). */
    @Serializable
    data object AlreadyCollected : CollectResult

    /** The player reached the event's daily limit. */
    @Serializable
    data object DailyLimitReached : CollectResult

    /** The player already received every package. */
    @Serializable
    data object NothingLeft : CollectResult

    /** No event is currently active. */
    @Serializable
    data object NoActiveEvent : CollectResult

    /** The previewed package can no longer be given (deleted or received meanwhile); preview again. */
    @Serializable
    data object PackageUnavailable : CollectResult
}
