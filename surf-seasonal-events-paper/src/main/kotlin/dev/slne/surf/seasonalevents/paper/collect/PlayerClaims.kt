package dev.slne.surf.seasonalevents.paper.collect

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Lets each player run one item hand-out at a time, whether it comes from a block or from the NPC, so two
 * of them cannot check the same free inventory space and then both fill it. The microservice still decides
 * what may be booked; this only guards the local check and delivery.
 */
object PlayerClaims {

    private val busy = ConcurrentHashMap.newKeySet<UUID>()

    /** Runs [action] unless another hand-out of [player] is running; further clicks are ignored meanwhile. */
    suspend fun exclusive(player: UUID, action: suspend () -> Unit) {
        if (!busy.add(player)) return
        try {
            action()
        } finally {
            busy.remove(player)
        }
    }
}
