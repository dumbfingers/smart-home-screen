package com.smarthome.launcher.calendar

data class CalendarModel(
    val id: Long,
    val displayName: String,
    val accountName: String,
    val accountType: String,
    val color: Int,
    val isPrimary: Boolean = false
)

data class CalendarEventModel(
    val id: Long,
    val calendarId: Long,
    val title: String,
    val description: String = "",
    val location: String = "",
    val startMillis: Long,
    val endMillis: Long,
    val allDay: Boolean = false,
    val color: Int? = null,
    val calendarDisplayName: String? = null
)
