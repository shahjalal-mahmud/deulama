package com.appriyo.deulama.presentation.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appriyo.deulama.data.remote.ApiResult
import com.appriyo.deulama.domain.model.Drama
import com.appriyo.deulama.domain.model.SwipeType
import com.appriyo.deulama.domain.repository.FavoritesRepository
import com.appriyo.deulama.domain.repository.SwipeRepository
import com.appriyo.deulama.domain.repository.WatchLaterRepository
import com.appriyo.deulama.domain.repository.WatchedRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class DeckUiState(
    val isAnimating: Boolean = false,
)

sealed interface DeckEvent {
    data class Error(val message: String) : DeckEvent
}

enum class DeckAction { Like, Dislike, Favorite, WatchLater, Watched }

class SwipeDeckViewModel(
    private val swipeRepository: SwipeRepository,
    private val favoritesRepository: FavoritesRepository,
    private val watchLaterRepository: WatchLaterRepository,
    private val watchedRepository: WatchedRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(DeckUiState())
    val state: StateFlow<DeckUiState> = _state.asStateFlow()

    private val _events = Channel<DeckEvent>(Channel.BUFFERED)
    val events: Flow<DeckEvent> = _events.receiveAsFlow()

    fun onActionConfirmed(action: DeckAction, drama: Drama) {
        if (_state.value.isAnimating) return
        _state.value = _state.value.copy(isAnimating = true)
        viewModelScope.launch {
            val result = dispatchAction(action, drama.dramaId)
            _state.value = _state.value.copy(isAnimating = false)
            if (result !is ApiResult.Success) emitFailure(action, result)
        }
    }

    private suspend fun dispatchAction(action: DeckAction, dramaId: Int): ApiResult<*> =
        when (action) {
            DeckAction.Like -> swipeRepository.recordSwipe(dramaId, SwipeType.LIKE)
            DeckAction.Dislike -> swipeRepository.recordSwipe(dramaId, SwipeType.DISLIKE)
            DeckAction.Favorite -> favoritesRepository.add(dramaId)
            DeckAction.WatchLater -> watchLaterRepository.add(dramaId)
            DeckAction.Watched -> watchedRepository.markWatched(dramaId)
        }

    private fun emitFailure(action: DeckAction, result: ApiResult<*>) {
        val msg = when (result) {
            is ApiResult.ValidationError -> result.fieldErrors.values.flatten().firstOrNull()
                ?: "Couldn't save that action."
            is ApiResult.NetworkError -> "Can't reach the server. Try again when you're back online."
            is ApiResult.Error -> result.message.ifBlank { "Couldn't save that action." }
            else -> "Couldn't save that action."
        }
        val verb = when (action) {
            DeckAction.Like -> "like"
            DeckAction.Dislike -> "dislike"
            DeckAction.Favorite -> "favorite"
            DeckAction.WatchLater -> "watch later"
            DeckAction.Watched -> "watched"
        }
        _events.trySend(DeckEvent.Error("Couldn't save $verb: $msg"))
    }
}