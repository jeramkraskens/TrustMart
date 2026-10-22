// Kotlin representations of API data plus helpers for deciding which actions to show.
// These are client-side records; Django models define the actual database tables.
package com.trustmart.app.data

/** API contracts mirror Django serializers. Money stays decimal text, never floating-point. */
data class User(val id: Int, val username: String, val email: String, val role: String,
    val firstName: String, val lastName: String, val phone: String, val trustScore: Int,
    val district: String = "", val nicNumber: String = "", val nicSuffix: String = "")
data class Category(val id: Int, val name: String)
data class Photo(val id: Int, val image: String)
data class Listing(val id: Int, val seller: Int, val sellerName: String, val sellerTrustScore: Int,
    val category: Int, val categoryName: String?, val title: String, val description: String,
    val price: String, val condition: String, val status: String, val location: String,
    val images: List<Photo>, val district: String = "", val city: String = "",
    val contactPhone: String = "", val whatsappEnabled: Boolean = false, val canManage: Boolean = false)

data class Advertisement(val title: String, val description: String, val image: String?,
    val linkUrl: String?, val originalPrice: String?, val effectiveOfferPrice: String?, val effectiveDiscountPercent: String?)
data class SearchSuggestion(val label: String, val detail: String, val kind: String, val url: String)

/** Match the final website's ASCII phone/password requirements. Server validation remains authoritative. */
fun validPhone(value: String) = Regex("[0-9]{10}").matches(value)
fun validPassword(value: String) = value.length >= 8 && value.any { it in 'A'..'Z' } &&
    value.any { it in 'a'..'z' } && value.any { it in '0'..'9' }
data class SavedListing(val listing: Int, val listingDetail: Listing)
data class Order(val id: Int, val buyer: Int, val buyerName: String, val listing: Int,
    val listingTitle: String, val sellerId: Int, val sellerName: String, val amount: String,
    val sellerPayout: String, val escrowStatus: String, val paymentMethod: String,
    val deliveryConfirmed: Boolean, val createdAt: String)
data class Message(val id: Int, val sender: Int, val senderName: String, val receiver: Int,
    val receiverName: String, val content: String, val sentAt: String)
data class Review(val id: Int, val transaction: Int, val reviewer: Int, val reviewerName: String,
    val rating: Int, val comment: String)
data class Dispute(val id: Int, val transaction: Int, val reason: String,
    val status: String, val resolutionNotes: String)
data class Verification(val id: Int, val listing: Int, val status: String, val feedback: String)
data class Notice(val id: Int, val content: String, val isRead: Boolean,
    val type: String = "", val link: String = "", val createdAt: String = "")
data class Conversation(val userId: Int, val name: String, val last: Message)

/** Group by stable user ID, not a mutable display name; preserve chronological message order. */
fun conversations(messages: List<Message>, me: Int): List<Conversation> = messages
    .filter { it.sender == me || it.receiver == me }
    .groupBy { if (it.sender == me) it.receiver else it.sender }
    .map { (id, thread) ->
        val last = thread.maxBy { it.sentAt }
        Conversation(id, if (last.sender == me) last.receiverName else last.senderName, last)
    }.sortedByDescending { it.last.sentAt }

fun canBuy(item: Listing, me: User?) = me?.role == "buyer" && item.seller != me.id && item.status == "verified"
fun canConfirm(order: Order, me: User, disputed: Boolean) = order.buyer == me.id &&
    order.escrowStatus == "held" && !order.deliveryConfirmed && !disputed

fun canDispute(order: Order, me: User, disputes: List<Dispute>) = me.role == "buyer" &&
    canConfirm(order, me, disputes.any { it.transaction == order.id })
fun canReview(order: Order, me: User, reviews: List<Review>) = me.role == "buyer" &&
    order.buyer == me.id && order.escrowStatus == "released" && reviews.none { it.transaction == order.id && it.reviewer == me.id }

fun conversationMessages(messages: List<Message>, me: Int, other: Int) = messages
    .filter { (it.sender == me && it.receiver == other) || (it.sender == other && it.receiver == me) }
    .sortedBy { it.sentAt }
