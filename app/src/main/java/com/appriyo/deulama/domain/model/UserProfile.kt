package com.appriyo.deulama.domain.model

/**
 * Domain representation of the `GET /api/profile` payload — the full
 * profile of the authenticated user, including activity counters and
 * the server-computed top-3 favourite genres.
 *
 * Distinct from [User] (which only carries the public/auth essentials).
 * `profileImage` is **non-nullable** here on purpose: the server always
 * returns at least `uploads/profile/default.png` — don't introduce a
 * null-avatar fallback in the client (per api.md).
 *
 * - `favoriteGenres` is whatever the server returned, in the order
 *   returned. Treated as opaque UI data — the scoring formula lives on
 *   the server.
 * - `rawUpdatedFields` is `null` for GET responses (the server only
 *   emits `updated_fields` on PUT). The EditProfileViewModel reads it
 *   to show "what actually changed" feedback.
 */
data class UserProfile(
    val userId: Int,
    val fullName: String,
    val email: String,
    val profileImage: String,
    val likedCount: Int,
    val watchedCount: Int,
    val favoriteGenres: List<String>,
    val rawUpdatedFields: List<String>? = null,
)

/**
 * The data the EditProfile screen needs to seed its form before the
 * user changes anything. Pulled from the live AuthViewModel.
 */
data class ProfileFormSeed(
    val fullName: String,
    val profileImage: String,
    val email: String,
)

/** Convert a [User] (auth-cached profile) into a seed for the form. */
fun User.toProfileFormSeed(): ProfileFormSeed =
    ProfileFormSeed(
        fullName = fullName,
        email = email,
        profileImage = profileImage.orEmpty(), // default.png seeded by server
    )
