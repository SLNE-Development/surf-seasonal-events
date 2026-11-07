package dev.slne.surf.seasonalevents.microservice.logic

import kotlin.random.Random

/** Picks the marker a player gets for the day in a `DAILY` event. */
object AssignmentPicker {

    /** Returns a random marker id from [markerIds], or null if the list is empty. */
    fun pick(markerIds: List<ULong>, random: Random): ULong? = markerIds.randomOrNull(random)
}
