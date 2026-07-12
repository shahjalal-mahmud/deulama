package com.appriyo.deulama.domain.repository

import com.appriyo.deulama.data.remote.ApiResult
import com.appriyo.deulama.domain.model.UserProfile

/**
 * Profile-management operations the presentation layer is allowed to
 * call. All network calls return [ApiResult] (see `data/remote/ApiResult.kt`)
 * so the UI can render banners / per-field errors without try/catch noise.
 */
interface ProfileRepository {

    /**
     * `GET /api/profile` — returns the authenticated user's full profile
     * (id, name, email, image, counts, top-3 favourite genres).
     */
    suspend fun getProfile(): ApiResult<UserProfile>

    /**
     * `PUT /api/profile` — flexible update with three optional field
     * groups:
     *
     * - `name` (non-empty to update).
     * - All three password fields **together**, or none. Sending only
     *   one or two yields a `422` from the server.
     * - `imageBytes` + `imageMime` + `imageName` for an avatar upload.
     *   When non-null the repository switches to `multipart/form-data`;
     *   otherwise it sends `application/json`.
     *
     * Returns the new [UserProfile] AND the raw `updated_fields` list
     * (echoed only on PUT responses) so the UI can show what changed.
     */
    suspend fun updateProfile(
        name: String?,
        currentPassword: String?,
        newPassword: String?,
        confirmPassword: String?,
        imageBytes: ByteArray?,
        imageMime: String?,
        imageName: String?,
    ): ApiResult<UpdateProfileResult>
}

/**
 * Result envelope returned by [ProfileRepository.updateProfile].
 *
 * `profile` is the new profile data (always emitted on success).
 * `updatedFields` is whatever the server echoed in
 * `data.updated_fields` — entries are one of `"name"`, `"image"`,
 * `"password"`. May be `null` if the server omitted the field.
 */
data class UpdateProfileResult(
    val profile: UserProfile,
    val updatedFields: List<String>?,
)
