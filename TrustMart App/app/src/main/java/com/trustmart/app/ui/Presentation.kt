// Display helpers used to present marketplace data in the Android UI.
package com.trustmart.app.ui

import com.trustmart.app.data.*
import java.math.BigDecimal
import java.text.DecimalFormat

/** Display transformations remain pure and tested; never derive fictional discounts or courier updates. */
fun money(value: String): String = value.toBigDecimalOrNull()?.let {
    DecimalFormat("#,##0.##", java.text.DecimalFormatSymbols(java.util.Locale.US)).format(it)
} ?: value
fun initials(name: String): String = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    .take(2).joinToString("") { it.take(1).uppercase() }.ifEmpty { "TM" }
fun averageRating(reviews: List<Review>): String = if (reviews.isEmpty()) "—" else
    String.format(java.util.Locale.US, "%.1f", reviews.map { it.rating }.average())
fun orderLabel(order: Order, disputes: List<Dispute>): String = when {
    disputes.any { it.transaction == order.id && it.status != "resolved" } -> "Dispute"
    order.escrowStatus == "refunded" -> "Refunded"
    order.escrowStatus == "released" -> "Completed"
    order.deliveryConfirmed -> "Delivered"
    else -> "In escrow"
}
fun belowBudget(items: List<Listing>, maximum: String): List<Listing> {
    val max = maximum.toBigDecimalOrNull() ?: return items
    return items.filter { (it.price.toBigDecimalOrNull() ?: BigDecimal.ZERO) <= max }
}
fun releasedRevenue(orders: List<Order>, sellerId: Int): String = orders
    .filter { it.sellerId == sellerId && it.escrowStatus == "released" }
    .fold(BigDecimal.ZERO) { total, item -> total + (item.sellerPayout.toBigDecimalOrNull() ?: BigDecimal.ZERO) }.toPlainString()
