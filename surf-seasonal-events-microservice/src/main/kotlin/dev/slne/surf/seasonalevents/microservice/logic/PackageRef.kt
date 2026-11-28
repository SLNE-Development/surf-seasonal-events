package dev.slne.surf.seasonalevents.microservice.logic

import java.time.LocalDate

/**
 * A filled loot package without its items: enough to decide which package a player gets, so the
 * item bytes are only loaded for the package that is actually handed out.
 *
 * @param npcReward whether the event's NPC hands the package out; it is never found on the map
 */
data class PackageRef(val id: ULong, val fixedDay: LocalDate?, val npcReward: Boolean = false)
