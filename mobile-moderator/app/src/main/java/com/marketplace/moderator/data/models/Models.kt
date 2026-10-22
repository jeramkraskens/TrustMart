// Request and response records exchanged with Django.
// JSON field mappings connect backend field names to Kotlin properties.
package com.marketplace.moderator.data.models

/** JSON transport models shared by screens. SerializedName maps Django snake_case keys to Kotlin properties. */

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    val username: String,
    val password: String
)

data class User(
    val id: Int,
    val username: String,
    @SerializedName("first_name") val firstName: String,
    @SerializedName("last_name") val lastName: String,
    val role: String,
    @SerializedName("trust_score") val trustScore: Int
)

data class LoginResponse(
    val access: String,
    val refresh: String,
    val user: User
)

data class VerificationRequest(
    val id: Int,
    @SerializedName("listing_detail") val listingDetail: ListingDetail? = null,
    val moderator: Int? = null,
    @SerializedName("listing_title") val listingTitle: String,
    @SerializedName("seller_name") val sellerName: String,
    val notes: String,
    val status: String,
    val feedback: String,
    @SerializedName("created_at") val createdAt: String
)

data class PaginatedResponse<T>(
    val next: String?,
    val results: List<T>
)

data class ModerationActionRequest(
    val feedback: String = ""
)

data class Dispute(
    val id: Int,
    @SerializedName("listing_detail") val listingDetail: ListingDetail? = null,
    val moderator: Int? = null,
    @SerializedName("resolved_at") val resolvedAt: String? = null,
    @SerializedName("listing_title") val listingTitle: String,
    val amount: String,
    @SerializedName("buyer_name") val buyerName: String,
    @SerializedName("seller_name") val sellerName: String,
    val reason: String,
    val status: String,
    val resolution: String,
    @SerializedName("resolution_notes") val resolutionNotes: String
)

data class ResolveDisputeRequest(
    val resolution: String,
    @SerializedName("resolution_notes") val resolutionNotes: String
)

data class Message(
    val id: Int,
    val sender: Int,
    @SerializedName("sender_name") val senderName: String,
    val receiver: Int,
    @SerializedName("receiver_name") val receiverName: String,
    val content: String,
    @SerializedName("sent_at") val sentAt: String
)

// These read-only fields mirror ListingSerializer; missing metadata stays optional.
data class ListingDetail(
    val seller: Int? = null,
    val price: String?,
    val condition: String?,
    @SerializedName("category_name") val categoryName: String?,
    val images: List<ListingPhoto> = emptyList(),
    val district: String? = null,
    val city: String? = null,
    @SerializedName("contact_phone") val contactPhone: String? = null
)

data class ListingPhoto(val image: String)

data class SendMessageRequest(val receiver: Int, val content: String)

data class Notification(
    val id: Int, val type: String, val content: String, val link: String = "",
    @SerializedName("is_read") val isRead: Boolean,
    @SerializedName("created_at") val createdAt: String
)
