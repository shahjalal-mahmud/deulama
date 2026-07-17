package com.appriyo.deulama.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appriyo.deulama.data.local.datastore.SessionManager
import com.appriyo.deulama.data.remote.ApiResult
import com.appriyo.deulama.domain.model.UserProfile
import com.appriyo.deulama.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UiState for the Profile screen.
 *
 *  - [Loading]   — first fetch in flight (also shown on Retry).
 *  - [SignedOut] — endpoint is JWT-protected; signed-out users skip the
 *                  body and go straight to the logout action.
 *  - [Success]   — render the full [UserProfile] verbatim — id, name,
 *                  email, image, liked_count, watched_count, favorite_genres.
 *                  The screen MUST display the values verbatim — no
 *                  client-side recomputation, scaling, or sorting
 *                  (the server is the source of truth per api.md).
 *  - [Error]     — non-2xx with a user-facing message; screen offers Retry.
 */
sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data object SignedOut : ProfileUiState
    data class Success(val profile: UserProfile) : ProfileUiState
    data class Error(val message: String) : ProfileUiState
}

/**
 * Loads the authenticated user's profile via
 * `ProfileRepository.getProfile()` (which hits `GET /api/profile`).
 *
 * Pattern mirrors `GenreStatsViewModel` — sealed state, init triggers
 * a fetch, explicit [refresh] for retries.
 */
class ProfileViewModel(
    private val profileRepository: ProfileRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _state = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init {
        // Auto-load on first composition so the user never has to tap
        // "refresh" on first launch.
        refresh()
    }

    /**
     * Re-fetch the profile. Safe to call repeatedly — used by the
     * "Retry" button on the error banner and after the user returns
     * from the Edit Profile screen.
     */
    fun refresh() {
        if (sessionManager.currentSession() == null) {
            _state.value = ProfileUiState.SignedOut
            return
        }
        _state.value = ProfileUiState.Loading
        viewModelScope.launch {
            _state.value = when (val result = profileRepository.getProfile()) {
                is ApiResult.Success -> ProfileUiState.Success(result.value)
                is ApiResult.Error -> ProfileUiState.Error(
                    message = result.message.ifBlank { "Couldn't load profile." },
                )
                is ApiResult.ValidationError -> ProfileUiState.Error(
                    message = result.fieldErrors.values.flatten().firstOrNull()
                        ?: "Couldn't load profile.",
                )
                is ApiResult.NetworkError -> ProfileUiState.Error(
                    message = "Can't reach the server. Check your connection and try again.",
                )
            }
        }
    }
}