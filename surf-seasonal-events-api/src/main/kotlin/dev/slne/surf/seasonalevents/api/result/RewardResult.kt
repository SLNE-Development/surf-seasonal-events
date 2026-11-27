package dev.slne.surf.seasonalevents.api.result

import dev.slne.surf.seasonalevents.api.loot.LootPackage
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.LocalDate

/** Outcome of previewing or claiming the NPC reward; only [Granted] means items may be handed out. */
@Serializable
sealed interface RewardResult {
    /** The reward can be claimed; [items] is the serialized content of package [packageId]. */
    @Serializable
    class Preview(val packageId: ULong, val items: ByteArray) : RewardResult {
        override fun equals(other: Any?) =
            other is Preview && other.packageId == packageId && other.items.contentEquals(items)

        override fun hashCode() = 31 * packageId.hashCode() + items.contentHashCode()
    }

    /** The claim is booked; the caller gives the items of [lootPackage]. */
    @Serializable
    data class Granted(val lootPackage: LootPackage) : RewardResult

    /** The reward is handed out from [day] on, once the [missing] earlier markers are found. */
    @Serializable
    data class NotYet(val day: @Contextual LocalDate, val missing: Int) : RewardResult

    /** The player already received the reward. */
    @Serializable
    data object AlreadyClaimed : RewardResult

    /** The active event has no NPC reward. */
    @Serializable
    data object NoReward : RewardResult

    /** No event is currently active. */
    @Serializable
    data object NoActiveEvent : RewardResult

    /** The previewed package changed meanwhile; preview again. */
    @Serializable
    data object PackageUnavailable : RewardResult
}
