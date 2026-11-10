package com.smarthome.launcher.homeassistant

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class HomeAssistantClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()
) {

    suspend fun getEntities(baseUrl: String, token: String): Result<List<HaEntity>> = withContext(Dispatchers.IO) {
        val sanitizedUrl = baseUrl.trimEnd('/')
        if (sanitizedUrl.isBlank() || token.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("URL or token not configured"))
        }

        val request = Request.Builder()
            .url("$sanitizedUrl/api/states")
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Content-Type", "application/json")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
                }
                val bodyStr = response.body?.string() ?: "[]"
                val jsonArray = JSONArray(bodyStr)
                val entities = mutableListOf<HaEntity>()

                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(i)
                    val entityId = item.optString("entity_id")
                    val state = item.optString("state")
                    val attrs = item.optJSONObject("attributes") ?: JSONObject()
                    val friendlyName = attrs.optString("friendly_name", entityId)
                    val unit = attrs.optString("unit_of_measurement", null)
                    val icon = attrs.optString("icon", null)
                    val brightness = if (attrs.has("brightness")) attrs.optInt("brightness") else null
                    val currentTemp = if (attrs.has("current_temperature")) attrs.optDouble("current_temperature") else null
                    val targetTemp = if (attrs.has("temperature")) attrs.optDouble("temperature") else null

                    val domain = when {
                        entityId.startsWith("light.") -> EntityDomain.LIGHT
                        entityId.startsWith("switch.") -> EntityDomain.SWITCH
                        entityId.startsWith("climate.") -> EntityDomain.CLIMATE
                        entityId.startsWith("sensor.") -> EntityDomain.SENSOR
                        entityId.startsWith("media_player.") -> EntityDomain.MEDIA_PLAYER
                        entityId.startsWith("camera.") -> EntityDomain.CAMERA
                        else -> EntityDomain.OTHER
                    }

                    entities.add(
                        HaEntity(
                            entityId = entityId,
                            state = state,
                            friendlyName = friendlyName,
                            domain = domain,
                            unitOfMeasurement = unit,
                            icon = icon,
                            brightness = brightness,
                            currentTemperature = currentTemp,
                            targetTemperature = targetTemp
                        )
                    )
                }

                Result.success(entities)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun callService(
        baseUrl: String,
        token: String,
        domain: String,
        service: String,
        entityId: String,
        extraData: Map<String, Any> = emptyMap()
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val sanitizedUrl = baseUrl.trimEnd('/')
        if (sanitizedUrl.isBlank() || token.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("URL or token not configured"))
        }

        val jsonObject = JSONObject().apply {
            put("entity_id", entityId)
            for ((key, value) in extraData) {
                put(key, value)
            }
        }

        val request = Request.Builder()
            .url("$sanitizedUrl/api/services/$domain/$service")
            .addHeader("Authorization", "Bearer $token")
            .post(jsonObject.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(true)
                } else {
                    Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        fun getMockEntities(): List<HaEntity> {
            return listOf(
                HaEntity("light.living_room_ceiling", "on", "Living Room Lights", EntityDomain.LIGHT, brightness = 204),
                HaEntity("light.kitchen_spots", "off", "Kitchen Spots", EntityDomain.LIGHT, brightness = 0),
                HaEntity("light.master_bedroom", "on", "Master Bedroom", EntityDomain.LIGHT, brightness = 150),
                HaEntity("switch.air_purifier", "on", "Living Room Purifier", EntityDomain.SWITCH),
                HaEntity("switch.water_heater", "off", "Water Heater", EntityDomain.SWITCH),
                HaEntity("climate.main_ac", "cool", "Main AC", EntityDomain.CLIMATE, currentTemperature = 22.5, targetTemperature = 21.0),
                HaEntity("sensor.outdoor_temperature", "24.5", "Balcony Temp", EntityDomain.SENSOR, unitOfMeasurement = "°C"),
                HaEntity("sensor.indoor_humidity", "48", "Living Humidity", EntityDomain.SENSOR, unitOfMeasurement = "%")
            )
        }
    }
}
