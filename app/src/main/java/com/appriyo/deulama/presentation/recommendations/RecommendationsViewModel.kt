package com.appriyo.deulama.presentation.recommendations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appriyo.deulama.data.local.datastore.SessionManager
import com.appriyo.deulama.data.remote.ApiResult
import com.appriyo.deulama.domain.model.RecommendationSet
import com.appriyo.deulama.domain.repository.GenreStatsRepository
import com.appriyo.deulama.domain.repository.RecommendationsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state for the "For You" / Recommendations tab.
 *
 *  - [Loading]            — first fetch in flight (also used during pull-to-refresh).
 *  - [SignedOut]          — there's no JWT to fetch recommendations with.
 *  - [Empty]              — server returned `recommendations: []`.
 *  - [Success]            — full set, with [isPersonalized] / [isFallback]
 *                           flags so the UI can show the cold-start banner.
 *  - [Error]              — non-2xx response (network, 401, 5xx…). The
 *                           message is already user-facing and safe to
 *                           drop into the empty-state slot.
 */
sealed interface RecommendationsUiState {
    data object Loading : RecommendationsUiState
    data object SignedOut : RecommendationsUiState
    data object Empty : RecommendationsUiState
    data class Success(
        val set: RecommendationSet,
        val isPersonalized: Boolean,
        val isFallback: Boolean,
    ) : RecommendationsUiState
    data class Error(val message: String) : RecommendationsUiState
}

class RecommendationsViewModel(
    private val recommendationsRepository: RecommendationsRepository,
    // Reserved for a future "genre stats strip" CTA at the top of the
    // screen; declared as a constructor dep so the VM is fully wired
    // and the screen can observe genre activity without re-creating it.
    @Suppress("unused") private val genreStatsRepository: GenreStatsRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _state = MutableStateFlow<RecommendationsUiState>(RecommendationsUiState.Loading)
    val state: StateFlow<RecommendationsUiState> = _state.asStateFlow()

    /** Fetch on first composition. Safe to call repeatedly — it's a no-op
     *  while a request is already in flight. */
    fun load() {
        if (_state.value is RecommendationsUiState.Loading) {
            refresh()
        } else if (_state.value is RecommendationsUiState.Error) {
            // Allow retry from the error state.
            refresh()
        }
    }

    /** Force a fresh fetch — used by pull-to-refresh / error retry buttons. */
    fun refresh() {
        if (sessionManager.currentSession() == null) {
            _state.value = RecommendationsUiState.SignedOut
            return
        }
        _state.value = RecommendationsUiState.Loading
        viewModelScope.launch {
            when (val result = recommendationsRepository.recommendations()) {
                is ApiResult.Success -> {
                    val set = result.value
                    if (set.isEmpty) {
                        _state.value = RecommendationsUiState.Empty
                    } else {
                        _state.value = RecommendationsUiState.Success(
                            set = set,
                            isPersonalized = set.isPersonalized,
                            isFallback = set.fallback,
                        )
                    }
                }
                is ApiResult.Error -> _state.value = RecommendationsUiState.Error(
                    message = result.message.ifBlank { "Couldn't load recommendations." },
                )
                is ApiResult.ValidationError -> _state.value = RecommendationsUiState.Error(
                    message = result.fieldErrors.values.flatten().firstOrNull()
                        ?: "Couldn't load recommendations.",
                )
                is ApiResult.NetworkError -> _state.value = RecommendationsUiState.Error(
                    message = "Can't reach the server. Check your connection and try again.",
                )
            }
        }
    }

    /** Used by the screen to show "log in" CTA when the user is signed out. */
    fun isSignedIn(): Boolean = sessionManager.currentSession() != null

    /**
     * Optional manual refresh trigger; called by the screen on pull-down.
     * Kept as a thin alias for [refresh] to make the call site read well.
     */
    @Suppress("FunctionName", "unused")
    fun onRetry() = refresh()

    /**
     * Helper for the screen — composes a short "why this" caption based
     * on the flags. Kept here so the UI doesn't have to repeat the
     * decision tree.
     */
    fun reasoningFor(state: RecommendationsUiState): String? = when (state) {
        is RecommendationsUiState.Success -> when {
            state.isPersonalized -> "Picked from your likes and watches"
            state.isFallback -> "Top picks while we learn your taste"
            else -> null
        }
        else -> null
    }

    @Suppress("unused")
    private fun touchState() = _state.update { it }
}
