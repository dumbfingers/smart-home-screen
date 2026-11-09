package com.smarthome.launcher.homeassistant

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.smarthome.launcher.settings.AppSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class HaUiState(
    val isConfigured: Boolean = false,
    val isLoading: Boolean = false,
    val entities: List<HaEntity> = emptyList(),
    val errorMessage: String? = null,
    val showWebViewDashboard: Boolean = false
)

class HomeAssistantViewModel(application: Application) : AndroidViewModel(application) {

    private val client = HomeAssistantClient()
    private val settings = AppSettings(application)

    private val _uiState = MutableStateFlow(HaUiState())
    val uiState: StateFlow<HaUiState> = _uiState.asStateFlow()

    init {
        loadData()
        startPeriodicRefresh()
    }

    fun loadData() {
        val configured = settings.isHaConfigured
        _uiState.value = _uiState.value.copy(isConfigured = configured)

        if (!configured) {
            _uiState.value = _uiState.value.copy(
                entities = HomeAssistantClient.getMockEntities(),
                isLoading = false,
                errorMessage = null
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = client.getEntities(settings.haUrl, settings.haToken)
            result.fold(
                onSuccess = { list ->
                    // Filter down to common controllable / glanceable entities
                    val filtered = list.filter {
                        it.domain in listOf(
                            EntityDomain.LIGHT,
                            EntityDomain.SWITCH,
                            EntityDomain.CLIMATE,
                            EntityDomain.SENSOR
                        )
                    }
                    _uiState.value = _uiState.value.copy(
                        entities = if (filtered.isNotEmpty()) filtered else list,
                        isLoading = false,
                        errorMessage = null
                    )
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        entities = HomeAssistantClient.getMockEntities(),
                        isLoading = false,
                        errorMessage = err.message
                    )
                }
            )
        }
    }

    private fun startPeriodicRefresh() {
        viewModelScope.launch {
            while (isActive) {
                delay(30_000L) // Refresh every 30 seconds
                if (settings.isHaConfigured) {
                    val result = client.getEntities(settings.haUrl, settings.haToken)
                    result.onSuccess { list ->
                        val filtered = list.filter {
                            it.domain in listOf(
                                EntityDomain.LIGHT,
                                EntityDomain.SWITCH,
                                EntityDomain.CLIMATE,
                                EntityDomain.SENSOR
                            )
                        }
                        _uiState.value = _uiState.value.copy(entities = filtered)
                    }
                }
            }
        }
    }

    fun toggleEntity(entity: HaEntity) {
        val targetService = if (entity.isTurnedOn) "turn_off" else "turn_on"
        val domain = when (entity.domain) {
            EntityDomain.LIGHT -> "light"
            EntityDomain.SWITCH -> "switch"
            else -> return
        }

        if (!settings.isHaConfigured) {
            // Optimistic update in mock mode
            val updated = _uiState.value.entities.map {
                if (it.entityId == entity.entityId) {
                    it.copy(state = if (it.isTurnedOn) "off" else "on")
                } else it
            }
            _uiState.value = _uiState.value.copy(entities = updated)
            return
        }

        viewModelScope.launch {
            // Optimistic local update
            val current = _uiState.value.entities
            _uiState.value = _uiState.value.copy(
                entities = current.map {
                    if (it.entityId == entity.entityId) {
                        it.copy(state = if (it.isTurnedOn) "off" else "on")
                    } else it
                }
            )

            val res = client.callService(
                baseUrl = settings.haUrl,
                token = settings.haToken,
                domain = domain,
                service = targetService,
                entityId = entity.entityId
            )

            if (res.isFailure) {
                // Revert on failure
                _uiState.value = _uiState.value.copy(entities = current)
            }
        }
    }

    fun setShowWebViewDashboard(show: Boolean) {
        _uiState.value = _uiState.value.copy(showWebViewDashboard = show)
    }

    fun getDashboardUrl(): String = settings.haUrl
}
