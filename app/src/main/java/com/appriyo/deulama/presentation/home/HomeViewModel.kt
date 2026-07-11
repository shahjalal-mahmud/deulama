package com.appriyo.deulama.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appriyo.deulama.data.remote.api.HealthApi
import com.appriyo.deulama.domain.model.User
import com.appriyo.deulama.domain.repository.AuthRepository
import com.appriyo.deulama.presentation.components.ConnectionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val status: ConnectionStatus = ConnectionStatus.LOADING,
    val message: String = "Checking backend connection…",
    val user: User? = null,
)

class HomeViewModel(
    private val healthApi: HealthApi,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

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