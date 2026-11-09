package com.smarthome.launcher.screensaver

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.smarthome.launcher.settings.AppSettings
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ScreensaverUiState(
    val isScreensaverActive: Boolean = false,
    val dailyImage: BingImage = BingImageClient.FALLBACK_IMAGE,
    val isLoading: Boolean = false,
    val timeoutMillis: Long = 2 * 60 * 1000L
)

class ScreensaverViewModel(application: Application) : AndroidViewModel(application) {

    private val client = BingImageClient()
    private val settings = AppSettings(application)

    private val _uiState = MutableStateFlow(
        ScreensaverUiState(timeoutMillis = settings.screensaverTimeoutMinutes * 60 * 1000L)
    )
    val uiState: StateFlow<ScreensaverUiState> = _uiState.asStateFlow()

    private var idleTimerJob: Job? = null

    init {
        loadBingImage()
        resetIdleTimer()
    }

    fun loadBingImage() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val res = client.fetchDailyImage()
            res.fold(
                onSuccess = { img ->
                    _uiState.value = _uiState.value.copy(dailyImage = img, isLoading = false)
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            )
        }
    }

    fun resetIdleTimer() {
        if (_uiState.value.isScreensaverActive) {
            _uiState.value = _uiState.value.copy(isScreensaverActive = false)
        }

        idleTimerJob?.cancel()

        if (!settings.screensaverEnabled) return

        val timeout = settings.screensaverTimeoutMinutes * 60 * 1000L
        _uiState.value = _uiState.value.copy(timeoutMillis = timeout)

        idleTimerJob = viewModelScope.launch {
            delay(timeout)
            _uiState.value = _uiState.value.copy(isScreensaverActive = true)
        }
    }

    fun dismissScreensaver() {
        _uiState.value = _uiState.value.copy(isScreensaverActive = false)
        resetIdleTimer()
    }

    fun triggerScreensaverNow() {
        _uiState.value = _uiState.value.copy(isScreensaverActive = true)
    }
}
