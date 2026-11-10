package com.smarthome.launcher.frigate

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.smarthome.launcher.settings.AppSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FrigateUiState(
    val isConfigured: Boolean = false,
    val isLoading: Boolean = false,
    val cameras: List<FrigateCamera> = emptyList(),
    val errorMessage: String? = null,
    val snapshotTimestamp: Long = System.currentTimeMillis(),
    val selectedCameraForLiveView: FrigateCamera? = null
)

@HiltViewModel
class FrigateViewModel @Inject constructor(
    application: Application,
    private val client: FrigateClient,
    private val settings: AppSettings
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(FrigateUiState())
    val uiState: StateFlow<FrigateUiState> = _uiState.asStateFlow()

    init {
        loadData()
        startSnapshotRefreshLoop()
    }

    fun loadData() {
        val configured = settings.isFrigateConfigured
        _uiState.value = _uiState.value.copy(isConfigured = configured)

        if (!configured) {
            _uiState.value = _uiState.value.copy(
                cameras = FrigateClient.getMockCameras(),
                isLoading = false,
                errorMessage = null
            )
            return
        }

        val manualList = settings.frigateCameras
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = client.getCameras(settings.frigateUrl, manualList)
            result.fold(
                onSuccess = { cams ->
                    _uiState.value = _uiState.value.copy(
                        cameras = cams,
                        isLoading = false,
                        errorMessage = null
                    )
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        cameras = FrigateClient.getMockCameras(),
                        isLoading = false,
                        errorMessage = err.message
                    )
                }
            )
        }
    }

    private fun startSnapshotRefreshLoop() {
        viewModelScope.launch {
            while (isActive) {
                delay(3000L) // Refresh camera snapshots every 3 seconds
                _uiState.value = _uiState.value.copy(snapshotTimestamp = System.currentTimeMillis())
            }
        }
    }

    fun getSnapshotUrl(camera: FrigateCamera): String {
        return if (settings.isFrigateConfigured) {
            client.getSnapshotUrl(settings.frigateUrl, camera.name, _uiState.value.snapshotTimestamp)
        } else {
            // High quality architectural smart home camera placeholder
            "https://picsum.photos/seed/${camera.name}/640/360"
        }
    }

    fun getLiveStreamUrl(camera: FrigateCamera): String {
        return if (settings.isFrigateConfigured) {
            client.getWebUrl(settings.frigateUrl, camera.name)
        } else {
            "https://picsum.photos/seed/${camera.name}/1280/720"
        }
    }

    fun selectCamera(camera: FrigateCamera?) {
        _uiState.value = _uiState.value.copy(selectedCameraForLiveView = camera)
    }
}
