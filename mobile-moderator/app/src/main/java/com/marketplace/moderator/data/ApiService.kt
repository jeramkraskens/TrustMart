// Retrofit endpoint declarations for the moderator app.
// Annotations specify HTTP verbs, URL paths and whether values belong in the body, path or query.
package com.marketplace.moderator.data

/** Retrofit endpoint contract. Request and response names must match Django serializers; credentials stay in the authorization interceptor. */

import com.marketplace.moderator.data.models.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @GET("notifications/")
    suspend fun getNotifications(@Query("page") page: Int = 1): Response<PaginatedResponse<Notification>>

    @POST("notifications/{id}/mark_read/")
    suspend fun markNotificationRead(@Path("id") id: Int): Response<Notification>

    @POST("notifications/mark_all_read/")
    suspend fun markAllNotificationsRead(): Response<Map<String, String>>

    @POST("auth/login/")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("auth/me/")
    suspend fun getMe(): Response<User>

    @PATCH("auth/me/")
    suspend fun updateProfile(@Body fields: Map<String, String>): Response<User>

    @POST("auth/change-password/")
    suspend fun changePassword(@Body fields: Map<String, String>): Response<Map<String, String>>

    @GET("verifications/")
    suspend fun getVerifications(
        @Query("status") status: String? = null,
        @Query("page") page: Int = 1
    ): Response<PaginatedResponse<VerificationRequest>>

    @GET("verifications/{id}/")
    suspend fun getVerification(@Path("id") id: Int): Response<VerificationRequest>

    @POST("verifications/{id}/approve/")
    suspend fun approveVerification(
        @Path("id") id: Int,
        @Body body: ModerationActionRequest
    ): Response<VerificationRequest>

    @POST("verifications/{id}/reject/")
    suspend fun rejectVerification(
        @Path("id") id: Int,
        @Body body: ModerationActionRequest
    ): Response<VerificationRequest>

    @GET("disputes/")
    suspend fun getDisputes(@Query("page") page: Int = 1): Response<PaginatedResponse<Dispute>>

    @GET("disputes/{id}/")
    suspend fun getDispute(@Path("id") id: Int): Response<Dispute>

    @POST("disputes/{id}/resolve/")
    suspend fun resolveDispute(
        @Path("id") id: Int,
        @Body body: ResolveDisputeRequest
    ): Response<Dispute>

    @GET("messages/")
    suspend fun getMessages(@Query("with") withUserId: Int? = null, @Query("page") page: Int = 1): Response<PaginatedResponse<Message>>

    @POST("messages/")
    suspend fun sendMessage(@Body message: SendMessageRequest): Response<Message>
}
