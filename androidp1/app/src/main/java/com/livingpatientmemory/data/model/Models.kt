package com.livingpatientmemory.data.model

data class AuthRequest(
    val email: String,
    val password: String
)

data class AuthResponse(
    val token: String,
    val user: User
)

data class RecommendRequest(
    val symptoms: String,
    val appointment_date: String? = null,
    val rules: TrackingRulesDto? = null
)

data class User(
    val id: String,
    val email: String,
    val created_at: String
)

data class ProfileResponse(
    val id: String,
    val first_name: String,
    val last_name: String,
    val relation: String,
    val created_at: String
)

data class ProfileRequest(
    val first_name: String,
    val last_name: String,
    val relation: String
)

data class AgentResponse(
    val id: String,
    val name: String,
    val version: String,
    val category: String,
    val description: String,
    val price_cents: Int,
    val duration_days_min: Int,
    val duration_days_max: Int,
    val gemini_model: String
)

data class SubscriptionRequest(
    val profile_id: String,
    val agent_id: String,
    val private_backend_url: String? = null,
    val parameters: Map<String, Any>? = null
)

data class SubscriptionResponse(
    val id: String,
    val profile_id: String,
    val agent_id: String,
    val status: String,
    val private_backend_url: String,
    val starts_at: String,
    val expires_at: String
)

// ── New models for the refonte ──

data class TrackingRulesDto(
    val temperature: Boolean = false,
    val pain: Boolean = true,
    val photos: Boolean = true,
    val smartwatch: Boolean = false,
    val blood_pressure: Boolean = false,
    val custom: String = ""
)

/** Local-only model representing tracking rules on the device */
data class TrackingRules(
    val temperature: Boolean = true,
    val pain: Boolean = true,
    val photos: Boolean = true,
    val smartwatch: Boolean = false,
    val bloodPressure: Boolean = false,
    val custom: String = ""
)

/** Local-only model representing an AI-generated plan item */
data class PlanItem(
    val icon: String,
    val title: String,
    val description: String
)

data class TimelineEventRequest(
    val content: String,
    val date_label: String
)

data class TimelineEventResponse(
    val id: String,
    val subscription_id: String,
    val type: String,
    val date_label: String,
    val content: String,
    val created_at: String
)
