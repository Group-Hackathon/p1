package com.livingpatientmemory.data.api

import com.livingpatientmemory.data.model.*
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.DELETE
import retrofit2.http.Path

interface ApiService {
    @POST("api/v1/auth/register")
    suspend fun register(@Body request: AuthRequest): AuthResponse

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: AuthRequest): AuthResponse

    @POST("api/v1/agents/recommend")
    suspend fun recommendAgent(@Body request: RecommendRequest): AgentResponse

    @GET("api/v1/agents")
    suspend fun getAgents(): List<AgentResponse>

    @GET("api/v1/profiles")
    suspend fun getProfiles(): List<ProfileResponse>

    @POST("api/v1/profiles")
    suspend fun createProfile(@Body request: ProfileRequest): ProfileResponse

    @GET("api/v1/subscriptions")
    suspend fun getSubscriptions(): List<SubscriptionResponse>

    @POST("api/v1/subscriptions")
    suspend fun createSubscription(@Body request: SubscriptionRequest): SubscriptionResponse

    @DELETE("api/v1/subscriptions/{id}")
    suspend fun deleteSubscription(@Path("id") id: String)

    @DELETE("api/v1/auth/delete")
    suspend fun deleteAccount()

    @GET("api/v1/subscriptions/{id}/timeline")
    suspend fun getTimeline(@Path("id") subscriptionId: String): List<TimelineEventResponse>

    @POST("api/v1/subscriptions/{id}/timeline")
    suspend fun postTimelineEvent(
        @Path("id") subscriptionId: String,
        @Body request: TimelineEventRequest
    ): TimelineEventResponse
}
