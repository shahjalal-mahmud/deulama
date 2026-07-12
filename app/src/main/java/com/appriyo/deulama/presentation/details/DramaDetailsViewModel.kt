package com.appriyo.deulama.presentation.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appriyo.deulama.data.remote.ApiResult
import com.appriyo.deulama.domain.model.Drama
import com.appriyo.deulama.domain.repository.DramaRepository
import com.appriyo.deulama.domain.repository.FavoritesRepository
import com.appriyo.deulama.domain.repository.WatchLaterRepository
import com.appriyo.deulama.domain.repository.WatchedRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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

/**
 * One-shot events the screen surfaces as snackbars. Kept separate
 * from [DramaDetailsUiState] so the message doesn't re-show on every
 * recomposition.
 */
sealed interface DramaDetailsEvent {
    data class Info(val message: String) : DramaDetailsEvent
}

class DramaDetailsViewModel(
    private val dramaRepository: DramaRepository,
    private val favoritesRepository: FavoritesRepository,
    private val watchLaterRepository: WatchLaterRepository,
    private val watchedRepository: WatchedRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<DramaDetailsUiState>(DramaDetailsUiState.Loading)
    val state: StateFlow<DramaDetailsUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<DramaDetailsEvent>(
        extraBufferCapacity = 4,
    )
    val events: SharedFlow<DramaDetailsEvent> = _events.asSharedFlow()

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

    // ---- Engagement actions (wired to the bottom action bar) ----

    fun toggleFavorite() = runForCurrent { dramaId ->
        val currentlyFav = favoritesRepository.isFavorited(dramaId).first()
        val result = if (currentlyFav) {
            favoritesRepository.remove(dramaId)
        } else {
            favoritesRepository.add(dramaId)
        }
        if (result is ApiResult.Success) {
            _events.tryEmit(
                DramaDetailsEvent.Info(
                    if (currentlyFav) "Removed from favorites" else "Added to favorites"
                ),
            )
        }
    }

    fun toggleWatchLater() = runForCurrent { dramaId ->
        val currentlyQueued = watchLaterRepository.isQueued(dramaId).first()
        val result = if (currentlyQueued) {
            watchLaterRepository.remove(dramaId)
        } else {
            watchLaterRepository.add(dramaId)
        }
        if (result is ApiResult.Success) {
            _events.tryEmit(
                DramaDetailsEvent.Info(
                    if (currentlyQueued) "Removed from watch later" else "Added to watch later"
                ),
            )
        }
    }

    fun markWatched() = runForCurrent { dramaId ->
        val result = watchedRepository.markWatched(dramaId)
        if (result is ApiResult.Success) {
            _events.tryEmit(DramaDetailsEvent.Info("Marked as watched"))
        }
    }

    private fun runForCurrent(block: suspend (Int) -> Unit) {
        val id = currentId
        if (id < MIN_ID) return
        viewModelScope.launch { block(id) }
    }

    companion object {
        /** Per api.md the `id` path param must be `>= 1`. */
        private const val MIN_ID = 1

        /** HTTP 404 — no drama with that id. */
        private const val NOT_FOUND_STATUS = 404
    }
}
