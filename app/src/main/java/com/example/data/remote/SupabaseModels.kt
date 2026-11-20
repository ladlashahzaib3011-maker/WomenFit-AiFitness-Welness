package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SupabaseSignInRequest(
    @param:Json(name = "email") val email: String,
    @param:Json(name = "password") val password: String
)

@JsonClass(generateAdapter = true)
data class SupabaseSignUpMetadata(
    @param:Json(name = "full_name") val fullName: String
)

@JsonClass(generateAdapter = true)
data class SupabaseSignUpRequest(
    @param:Json(name = "email") val email: String,
    @param:Json(name = "password") val password: String,
    @param:Json(name = "data") val data: SupabaseSignUpMetadata
)

@JsonClass(generateAdapter = true)
data class SupabaseRefreshTokenRequest(
    @param:Json(name = "refresh_token") val refreshToken: String
)

@JsonClass(generateAdapter = true)
data class SupabaseRecoverRequest(
    @param:Json(name = "email") val email: String
)

@JsonClass(generateAdapter = true)
data class SupabaseUser(
    @param:Json(name = "id") val id: String,
    @param:Json(name = "email") val email: String? = null,
    @param:Json(name = "user_metadata") val userMetadata: Map<String, Any?>? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseAuthResponse(
    @param:Json(name = "access_token") val accessToken: String? = null,
    @param:Json(name = "token_type") val tokenType: String? = null,
    @param:Json(name = "expires_in") val expiresIn: Long? = null,
    @param:Json(name = "expires_at") val expiresAt: Long? = null,
    @param:Json(name = "refresh_token") val refreshToken: String? = null,
    @param:Json(name = "user") val user: SupabaseUser? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseErrorResponse(
    @param:Json(name = "error") val error: String? = null,
    @param:Json(name = "error_description") val errorDescription: String? = null,
    @param:Json(name = "msg") val msg: String? = null,
    @param:Json(name = "message") val message: String? = null,
    @param:Json(name = "error_code") val errorCode: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseProfileDto(
    @param:Json(name = "id") val id: String,
    @param:Json(name = "full_name") val fullName: String,
    @param:Json(name = "email") val email: String,
    @param:Json(name = "fitness_goal") val fitnessGoal: String = "Tone & Sculpt",
    @param:Json(name = "fitness_level") val fitnessLevel: String = "Intermediate",
    @param:Json(name = "age") val age: Int = 28,
    @param:Json(name = "height_cm") val heightCm: Float = 168f,
    @param:Json(name = "current_weight_kg") val currentWeightKg: Float = 62.5f,
    @param:Json(name = "target_weight_kg") val targetWeightKg: Float = 58.0f,
    @param:Json(name = "daily_water_goal_ml") val dailyWaterGoalMl: Int = 2500,
    @param:Json(name = "cycle_length_days") val cycleLengthDays: Int = 28,
    @param:Json(name = "last_period_start_timestamp") val lastPeriodStartTimestamp: Long = 0L
)
