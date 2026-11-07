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
    ): LootPackage? = candidates(mode, packages, receivedIds, today).randomOrNull(random)

    /**
     * Returns every package [pick] may choose from, following the same rules. Used to check that a
     * previewed package can still be given.
     */
    fun candidates(
        mode: EventMode,
        packages: List<LootPackage>,
        receivedIds: Set<ULong>,
        today: LocalDate,
    ): List<LootPackage> {
        val open = packages.filter { it.id !in receivedIds }

        return when (mode) {
            EventMode.COLLECT -> open
            EventMode.DAILY -> open.filter { it.fixedDay == today }
                .ifEmpty { open.filter { it.fixedDay == null } }
        }
    }
}
