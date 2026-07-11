package com.appriyo.deulama.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.appriyo.deulama.data.remote.api.HealthApi
import com.appriyo.deulama.domain.model.Drama
import com.appriyo.deulama.domain.model.User
import com.appriyo.deulama.domain.repository.AuthRepository
import com.appriyo.deulama.domain.repository.DramaRepository
import com.appriyo.deulama.domain.repository.DramaSort
import com.appriyo.deulama.domain.repository.SortOrder
import com.appriyo.deulama.presentation.components.ConnectionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state for the Home tab. Holds:
 *  - the persisted session's user (for the greeting);
 *  - the backend health-check result;
 *  - the cached "Trending now" pager flow (imdb_rating DESC), shared
 *    with the screen via [trendingPaging].
 *
 * No `Loading`/`Success`/`Error` wrapper for the pager — Compose's
 * `collectAsLazyPagingItems` handles that on the UI side.
 */
data class HomeUiState(
    val status: ConnectionStatus = ConnectionStatus.LOADING,
    val message: String = "Checking backend connection…",
    val user: User? = null,
)

class HomeViewModel(
    private val healthApi: HealthApi,
    private val authRepository: AuthRepository,
    dramaRepository: DramaRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /**
     * "Trending now" rail — highest-rated first, descending. The flow
     * is cached on `viewModelScope` so scroll position survives
     * configuration changes.
     */
    val trendingPaging: Flow<PagingData<Drama>> = dramaRepository
        .pagedCatalog(sort = DramaSort.IMDB_RATING, order = SortOrder.DESC)
        .cachedIn(viewModelScope)

    init {
        viewModelScope.launch {
            authRepository.sessionFlow
                .map { it?.user }
                .distinctUntilChanged()
                .collect { user ->
                    _uiState.update { it.copy(user = user) }
                }
        }
        checkHealth()
    }

    fun checkHealth() {
        _uiState.update {
            it.copy(
                status = ConnectionStatus.LOADING,
                message = "Checking backend connection…",
            )
        }
        viewModelScope.launch {
            try {
                val envelope = healthApi.health()
                val data = envelope.data
                _uiState.update {
                    it.copy(
                        status = if (envelope.success && data != null) ConnectionStatus.CONNECTED
                                 else ConnectionStatus.ERROR,
                        message = if (envelope.success && data != null)
                            "Connected — API says: ${data.status}"
                        else
                            "Can't reach backend — check your local server is running and the base URL is correct.",
                    )
                }
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(
                        status = ConnectionStatus.ERROR,
                        message = "Can't reach backend — check your local server is running and the base URL is correct.",
                    )
                }
            }
        }
    }
}
