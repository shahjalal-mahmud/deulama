package com.appriyo.deulama.presentation.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appriyo.deulama.data.remote.ApiResult
import com.appriyo.deulama.domain.model.Drama
import com.appriyo.deulama.domain.repository.DramaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UiState for the Details screen. Three terminal states plus loading:
 *
 *  - [Loading]            — first fetch in flight.
 *  - [Success]            — drama loaded; render the full layout.
 *  - [NotFound]           — 404 / invalid id; render a back CTA.
 *  - [Error]              — network or 5xx; render a retry CTA.
 *
 * The 422 case (id < 1) is intercepted by the repository and surfaces
 * as [Error] with a field-keyed message — no need to special-case it
 * in the UI.
 */
sealed interface DramaDetailsUiState {
    data object Loading : DramaDetailsUiState
    data class Success(val drama: Drama) : DramaDetailsUiState
    data object NotFound : DramaDetailsUiState
    data class Error(val message: String) : DramaDetailsUiState
}

class DramaDetailsViewModel(
    private val dramaRepository: DramaRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<DramaDetailsUiState>(DramaDetailsUiState.Loading)
    val state: StateFlow<DramaDetailsUiState> = _state.asStateFlow()

    /** Tracks the last id so [retry] can re-fetch without re-pushing args. */
    private var currentId: Int = 0

    /** Called by the screen on first composition. */
    fun load(id: Int) {
        currentId = id
        fetch(id)
    }

    fun retry() {
        if (currentId >= MIN_ID) fetch(currentId)
    }

    private fun fetch(id: Int) {
        _state.update { DramaDetailsUiState.Loading }
        viewModelScope.launch {
            when (val result = dramaRepository.dramaDetails(id)) {
                is ApiResult.Success -> _state.update { DramaDetailsUiState.Success(result.value) }
                is ApiResult.Error -> {
                    if (result.httpStatus == NOT_FOUND_STATUS) {
                        _state.update { DramaDetailsUiState.NotFound }
                    } else {
                        _state.update {
                            DramaDetailsUiState.Error(
                                result.message.ifBlank { "Couldn't load this drama." }
                            )
                        }
                    }
                }
                is ApiResult.ValidationError -> _state.update {
                    DramaDetailsUiState.Error(
                        result.fieldErrors.values.flatten().firstOrNull()
                            ?: "Invalid drama id."
                    )
                }
                is ApiResult.NetworkError -> _state.update {
                    DramaDetailsUiState.Error(
                        "Can't reach the server. Check your connection and try again."
                    )
                }
            }
        }
    }

    companion object {
        /** Per api.md the `id` path param must be `>= 1`. */
        private const val MIN_ID = 1

        /** HTTP 404 — no drama with that id. */
        private const val NOT_FOUND_STATUS = 404
    }
}
