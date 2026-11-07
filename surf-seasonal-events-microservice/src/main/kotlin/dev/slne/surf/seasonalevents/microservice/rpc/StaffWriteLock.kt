package dev.slne.surf.seasonalevents.microservice.rpc

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Serializes every staff write (event lifecycle, markers, loot), so a check and the write that
 * follows it cannot interleave with another write: two events cannot start at once, and the marker
 * limit cannot be exceeded by parallel clicks. Staff writes are rare, so one lock is enough.
 * Valid because only one microservice instance runs.
 */
object StaffWriteLock {

    private val mutex = Mutex()

    suspend fun <T> withLock(block: suspend () -> T): T = mutex.withLock { block() }
}
