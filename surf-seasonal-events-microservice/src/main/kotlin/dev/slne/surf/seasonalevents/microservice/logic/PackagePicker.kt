package dev.slne.surf.seasonalevents.microservice.logic

import dev.slne.surf.seasonalevents.api.event.EventMode
import dev.slne.surf.seasonalevents.api.loot.LootPackage
import java.time.LocalDate
import kotlin.random.Random

/** Picks the loot package a player receives for a collection. */
object PackagePicker {

    /**
     * Picks a package the player has not received yet.
     *
     * In a `DAILY` event, the package fixed to [today] wins. Otherwise a random package without a fixed day
     * is chosen; packages fixed to other days are kept for their day. In a `COLLECT` event, fixed days are
     * ignored and any package not yet received may be chosen.
     *
     * @param receivedIds ids of the packages the player already received in this event
     * @return null if nothing is left for the player
     */
    fun pick(
        mode: EventMode,
        packages: List<LootPackage>,
        receivedIds: Set<ULong>,
        today: LocalDate,
        random: Random,
    ): LootPackage? {
        val open = packages.filter { it.id !in receivedIds }

        return when (mode) {
            EventMode.COLLECT -> open.randomOrNull(random)
            EventMode.DAILY -> open.firstOrNull { it.fixedDay == today }
                ?: open.filter { it.fixedDay == null }.randomOrNull(random)
        }
    }
}
