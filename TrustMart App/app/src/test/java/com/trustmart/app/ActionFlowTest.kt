package com.trustmart.app

import com.trustmart.app.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

/** Regression checks for successful writes followed by failed refreshes and stale order actions. */
class ActionFlowTest {
    private val buyer = User(1, "buyer", "", "buyer", "", "", "", 0)
    private val order = Order(1, 1, "buyer", 7, "Phone", 2, "seller", "100", "95", "held", "card", false, "2026-09-26")
    @Test fun acknowledgementPrecedesRefresh() = runBlocking {
        val events = mutableListOf<String>()
        acknowledgeThenRefresh({ events += "ack" }, { events += "refresh" }, { events += "failure" })
        assertEquals(listOf("ack", "refresh"), events)
    }
    @Test fun refreshFailureDoesNotRestoreAcknowledgedDraft() = runBlocking {
        var draft = "Send once"
        var failed = false
        acknowledgeThenRefresh({ draft = "" }, { throw IOException("offline after POST") }, { failed = true })
        assertEquals("", draft); assertTrue(failed)
    }
    @Test fun cancellationStillPropagates() = runBlocking {
        var acknowledged = false
        try {
            acknowledgeThenRefresh({ acknowledged = true }, { throw CancellationException("stopped") }, { fail("Must not swallow cancellation") })
            fail("Expected cancellation")
        } catch (_: CancellationException) { assertTrue(acknowledged) }
    }
    @Test fun authenticationFailureCanReachSessionHandler() = runBlocking {
        try {
            acknowledgeThenRefresh({}, { throw ApiFailure(401, "expired") }, { throw it })
            fail("Expected session rejection")
        } catch (e: ApiFailure) { assertEquals(401, e.status) }
    }
    @Test fun disputeRejectsWrongBuyerTerminalStatesAndExistingCases() {
        assertTrue(canDispute(order, buyer, emptyList()))
        assertFalse(canDispute(order, buyer.copy(id = 9), emptyList()))
        assertFalse(canDispute(order, buyer.copy(role = "seller"), emptyList()))
        assertFalse(canDispute(order.copy(escrowStatus = "released"), buyer, emptyList()))
        assertFalse(canDispute(order, buyer, listOf(Dispute(1, 1, "", "open", ""))))
    }
    @Test fun reviewRequiresCompletedOwnUnreviewedOrder() {
        val complete = order.copy(escrowStatus = "released")
        assertTrue(canReview(complete, buyer, emptyList()))
        assertFalse(canReview(order, buyer, emptyList()))
        assertFalse(canReview(complete, buyer.copy(id = 9), emptyList()))
        assertFalse(canReview(complete, buyer, listOf(Review(1, 1, 1, "", 5, ""))))
    }
    @Test fun unrelatedMessagesNeverCreateInboxThreads() {
        val mine = Message(1, 1, "Buyer", 2, "Seller", "Mine", "2026-09-26")
        val unrelated = mine.copy(id = 2, sender = 9)
        assertEquals(1, conversations(listOf(mine, unrelated), 1).size)
        assertEquals(1, conversations(listOf(mine, unrelated), 1).first().last.id)
    }
    @Test fun threadIncludesOnlyBothParticipantsInTimeOrder() {
        val sent = Message(1, 1, "Buyer", 2, "Seller", "Hi", "2026-09-25")
        val reply = sent.copy(id = 2, sender = 2, receiver = 1, sentAt = "2026-09-26")
        assertEquals(listOf(sent, reply), conversationMessages(listOf(reply, sent, sent.copy(id = 3, receiver = 9)), 1, 2))
    }
}
