package com.appriyo.deulama.presentation.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appriyo.deulama.data.remote.ApiResult
import com.appriyo.deulama.data.util.ImageCompressor
import com.appriyo.deulama.data.util.ImageValidator
import com.appriyo.deulama.domain.model.User
import com.appriyo.deulama.domain.model.toProfileFormSeed
import com.appriyo.deulama.domain.repository.ProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * State + edit controller for the Edit Profile screen.
 *
 * The screen collects [form] and [authUser]; the latter is the live
 * cached user from [com.appriyo.deulama.presentation.auth.AuthViewModel]
 * — passed in as a dependency so we don't couple two ViewModels together
 * via Koin. Used only to seed the name field on first composition and
 * to show the current avatar.
 *
 * Validation mirrors api.md exactly:
 * - name: 2–150 chars after trim.
 * - password fields: all three (current/new/confirm) OR none — partial
 *   sets yield a server 422 (we short-circuit client-side too).
 * - new password: 8–255 chars.
 * - confirm: must equal new.
 * - image: pre-checked by [ImageValidator] before submit (≤ 5 MB, jpg/png/webp).
 *   Then compressed by [ImageCompressor] (max edge 1024 px, JPEG q85).
 */
class EditProfileViewModel(
    private val profileRepository: ProfileRepository,
    context: Context,
) : ViewModel() {

    /** Kept locally — never escapes the VM. */
    private val contentResolver = context.contentResolver

    private val _form = MutableStateFlow(EditProfileFormState())
    val form: StateFlow<EditProfileFormState> = _form.asStateFlow()

    // ---- Seeding ----

    /**
     * Called once by the screen on first composition. Pre-fills the
     * name field with the live cached user.fullName. Idempotent — does
     * nothing if the form already has a typed name.
     */
    fun seedFromUser(user: User?) {
        if (user == null) return
        if (_form.value.isSeeded) return
        val seed = user.toProfileFormSeed()
        _form.update {
            it.copy(
                name = seed.fullName,
                isSeeded = true,
            )
        }
    }

    // ---- Field change handlers ----

    fun onNameChanged(v: String) =
        _form.update { it.copy(name = v, nameError = null, banner = null) }

    fun onCurrentPasswordChanged(v: String) =
        _form.update { it.copy(currentPassword = v, currentPasswordError = null, banner = null) }

    fun onNewPasswordChanged(v: String) =
        _form.update { it.copy(newPassword = v, newPasswordError = null, confirmPasswordError = null, banner = null) }

    fun onConfirmPasswordChanged(v: String) =
        _form.update { it.copy(confirmPassword = v, confirmPasswordError = null, banner = null) }

    /**
     * User picked an image from the system photo picker.
     *
     * Runs validation immediately. If the file passes, kicks off
     * compression on `Dispatchers.IO` and stores the resulting bytes
     * + filename + MIME on the form state so the next submit reuses
     * them. Bad files set `imageError` and skip compression.
     */
    fun onImagePicked(uri: Uri) {
        viewModelScope.launch {
            when (val check = ImageValidator.validate(uri, contentResolver)) {
                is ImageValidator.Result.Invalid -> {
                    _form.update { it.copy(imageError = check.reason, banner = check.reason) }
                    return@launch
                }
                is ImageValidator.Result.Ok -> {
                    val compressed = runCatching {
                        withContext(Dispatchers.IO) {
                            ImageCompressor.compress(
                                uri = uri,
                                contentResolver = contentResolver,
                                originalMime = check.mime,
                            )
                        }
                    }
                    compressed.fold(
                        onSuccess = { bytes ->
                            _form.update {
                                it.copy(
                                    imageBytes = bytes,
                                    imageMime = check.mime,
                                    imageName = "avatar.jpg",
                                    imageError = null,
                                    banner = null,
                                )
                            }
                        },
                        onFailure = { e ->
                            _form.update {
                                it.copy(
                                    imageError = "Couldn't read that image. Try a different file.",
                                    banner = "Couldn't read that image. Try a different file.",
                                )
                            }
                        },
                    )
                }
            }
        }
    }

    /** Clears the pending image (the user tapped "Remove"). */
    fun onImageCleared() =
        _form.update { it.copy(imageBytes = null, imageMime = null, imageName = null, imageError = null) }

    // ---- Submit ----

    /**
     * Validate the whole form, run the update, surface results.
     *
     * On success: reset just the password fields (so they don't stay
     * visible), keep the new name + image preview, and show a banner
     * derived from the server's `updated_fields` list (e.g.
     * "Profile updated (name, password).").
     */
    fun submit() {
        val current = _form.value
        val trimmedName = current.name.trim()

        val nameErr = validateName(trimmedName)

        // Password-set detection: ANY of the three filled ⇒ all three required.
        val passwordTouched = current.currentPassword.isNotEmpty() ||
            current.newPassword.isNotEmpty() ||
            current.confirmPassword.isNotEmpty()

        val partialPasswordErr = if (passwordTouched && !allThreeFilled(current)) {
            "All three password fields are required together."
        } else null

        val currentPasswordErr = if (passwordTouched && current.currentPassword.isEmpty()) {
            "Enter your current password."
        } else null
        val newPasswordErr = if (passwordTouched) validateNewPassword(current.newPassword) else null
        val confirmPasswordErr = if (passwordTouched) {
            validateConfirmPassword(current.newPassword, current.confirmPassword)
        } else null

        if (nameErr != null || partialPasswordErr != null ||
            currentPasswordErr != null || newPasswordErr != null || confirmPasswordErr != null
        ) {
            _form.update {
                it.copy(
                    nameError = nameErr,
                    currentPasswordError = currentPasswordErr ?: partialPasswordErr,
                    newPasswordError = newPasswordErr,
                    confirmPasswordError = confirmPasswordErr ?: partialPasswordErr,
                    banner = partialPasswordErr,
                )
            }
            return
        }

        _form.update { it.copy(isSubmitting = true, banner = null) }

        viewModelScope.launch {
            val result = profileRepository.updateProfile(
                name = trimmedName,
                currentPassword = current.currentPassword.takeIf { it.isNotEmpty() },
                newPassword = current.newPassword.takeIf { it.isNotEmpty() },
                confirmPassword = current.confirmPassword.takeIf { it.isNotEmpty() },
                imageBytes = current.imageBytes,
                imageMime = current.imageMime,
                imageName = current.imageName,
            )

            when (result) {
                is ApiResult.Success -> {
                    _form.update {
                        EditProfileFormState(
                            // Re-seed with the new values so the screen
                            // shows the latest data.
                            name = result.value.profile.fullName,
                            // Clear password fields — they should not
                            // linger after a successful change.
                            currentPassword = "",
                            newPassword = "",
                            confirmPassword = "",
                            // Keep the new image preview; user can clear it.
                            imageBytes = it.imageBytes,
                            imageMime = it.imageMime,
                            imageName = it.imageName,
                            isSeeded = true,
                            banner = formatUpdatedFieldsBanner(result.value.updatedFields),
                        )
                    }
                }
                is ApiResult.ValidationError -> {
                    val byField = result.fieldErrors
                    _form.update {
                        it.copy(
                            isSubmitting = false,
                            nameError = byField[ApiFields.NAME]?.firstOrNull() ?: it.nameError,
                            currentPasswordError = byField[ApiFields.CURRENT_PASSWORD]?.firstOrNull(),
                            newPasswordError = byField[ApiFields.NEW_PASSWORD]?.firstOrNull(),
                            confirmPasswordError = byField[ApiFields.CONFIRM_PASSWORD]?.firstOrNull(),
                            imageError = byField[ApiFields.IMAGE]?.firstOrNull() ?: it.imageError,
                            banner = firstFieldErrorExcluding(
                                byField,
                                setOf(
                                    ApiFields.NAME,
                                    ApiFields.CURRENT_PASSWORD,
                                    ApiFields.NEW_PASSWORD,
                                    ApiFields.CONFIRM_PASSWORD,
                                    ApiFields.IMAGE,
                                ),
                            ),
                        )
                    }
                }
                is ApiResult.Error -> _form.update {
                    it.copy(isSubmitting = false, banner = result.message)
                }
                is ApiResult.NetworkError -> _form.update {
                    it.copy(isSubmitting = false, banner = NETWORK_BANNER)
                }
            }
        }
    }

    /** Dismiss the success/error banner without changing form state. */
    fun clearBanner() = _form.update { it.copy(banner = null) }

    // ---- Helpers ----

    private fun allThreeFilled(s: EditProfileFormState): Boolean =
        s.currentPassword.isNotEmpty() &&
            s.newPassword.isNotEmpty() &&
            s.confirmPassword.isNotEmpty()

    private fun formatUpdatedFieldsBanner(updated: List<String>?): String? {
        if (updated.isNullOrEmpty()) return "Profile updated."
        // Server returns lowercase names — display a friendly version.
        val labels = updated.map { key ->
            when (key.lowercase()) {
                "name" -> "name"
                "password" -> "password"
                "image" -> "avatar"
                else -> key
            }
        }
        val list = labels.joinToString(", ")
        return "Profile updated ($list)."
    }
}

// ---- Form state shape ----

data class EditProfileFormState(
    val name: String = "",
    val nameError: String? = null,
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val currentPasswordError: String? = null,
    val newPasswordError: String? = null,
    val confirmPasswordError: String? = null,
    /** Compressed image bytes waiting to upload. `null` means "no change". */
    val imageBytes: ByteArray? = null,
    val imageMime: String? = null,
    val imageName: String? = null,
    val imageError: String? = null,
    val isSubmitting: Boolean = false,
    /** "Success" or "Error" banner above the save button. */
    val banner: String? = null,
    /** Internal: track whether the form has been seeded from auth state. */
    val isSeeded: Boolean = false,
) {
    // ByteArray needs a custom equals/hashCode override only because
    // Kotlin's data-class-generated equality does a content compare on
    // ByteArray, which is what we want. Leaving the auto-generated
    // behaviour intact.
}

// ---- Field names the API returns 422 errors under. Must match api.md exactly. ----

private object ApiFields {
    const val NAME = "name"
    const val CURRENT_PASSWORD = "current_password"
    const val NEW_PASSWORD = "new_password"
    const val CONFIRM_PASSWORD = "confirm_password"
    const val IMAGE = "image"
}

private const val NETWORK_BANNER = "Can't reach the server. Check your connection."

// ---- Validation rules — mirror api.md exactly ----

private fun validateName(name: String): String? = when {
    name.length < 2 -> "Name must be at least 2 characters."
    name.length > 150 -> "Name must be 150 characters or fewer."
    else -> null
}

private fun validateNewPassword(pw: String): String? = when {
    pw.length < 8 -> "Password must be at least 8 characters."
    pw.length > 255 -> "Password must be 255 characters or fewer."
    else -> null
}

private fun validateConfirmPassword(pw: String, confirm: String): String? = when {
    confirm != pw -> "Passwords don't match."
    else -> null
}

/**
 * Pulls the first non-empty message from any field we haven't already
 * surfaced — used so a stray 422 like `{"_global": ["..."]}` still
 * reaches the user as a banner instead of being silently dropped.
 */
private fun firstFieldErrorExcluding(
    byField: Map<String, List<String>>,
    exclude: Set<String>,
): String? {
    for ((k, v) in byField) {
        if (k in exclude) continue
        val first = v.firstOrNull() ?: continue
        if (first.isNotBlank()) return first
    }
    return null
}