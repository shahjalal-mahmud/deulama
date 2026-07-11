package com.appriyo.deulama.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appriyo.deulama.data.remote.api.HealthApi
import com.appriyo.deulama.presentation.components.ConnectionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HealthUiState(
    val status: ConnectionStatus = ConnectionStatus.LOADING,
    val message: String = "Checking backend connection…",
)

class HomeViewModel(private val healthApi: HealthApi) : ViewModel() {

    private val _uiState = MutableStateFlow(HealthUiState())
    val uiState: StateFlow<HealthUiState> = _uiState.asStateFlow()

    init {
        checkHealth()
    }

    fun checkHealth() {
        _uiState.value = HealthUiState(ConnectionStatus.LOADING, "Checking backend connection…")
        viewModelScope.launch {
            try {
                val envelope = healthApi.health()
                val data = envelope.data
                _uiState.value = if (envelope.success && data != null) {
                    HealthUiState(
                        ConnectionStatus.CONNECTED,
                        "Connected — API says: ${data.status}",
                    )
                } else {
                    HealthUiState(
                        ConnectionStatus.ERROR,
                        "Can't reach backend — check your local server is running and the base URL is correct.",
                    )
                }
            } catch (t: Throwable) {
                _uiState.value = HealthUiState(
                    ConnectionStatus.ERROR,
                    "Can't reach backend — check your local server is running and the base URL is correct.",
                )
            }
        }
    }
}