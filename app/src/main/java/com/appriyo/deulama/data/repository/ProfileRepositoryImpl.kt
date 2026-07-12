package com.appriyo.deulama.data.repository

import com.appriyo.deulama.data.local.datastore.SessionManager
import com.appriyo.deulama.data.remote.ApiResult
import com.appriyo.deulama.data.remote.api.ProfileApi
import com.appriyo.deulama.data.remote.dto.ProfileResponseDto
import com.appriyo.deulama.data.remote.dto.ProfileUpdateRequestDto
import com.appriyo.deulama.data.remote.safeApiCall
import com.appriyo.deulama.domain.model.Session
import com.appriyo.deulama.domain.model.User
import com.appriyo.deulama.domain.model.UserProfile
import com.appriyo.deulama.domain.repository.ProfileRepository
import com.appriyo.deulama.domain.repository.UpdateProfileResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Concrete [ProfileRepository] backed by Retrofit [ProfileApi] and
 * the local [SessionManager].
 *
 * Routing rules:
 * - If [ProfileRepository.updateProfile] is called with a non-null
 *   `imageBytes` → multipart PUT (so the file goes through as a real
 *   `image` part).
 * - Otherwise → JSON PUT (faster, no `multipart/form-data` overhead).
 *
 * On any successful PUT the returned `User` shape is written back into
 * the persisted session so the live `AuthViewModel.state.user` (which
 * drives ProfileScreen, Home, etc.) reflects the new name/avatar
 * without an explicit re-fetch.
 */
class ProfileRepositoryImpl(
    private val profileApi: ProfileApi,
    private val sessionManager: SessionManager,
    private val json: Json,
) : ProfileRepository {

    override suspend fun getProfile(): ApiResult<UserProfile> =
        when (val result = safeApiCall(json) { profileApi.getProfile() }) {
            is ApiResult.Success -> ApiResult.Success(result.value.toDomain())
            is ApiResult.ValidationError -> result
            is ApiResult.Error -> result
            is ApiResult.NetworkError -> result
        }

    override suspend fun updateProfile(
        name: String?,
        currentPassword: String?,
        newPassword: String?,
        confirmPassword: String?,
        imageBytes: ByteArray?,
        imageMime: String?,
        imageName: String?,
    ): ApiResult<UpdateProfileResult> {
        val trimmedName = name?.trim()?.takeIf { it.isNotEmpty() }
        val trimmedCurrent = currentPassword?.takeIf { it.isNotEmpty() }
        val trimmedNew = newPassword?.takeIf { it.isNotEmpty() }
        val trimmedConfirm = confirmPassword?.takeIf { it.isNotEmpty() }

        return when (val result = if (imageBytes != null) {
            updateViaMultipart(
                name = trimmedName,
                currentPassword = trimmedCurrent,
                newPassword = trimmedNew,
                confirmPassword = trimmedConfirm,
                imageBytes = imageBytes,
                imageMime = imageMime ?: "image/jpeg",
                imageName = imageName ?: "avatar.jpg",
            )
        } else {
            updateViaJson(
                name = trimmedName,
                currentPassword = trimmedCurrent,
                newPassword = trimmedNew,
                confirmPassword = trimmedConfirm,
            )
        }) {
            is ApiResult.Success -> {
                val profile = result.value.toDomain()
                // Refresh the persisted session so AuthViewModel emits the
                // new name/avatar across the app immediately.
                refreshSessionFromProfile(profile)
                ApiResult.Success(
                    UpdateProfileResult(
                        profile = profile,
                        updatedFields = result.value.updated_fields,
                    ),
                )
            }
            is ApiResult.ValidationError -> result
            is ApiResult.Error -> result
            is ApiResult.NetworkError -> result
        }
    }

    private suspend fun updateViaJson(
        name: String?,
        currentPassword: String?,
        newPassword: String?,
        confirmPassword: String?,
    ): ApiResult<ProfileResponseDto> {
        val body = ProfileUpdateRequestDto(
            name = name,
            current_password = currentPassword,
            new_password = newPassword,
            confirm_password = confirmPassword,
        )
        return when (val result = safeApiCall(json) { profileApi.updateProfileJson(body) }) {
            is ApiResult.Success -> result
            is ApiResult.ValidationError -> result
            is ApiResult.Error -> result
            is ApiResult.NetworkError -> result
        }
    }

    private suspend fun updateViaMultipart(
        name: String?,
        currentPassword: String?,
        newPassword: String?,
        confirmPassword: String?,
        imageBytes: ByteArray,
        imageMime: String,
        imageName: String,
    ): ApiResult<ProfileResponseDto> = withContext(Dispatchers.IO) {
        val textMedia = "text/plain; charset=utf-8".toMediaType()
        val textParts: MutableMap<String, RequestBody> = LinkedHashMap()
        if (name != null) textParts["name"] = name.toRequestBody(textMedia)
        if (currentPassword != null) textParts["current_password"] = currentPassword.toRequestBody(textMedia)
        if (newPassword != null) textParts["new_password"] = newPassword.toRequestBody(textMedia)
        if (confirmPassword != null) textParts["confirm_password"] = confirmPassword.toRequestBody(textMedia)

        val imageMedia = imageMime.toMediaType()
        val imagePart = MultipartBody.Part.createFormData(
            name = "image",
            filename = imageName,
            body = imageBytes.toRequestBody(imageMedia),
        )

        when (val result = safeApiCall(json) {
            profileApi.updateProfileMultipart(textParts, imagePart)
        }) {
            is ApiResult.Success -> result
            is ApiResult.ValidationError -> result
            is ApiResult.Error -> result
            is ApiResult.NetworkError -> result
        }
    }

    private suspend fun refreshSessionFromProfile(profile: UserProfile) {
        val current = sessionManager.currentSession() ?: return
        // Preserve the cached createdAt — PUT /api/profile doesn't echo it.
        val mergedUser = User(
            userId = profile.userId,
            fullName = profile.fullName,
            email = profile.email,
            profileImage = profile.profileImage,
            createdAt = current.user.createdAt,
        )
        sessionManager.saveSession(Session(user = mergedUser, token = current.token))
    }
}

/**
 * Wire DTO → domain mapping.
 *
 * `profileImage` is **non-nullable** in the DTO (server always returns
 * at least `default.png`) — so the domain model keeps it non-null too.
 */
private fun ProfileResponseDto.toDomain(): UserProfile = UserProfile(
    userId = id,
    fullName = name,
    email = email,
    profileImage = image,
    likedCount = liked_count,
    watchedCount = watched_count,
    favoriteGenres = favorite_genres,
    rawUpdatedFields = updated_fields,
)