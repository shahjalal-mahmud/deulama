package com.appriyo.deulama.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Wire shape of the `data` payload returned by both
 * `GET /api/profile` and `PUT /api/profile`.
 *
 * Key shape notes (per api.md):
 * - `image` is **never null** — a fresh user always has the server's
 *   `uploads/profile/default.png` string. The client must not invent a
 *   null-avatar fallback path.
 * - `updated_fields` is only present on PUT responses (echoes which
 *   fields actually changed). Defaults to null on GET.
 * - `liked_count` / `watched_count` / `favorite_genres` are only on GET.
 *   Both are defaulted so a PUT response that omits them still parses.
 */
@Serializable
data class ProfileResponseDto(
    val id: Int,
    val name: String,
    val email: String,
    val image: String,
    val updated_fields: List<String>? = null,
    val liked_count: Int = 0,
    val watched_count: Int = 0,
    val favorite_genres: List<String> = emptyList(),
)

/**
 * Request body for `PUT /api/profile` when sent as `application/json`
 * (i.e. updating name and/or password only — no image).
 *
 * All fields optional. Per api.md the server validates that, when any
 * password field is present, **all three** must be — sending only one
 * or two yields `422`.
 */
@Serializable
data class ProfileUpdateRequestDto(
    val name: String? = null,
    val current_password: String? = null,
    val new_password: String? = null,
    val confirm_password: String? = null,
)
