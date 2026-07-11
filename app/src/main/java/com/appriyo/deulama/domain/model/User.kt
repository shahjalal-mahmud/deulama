package com.appriyo.deulama.domain.model

/**
 * Domain representation of the signed-in user. Mirrors the `user` object
 * returned by `/api/auth/register`, `/api/auth/login`, and `/api/me`.
 *
 * - `profileImage` is nullable — not every account has one yet.
 * - `createdAt` is kept as the raw ISO-8601 string the API returns;
 *   parse it into a typed value (Instant / LocalDateTime) later when
 *   we actually need to display it somewhere.
 */
data class User(
    val userId: Int,
    val fullName: String,
    val email: String,
    val profileImage: String?,
    val createdAt: String,
)