package dev.slne.surf.seasonalevents.core.client.rpc

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeoutException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame

/** Verifies that [rpcCall] turns failures into [RpcOutcome.Unavailable] without hiding cancellation. */
class RpcCallTest {

    @Test
    fun `success gives Ok with the value`() = runBlocking {
        val outcome = rpcCall { 42 }
        assertEquals(RpcOutcome.Ok(42), outcome)
    }

    @Test
    fun `an exception gives Unavailable with the cause`() = runBlocking {
        val failure = IllegalStateException("broker down")
        val outcome = rpcCall<Int> { throw failure }
        assertIs<RpcOutcome.Unavailable>(outcome)
        assertSame(failure, outcome.cause)
    }

    @Test
    fun `a timeout gives Unavailable`() = runBlocking {
        val outcome = rpcCall<Int> { throw TimeoutException("no answer") }
        assertIs<RpcOutcome.Unavailable>(outcome)
        Unit // JUnit only runs test methods that return Unit
    }

    @Test
    fun `cancellation is rethrown`() {
        assertFailsWith<CancellationException> {
            runBlocking { rpcCall<Int> { throw CancellationException("cancelled") } }
        }
    }
}
