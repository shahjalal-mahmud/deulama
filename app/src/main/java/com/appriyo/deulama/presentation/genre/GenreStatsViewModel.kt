package com.appriyo.deulama.presentation.genre

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appriyo.deulama.data.local.datastore.SessionManager
import com.appriyo.deulama.data.remote.ApiResult
import com.appriyo.deulama.domain.model.GenreStatistics
import com.appriyo.deulama.domain.repository.GenreStatsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UiState for the "Genre breakdown" screen.
 *
 *  - [Loading]            — first fetch in flight.
 *  - [SignedOut]          — endpoint is JWT-protected; signed-out
 *                           users see a "log in" prompt.
 *  - [Empty]              — server returned `statistics: []` (a brand-
 *                           new account, or one with no engagement).
 *  - [Success]            — render the bar chart / ranked list as-is.
 *  - [Error]              — non-2xx with a user-facing message.
 *
 * The screen MUST display the values verbatim — no client-side
 * recomputation, scaling, or sorting (the server has already done
 * all of that per the api.md scoring formula).
 */
sealed interface GenreStatsUiState {
    data object Loading : GenreStatsUiState
    data object SignedOut : GenreStatsUiState
    data object Empty : GenreStatsUiState
    data class Success(val stats: GenreStatistics) : GenreStatsUiState
    data class Error(val message: String) : GenreStatsUiState
}

class GenreStatsViewModel(
    private val genreStatsRepository: GenreStatsRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _state = MutableStateFlow<GenreStatsUiState>(GenreStatsUiState.Loading)
    val state: StateFlow<GenreStatsUiState> = _state.asStateFlow()

    fun load() {
        if (_state.value is GenreStatsUiState.Loading ||
            _state.value is GenreStatsUiState.Error
        ) {
            refresh()
        }
    }

    fun refresh() {
        if (sessionManager.currentSession() == null) {
            _state.value = GenreStatsUiState.SignedOut
            return
        }
        _state.value = GenreStatsUiState.Loading
        viewModelScope.launch {
            when (val result = genreStatsRepository.genreStatistics()) {
                is ApiResult.Success -> {
                    val stats = result.value
                    _state.value = if (stats.statistics.isEmpty()) {
                        GenreStatsUiState.Empty
                    } else {
                        GenreStatsUiState.Success(stats)
                    }
                }
                is ApiResult.Error -> _state.value = GenreStatsUiState.Error(
                    message = result.message.ifBlank { "Couldn't load genre statistics." },
                )
                is ApiResult.ValidationError -> _state.value = GenreStatsUiState.Error(
                    message = result.fieldErrors.values.flatten().firstOrNull()
                        ?: "Couldn't load genre statistics.",
                )
                is ApiResult.NetworkError -> _state.value = GenreStatsUiState.Error(
                    message = "Can't reach the server. Check your connection and try again.",
                )
            }
        }
    }
}
