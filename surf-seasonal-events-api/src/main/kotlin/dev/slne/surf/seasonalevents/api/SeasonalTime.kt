package dev.slne.surf.seasonalevents.api

import java.time.ZoneId

/** Time settings shared by plugin and microservice. */
object SeasonalTime {

    /** Every calendar day of an event (daily markers, fixed days, daily limits) is a day in this zone. */
    val ZONE: ZoneId = ZoneId.of("Europe/Berlin")
}
