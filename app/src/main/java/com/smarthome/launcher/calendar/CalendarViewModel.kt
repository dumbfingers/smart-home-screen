package com.smarthome.launcher.calendar

import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class CalendarUiState(
    val hasPermission: Boolean = false,
    val calendars: List<CalendarModel> = emptyList(),
    val events: List<CalendarEventModel> = emptyList(),
    val isLoading: Boolean = false,
    val isMockData: Boolean = false,
    val selectedEvent: CalendarEventModel? = null,
    val isEditing: Boolean = false,
    val isCreating: Boolean = false
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    application: Application,
    private val repository: CalendarRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        checkPermissionAndLoad()
    }

    fun checkPermissionAndLoad() {
        val readGranted = ContextCompat.checkSelfPermission(
            getApplication(),
            android.Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED

        val writeGranted = ContextCompat.checkSelfPermission(
            getApplication(),
            android.Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED

        val hasPerms = readGranted && writeGranted
        _uiState.value = _uiState.value.copy(hasPermission = hasPerms)

        if (hasPerms) {
            loadRealCalendarData()
        } else {
            loadMockCalendarData()
        }
    }

    fun loadRealCalendarData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val cals = repository.getAvailableCalendars()

            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            val startMillis = calendar.timeInMillis

            // Load next 7 days of events
            calendar.add(Calendar.DAY_OF_YEAR, 7)
            val endMillis = calendar.timeInMillis

            val evs = repository.getEvents(startMillis, endMillis)
            _uiState.value = _uiState.value.copy(
                calendars = cals,
                events = evs,
                isLoading = false,
                isMockData = false
            )
        }
    }

    private fun loadMockCalendarData() {
        _uiState.value = _uiState.value.copy(
            calendars = CalendarRepository.getMockCalendars(),
            events = CalendarRepository.getMockEvents(),
            isLoading = false,
            isMockData = true
        )
    }

    fun selectEvent(event: CalendarEventModel?) {
        _uiState.value = _uiState.value.copy(selectedEvent = event)
    }

    fun startCreateEvent() {
        _uiState.value = _uiState.value.copy(isCreating = true, selectedEvent = null)
    }

    fun startEditEvent(event: CalendarEventModel) {
        _uiState.value = _uiState.value.copy(isEditing = true, selectedEvent = event)
    }

    fun cancelDialog() {
        _uiState.value = _uiState.value.copy(isCreating = false, isEditing = false, selectedEvent = null)
    }

    fun saveEvent(
        eventId: Long?,
        calendarId: Long,
        title: String,
        description: String,
        location: String,
        startMillis: Long,
        endMillis: Long,
        allDay: Boolean
    ) {
        viewModelScope.launch {
            if (_uiState.value.hasPermission) {
                if (eventId != null) {
                    repository.updateEvent(eventId, calendarId, title, description, location, startMillis, endMillis, allDay)
                } else {
                    repository.createEvent(calendarId, title, description, location, startMillis, endMillis, allDay)
                }
                loadRealCalendarData()
            } else {
                // In demo/mock mode, update state in-memory so user can test UI
                val currentEvents = _uiState.value.events.toMutableList()
                if (eventId != null) {
                    val index = currentEvents.indexOfFirst { it.id == eventId }
                    if (index != -1) {
                        currentEvents[index] = currentEvents[index].copy(
                            title = title,
                            description = description,
                            location = location,
                            startMillis = startMillis,
                            endMillis = endMillis,
                            allDay = allDay
                        )
                    }
                } else {
                    val newId = System.currentTimeMillis()
                    currentEvents.add(
                        CalendarEventModel(
                            id = newId,
                            calendarId = calendarId,
                            title = title,
                            description = description,
                            location = location,
                            startMillis = startMillis,
                            endMillis = endMillis,
                            allDay = allDay,
                            calendarDisplayName = "Local Mock"
                        )
                    )
                }
                _uiState.value = _uiState.value.copy(events = currentEvents.sortedBy { it.startMillis })
            }
            cancelDialog()
        }
    }

    fun deleteEvent(eventId: Long) {
        viewModelScope.launch {
            if (_uiState.value.hasPermission) {
                repository.deleteEvent(eventId)
                loadRealCalendarData()
            } else {
                val updated = _uiState.value.events.filterNot { it.id == eventId }
                _uiState.value = _uiState.value.copy(events = updated)
            }
            cancelDialog()
        }
    }
}
