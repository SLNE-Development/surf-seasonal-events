package dev.slne.surf.seasonalevents.microservice.logic

import java.time.LocalDate

/**
 * A filled loot package without its items: enough to decide which package a player gets, so the
 * item bytes are only loaded for the package that is actually handed out.
 */
data class PackageRef(val id: ULong, val fixedDay: LocalDate?)
