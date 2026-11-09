package com.smarthome.launcher

import com.smarthome.launcher.calendar.CalendarEventModel
import com.smarthome.launcher.calendar.CalendarRepository
import com.smarthome.launcher.frigate.FrigateCamera
import com.smarthome.launcher.frigate.FrigateClient
import com.smarthome.launcher.homeassistant.EntityDomain
import com.smarthome.launcher.homeassistant.HaEntity
import com.smarthome.launcher.homeassistant.HomeAssistantClient
import com.smarthome.launcher.screensaver.BingImageClient
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class SmartHomeLauncherUnitTest {

    @Test
    fun testCalendarMockDataIntegrity() {
        val cals = CalendarRepository.getMockCalendars()
        assertEquals(2, cals.size)
        assertTrue(cals.any { it.accountType == "com.google" })
        assertTrue(cals.any { it.accountType == "com.samsung.android" })

        val events = CalendarRepository.getMockEvents()
        assertTrue(events.isNotEmpty())
        for (event in events) {
            assertTrue(event.title.isNotBlank())
            assertTrue(event.endMillis > event.startMillis)
        }
    }

    @Test
    fun testHomeAssistantEntityParsingLogic() {
        val mockJsonStr = """
            [
                {
                    "entity_id": "light.living_room",
                    "state": "on",
                    "attributes": {
                        "friendly_name": "Living Room Light",
                        "brightness": 255
                    }
                },
                {
                    "entity_id": "switch.kitchen_fan",
                    "state": "off",
                    "attributes": {
                        "friendly_name": "Kitchen Fan"
                    }
                },
                {
                    "entity_id": "climate.living_thermostat",
                    "state": "heat",
                    "attributes": {
                        "friendly_name": "Thermostat",
                        "current_temperature": 20.0,
                        "temperature": 22.0
                    }
                }
            ]
        """.trimIndent()

        val jsonArray = JSONArray(mockJsonStr)
        assertEquals(3, jsonArray.length())

        val lightItem = jsonArray.getJSONObject(0)
        val entityId = lightItem.getString("entity_id")
        val state = lightItem.getString("state")
        val attrs = lightItem.getJSONObject("attributes")

        val entity = HaEntity(
            entityId = entityId,
            state = state,
            friendlyName = attrs.getString("friendly_name"),
            domain = EntityDomain.LIGHT,
            brightness = attrs.optInt("brightness")
        )

        assertEquals("light.living_room", entity.entityId)
        assertTrue(entity.isTurnedOn)
        assertEquals(255, entity.brightness)
    }

    @Test
    fun testFrigateUrlBuilder() {
        val client = FrigateClient()
        val snapshot = client.getSnapshotUrl("http://192.168.1.100:5000/", "front_door", 123456789L)
        assertEquals("http://192.168.1.100:5000/api/front_door/latest.jpg?t=123456789", snapshot)

        val webUrl = client.getWebUrl("http://192.168.1.100:5000", "driveway")
        assertEquals("http://192.168.1.100:5000/#cameras/driveway", webUrl)

        val mockCams = FrigateClient.getMockCameras()
        assertTrue(mockCams.size >= 3)
        assertTrue(mockCams.any { it.name == "front_door" })
    }

    @Test
    fun testBingImageJsonExtraction() {
        val bingMockJson = """
            {
                "images": [
                    {
                        "startdate": "20250101",
                        "enddate": "20250102",
                        "url": "/th?id=OHR.SampleBingImage_EN-US1234567890_1920x1080.jpg&rf=LaDigue_1920x1080.jpg",
                        "title": "Stunning Coastline",
                        "copyright": "Sample Nature Photographer (© Getty Images)"
                    }
                ]
            }
        """.trimIndent()

        val json = JSONObject(bingMockJson)
        val images = json.getJSONArray("images")
        val first = images.getJSONObject(0)
        val rawUrl = first.getString("url")
        val fullUrl = if (rawUrl.startsWith("http")) rawUrl else "${BingImageClient.BING_BASE_URL}$rawUrl"

        assertEquals(
            "https://www.bing.com/th?id=OHR.SampleBingImage_EN-US1234567890_1920x1080.jpg&rf=LaDigue_1920x1080.jpg",
            fullUrl
        )
        assertEquals("Stunning Coastline", first.getString("title"))
    }
}
