package com.smarthome.launcher.frigate

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class FrigateCamera(
    val name: String,
    val displayName: String,
    val isOnline: Boolean = true,
    val fps: Double = 0.0,
    val detectionFps: Double = 0.0
)

class FrigateClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()
) {

    fun getSnapshotUrl(baseUrl: String, cameraName: String, timestamp: Long = System.currentTimeMillis()): String {
        val sanitized = baseUrl.trimEnd('/')
        return "$sanitized/api/$cameraName/latest.jpg?t=$timestamp"
    }

    fun getMjpegStreamUrl(baseUrl: String, cameraName: String): String {
        val sanitized = baseUrl.trimEnd('/')
        return "$sanitized/live/webrtc/api/$cameraName"
    }

    fun getWebUrl(baseUrl: String, cameraName: String? = null): String {
        val sanitized = baseUrl.trimEnd('/')
        return if (cameraName != null) {
            "$sanitized/#cameras/$cameraName"
        } else {
            sanitized
        }
    }

    suspend fun getCameras(baseUrl: String, manualNames: List<String> = emptyList()): Result<List<FrigateCamera>> = withContext(Dispatchers.IO) {
        val sanitized = baseUrl.trimEnd('/')
        if (sanitized.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Frigate URL not configured"))
        }

        // If user manually supplied camera names, use them or verify via stats
        val request = Request.Builder()
            .url("$sanitized/api/stats")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    if (manualNames.isNotEmpty()) {
                        return@withContext Result.success(manualNames.map { name ->
                            FrigateCamera(
                                name = name,
                                displayName = name.replace("_", " ").capitalizeWords()
                            )
                        })
                    }
                    return@withContext Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
                }

                val bodyStr = response.body?.string() ?: "{}"
                val json = JSONObject(bodyStr)
                val cameras = mutableListOf<FrigateCamera>()

                val keys = json.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val obj = json.optJSONObject(key)
                    if (obj != null && (obj.has("camera_fps") || obj.has("process_fps") || obj.has("detection_fps"))) {
                        val camFps = obj.optDouble("camera_fps", 0.0)
                        val detFps = obj.optDouble("detection_fps", 0.0)
                        cameras.add(
                            FrigateCamera(
                                name = key,
                                displayName = key.replace("_", " ").capitalizeWords(),
                                isOnline = camFps > 0.1 || true,
                                fps = camFps,
                                detectionFps = detFps
                            )
                        )
                    }
                }

                if (cameras.isEmpty() && manualNames.isNotEmpty()) {
                    Result.success(manualNames.map { name ->
                        FrigateCamera(
                            name = name,
                            displayName = name.replace("_", " ").capitalizeWords()
                        )
                    })
                } else {
                    Result.success(cameras)
                }
            }
        } catch (e: Exception) {
            if (manualNames.isNotEmpty()) {
                Result.success(manualNames.map { name ->
                    FrigateCamera(
                        name = name,
                        displayName = name.replace("_", " ").capitalizeWords()
                    )
                })
            } else {
                Result.failure(e)
            }
        }
    }

    companion object {
        fun getMockCameras(): List<FrigateCamera> {
            return listOf(
                FrigateCamera("front_door", "Front Door", isOnline = true, fps = 15.0, detectionFps = 5.0),
                FrigateCamera("driveway", "Driveway & Gate", isOnline = true, fps = 20.0, detectionFps = 5.0),
                FrigateCamera("backyard", "Backyard & Patio", isOnline = true, fps = 15.0, detectionFps = 0.0)
            )
        }
    }
}

private fun String.capitalizeWords(): String =
    split(" ").joinToString(" ") { it.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase() else char.toString() } }
