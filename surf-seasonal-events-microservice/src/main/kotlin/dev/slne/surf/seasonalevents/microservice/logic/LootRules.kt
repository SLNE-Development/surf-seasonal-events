package dev.slne.surf.seasonalevents.microservice.logic

import dev.slne.surf.seasonalevents.api.event.EventState
import dev.slne.surf.seasonalevents.api.event.SeasonalEvent
import dev.slne.surf.seasonalevents.api.result.UpdateLootResult

/** Rules for changing loot packages. Each check returns null if the change is allowed. */
object LootRules {

    /** Packages of an ended event are kept as they were handed out and can no longer change. */
    fun checkChange(event: SeasonalEvent): UpdateLootResult? =
        if (event.state == EventState.ENDED) UpdateLootResult.EventEnded else null

    /**
     * Checks a save of new contents. Refuses it if the package was saved by someone else since the
     * editor loaded [expectedItems], so that save is not silently overwritten.
     */
    fun checkSave(event: SeasonalEvent, currentItems: ByteArray, expectedItems: ByteArray): UpdateLootResult? {
        checkChange(event)?.let { return it }
        if (!currentItems.contentEquals(expectedItems)) return UpdateLootResult.Conflict
        return null
    }
}
