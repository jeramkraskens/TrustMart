package com.trustmart.app

import com.trustmart.app.data.*
import com.trustmart.app.ui.*
import org.junit.Assert.*
import org.junit.Test

class PresentationTest {
    private val item = Listing(1, 2, "Seller", 80, 1, "Phones", "Phone", "", "145000.50", "good", "verified", "Colombo", emptyList())
    private val order = Order(1, 3, "Buyer", 1, "Phone", 2, "Seller", "145000.50", "142100.49", "held", "card", false, "2026-09-26")
    @Test fun initialsHandleBlankAndWhitespace() {
        assertEquals("TM", initials(" "))
        assertEquals("NK", initials(" Nimal   Karunaratne "))
    }
    @Test fun priceRetainsFractionAndGroups() { assertEquals("145,000.5", money("145000.50")) }
    @Test fun missingRatingsAreNotInvented() {
        assertEquals("—", averageRating(emptyList()))
        assertEquals("4.5", averageRating(listOf(Review(1, 1, 3, "", 4, ""), Review(2, 2, 4, "", 5, ""))))
    }
    @Test fun budgetUsesDecimalInclusiveBoundary() {
        assertEquals(listOf(item), belowBudget(listOf(item), "145000.50"))
        assertTrue(belowBudget(listOf(item), "145000.49").isEmpty())
        assertEquals(listOf(item), belowBudget(listOf(item), ""))
    }
    @Test fun revenueExcludesHeldRefundedAndOtherSellers() {
        assertEquals("142100.49", releasedRevenue(listOf(order, order.copy(escrowStatus = "released"),
            order.copy(escrowStatus = "refunded"), order.copy(escrowStatus = "released", sellerId = 9)), 2))
    }
    @Test fun orderStateComesFromBackend() {
        assertEquals("In escrow", orderLabel(order, emptyList()))
        assertEquals("Delivered", orderLabel(order.copy(deliveryConfirmed = true), emptyList()))
        assertEquals("Completed", orderLabel(order.copy(escrowStatus = "released"), emptyList()))
        assertEquals("Refunded", orderLabel(order.copy(escrowStatus = "refunded"), emptyList()))
        val dispute = Dispute(1, 1, "", "pending", "")
        assertEquals("Dispute", orderLabel(order, listOf(dispute)))
        assertEquals("In escrow", orderLabel(order, listOf(dispute.copy(status = "resolved"))))
    }
}
