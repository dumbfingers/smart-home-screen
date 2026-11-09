package com.smarthome.launcher.calendar

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.CalendarContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.TimeZone

class CalendarRepository(private val context: Context) {

    suspend fun getAvailableCalendars(): List<CalendarModel> = withContext(Dispatchers.IO) {
        val list = mutableListOf<CalendarModel>()
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars.CALENDAR_COLOR,
            CalendarContract.Calendars.IS_PRIMARY
        )

        try {
            val uri: Uri = CalendarContract.Calendars.CONTENT_URI
            val cursor = context.contentResolver.query(
                uri,
                projection,
                CalendarContract.Calendars.VISIBLE + " = 1",
                null,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME + " ASC"
            )

            cursor?.use { c ->
                val idCol = c.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
                val nameCol = c.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
                val accNameCol = c.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME)
                val accTypeCol = c.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_TYPE)
                val colorCol = c.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_COLOR)
                val primaryCol = c.getColumnIndexOrThrow(CalendarContract.Calendars.IS_PRIMARY)

                while (c.moveToNext()) {
                    list.add(
                        CalendarModel(
                            id = c.getLong(idCol),
                            displayName = c.getString(nameCol) ?: "Calendar",
                            accountName = c.getString(accNameCol) ?: "",
                            accountType = c.getString(accTypeCol) ?: "",
                            color = c.getInt(colorCol),
                            isPrimary = c.getInt(primaryCol) == 1
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        list
    }

    suspend fun getEvents(
        startMillis: Long,
        endMillis: Long,
        selectedCalendarIds: Set<Long> = emptySet()
    ): List<CalendarEventModel> = withContext(Dispatchers.IO) {
        val list = mutableListOf<CalendarEventModel>()
        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, startMillis)
        ContentUris.appendId(builder, endMillis)

        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.CALENDAR_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.DESCRIPTION,
            CalendarContract.Instances.EVENT_LOCATION,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.DISPLAY_COLOR,
            CalendarContract.Instances.CALENDAR_DISPLAY_NAME
        )

        val selection = if (selectedCalendarIds.isNotEmpty()) {
            "${CalendarContract.Instances.CALENDAR_ID} IN (${selectedCalendarIds.joinToString(",")})"
        } else {
            null
        }

        try {
            val cursor = context.contentResolver.query(
                builder.build(),
                projection,
                selection,
                null,
                CalendarContract.Instances.BEGIN + " ASC"
            )

            cursor?.use { c ->
                val eventIdCol = c.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_ID)
                val calIdCol = c.getColumnIndexOrThrow(CalendarContract.Instances.CALENDAR_ID)
                val titleCol = c.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
                val descCol = c.getColumnIndexOrThrow(CalendarContract.Instances.DESCRIPTION)
                val locCol = c.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_LOCATION)
                val beginCol = c.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
                val endCol = c.getColumnIndexOrThrow(CalendarContract.Instances.END)
                val allDayCol = c.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)
                val colorCol = c.getColumnIndexOrThrow(CalendarContract.Instances.DISPLAY_COLOR)
                val calNameCol = c.getColumnIndexOrThrow(CalendarContract.Instances.CALENDAR_DISPLAY_NAME)

                while (c.moveToNext()) {
                    list.add(
                        CalendarEventModel(
                            id = c.getLong(eventIdCol),
                            calendarId = c.getLong(calIdCol),
                            title = c.getString(titleCol) ?: "(No title)",
                            description = c.getString(descCol) ?: "",
                            location = c.getString(locCol) ?: "",
                            startMillis = c.getLong(beginCol),
                            endMillis = c.getLong(endCol),
                            allDay = c.getInt(allDayCol) == 1,
                            color = if (!c.isNull(colorCol)) c.getInt(colorCol) else null,
                            calendarDisplayName = c.getString(calNameCol)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        list
    }

    suspend fun createEvent(
        calendarId: Long,
        title: String,
        description: String,
        location: String,
        startMillis: Long,
        endMillis: Long,
        allDay: Boolean = false
    ): Long? = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, title)
            put(CalendarContract.Events.DESCRIPTION, description)
            put(CalendarContract.Events.EVENT_LOCATION, location)
            put(CalendarContract.Events.DTSTART, startMillis)
            put(CalendarContract.Events.DTEND, endMillis)
            put(CalendarContract.Events.ALL_DAY, if (allDay) 1 else 0)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
        }

        try {
            val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            uri?.lastPathSegment?.toLongOrNull()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun updateEvent(
        eventId: Long,
        calendarId: Long,
        title: String,
        description: String,
        location: String,
        startMillis: Long,
        endMillis: Long,
        allDay: Boolean = false
    ): Boolean = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, title)
            put(CalendarContract.Events.DESCRIPTION, description)
            put(CalendarContract.Events.EVENT_LOCATION, location)
            put(CalendarContract.Events.DTSTART, startMillis)
            put(CalendarContract.Events.DTEND, endMillis)
            put(CalendarContract.Events.ALL_DAY, if (allDay) 1 else 0)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
        }

        try {
            val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
            val rows = context.contentResolver.update(uri, values, null, null)
            rows > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun deleteEvent(eventId: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
            val rows = context.contentResolver.delete(uri, null, null)
            rows > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    companion object {
        fun getMockCalendars(): List<CalendarModel> {
            return listOf(
                CalendarModel(1, "Google Calendar (Personal)", "user@gmail.com", "com.google", 0xFF4285F4.toInt(), true),
                CalendarModel(2, "Samsung Calendar (Home)", "Home", "com.samsung.android", 0xFF00C49F.toInt(), false)
            )
        }

        fun getMockEvents(): List<CalendarEventModel> {
            val now = System.currentTimeMillis()
            val hour = 3600_000L
            return listOf(
                CalendarEventModel(
                    id = 101,
                    calendarId = 1,
                    title = "Family Morning Briefing",
                    description = "Review weekly smart home schedule",
                    location = "Living Room",
                    startMillis = now + hour,
                    endMillis = now + (hour * 2),
                    allDay = false,
                    color = 0xFF4285F4.toInt(),
                    calendarDisplayName = "Google Calendar"
                ),
                CalendarEventModel(
                    id = 102,
                    calendarId = 2,
                    title = "Smart Garden Sprinklers",
                    description = "Automated lawn irrigation check",
                    location = "Backyard",
                    startMillis = now + (hour * 4),
                    endMillis = now + (hour * 5),
                    allDay = false,
                    color = 0xFF00C49F.toInt(),
                    calendarDisplayName = "Samsung Calendar"
                ),
                CalendarEventModel(
                    id = 103,
                    calendarId = 1,
                    title = "Kids Evening Activity",
                    description = "Dinner & bedtime routine",
                    location = "Home",
                    startMillis = now + (hour * 8),
                    endMillis = now + (hour * 9),
                    allDay = false,
                    color = 0xFFF59E0B.toInt(),
                    calendarDisplayName = "Family"
                )
            )
        }
    }
}
