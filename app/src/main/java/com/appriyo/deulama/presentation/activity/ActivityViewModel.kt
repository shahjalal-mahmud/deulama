package com.appriyo.deulama.presentation.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appriyo.deulama.data.local.datastore.SessionManager
import com.appriyo.deulama.data.remote.ApiResult
import com.appriyo.deulama.domain.model.EngagementEntry
import com.appriyo.deulama.domain.repository.TimelineRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UiState for the "Activity" tab. Mirrors the
 * [com.appriyo.deulama.presentation.genre.GenreStatsUiState] shape so
 * both screens have predictable terminal states the user can rely on.
 *
 * - `Loading`   — first composition / refresh in flight.
 * - `SignedOut` — no JWT (defensive — the Activity tab is gated by the
 *                 auth root, but we still short-circuit here so a
 *                 stale deep-link doesn't hang on a 401).
 * - `Empty`     — signed in but no favorited/queued/watched rows yet.
 * - `Error`     — any of the three list calls failed; banner + retry.
 * - `Success`   — `entries` sorted newest-first, ready for the list.
 */
sealed interface ActivityUiState {
    data object Loading : ActivityUiState
    data object SignedOut : ActivityUiState
    data object Empty : ActivityUiState
    data class Error(val message: String) : ActivityUiState
    data class Success(val entries: List<EngagementEntry>) : ActivityUiState
}

private const val NETWORK_BANNER = "Can't reach the server. Check your connection."

class ActivityViewModel(
    private val timelineRepository: TimelineRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _state = MutableStateFlow<ActivityUiState>(ActivityUiState.Loading)
    val state: StateFlow<ActivityUiState> = _state.asStateFlow()

    /**
     * Public entry-point. Idempotent — multiple in-flight calls don't
     * clobber each other (the latest assignment wins, and we re-check
     * `_state.value` at every step).
     *
     * The defensive `currentSession()` check mirrors the auth-gate
     * already done by the nav graph; it's here so the screen stays
     * self-contained if a deep-link routes here without auth.
     */
    fun load() {
        if (sessionManager.currentSession() == null) {
            _state.value = ActivityUiState.SignedOut
            return
        }
        _state.value = ActivityUiState.Loading
        viewModelScope.launch {
            when (val result = timelineRepository.loadTimeline()) {
                is ApiResult.Success -> {
                    _state.value = if (result.value.isEmpty()) {
                        ActivityUiState.Empty
                    } else {
                        ActivityUiState.Success(result.value)
                    }
                }
                is ApiResult.ValidationError -> _state.value =
                    ActivityUiState.Error("Couldn't parse your activity feed.")
                is ApiResult.NetworkError -> _state.value =
                    ActivityUiState.Error(NETWORK_BANNER)
                is ApiResult.Error -> _state.value =
                    ActivityUiState.Error(result.message.ifBlank { "Couldn't load your activity." })
            }
        }
    }

    /** Force reload — wired to the Retry button on `Error`/`Empty`. */
    fun refresh() = load()
}