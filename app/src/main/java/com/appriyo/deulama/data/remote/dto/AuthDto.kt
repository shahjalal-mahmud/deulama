package com.appriyo.deulama.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Wire shape of a user as the API returns it. JSON keys are snake_case
 * to match api.md; the mapper converts to the `User` domain model.
 */
@Serializable
data class UserDto(
    val user_id: Int,
    val full_name: String,
    val email: String,
    val profile_image: String? = null,
    val created_at: String,
)

/**
 * Body of the `data` field returned by both `/api/auth/register` (201)
 * and `/api/auth/login` (200).
 */
@Serializable
data class AuthResponseDto(
    val user: UserDto,
    val token: String,
)

/** Request body for POST /api/auth/register. */
@Serializable
data class RegisterRequest(
    val full_name: String,
    val email: String,
    val password: String,
    val password_confirmation: String,
)

/** Request body for POST /api/auth/login. */
@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)