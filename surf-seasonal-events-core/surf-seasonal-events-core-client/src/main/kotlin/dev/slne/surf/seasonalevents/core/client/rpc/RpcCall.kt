package dev.slne.surf.seasonalevents.core.client.rpc

import dev.slne.surf.api.core.util.logger
import kotlinx.coroutines.CancellationException

/** Result of an RPC call that may fail because the microservice is unreachable. */
sealed interface RpcOutcome<out T> {

    /** The microservice answered with [value]. */
    data class Ok<T>(val value: T) : RpcOutcome<T>

    /** The call failed, e.g. timeout or broken connection; [cause] is already logged. */
    data class Unavailable(val cause: Throwable) : RpcOutcome<Nothing>
}

private val log = logger()

/**
 * Runs an RPC call and never throws for a failed call: every exception becomes
 * [RpcOutcome.Unavailable] and is logged. A [CancellationException] is rethrown, so the
 * calling coroutine can still be cancelled.
 */
suspend fun <T> rpcCall(block: suspend () -> T): RpcOutcome<T> = try {
    RpcOutcome.Ok(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    log.atWarning()
        .withCause(e)
        .log("Seasonal events microservice call failed")
    RpcOutcome.Unavailable(e)
}
