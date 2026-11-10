package com.smarthome.launcher.screensaver

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class BingImageClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        const val BING_API_URL = "https://www.bing.com/HPImageArchive.aspx?format=js&idx=0&n=1&mkt=en-US"
        const val BING_BASE_URL = "https://www.bing.com"

        val FALLBACK_IMAGE = BingImage(
            url = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=1920&q=80",
            title = "Yosemite National Park",
            copyright = "National Park Service / Bing Daily Wallpaper",
            date = "Today"
        )
    }

    suspend fun fetchDailyImage(): Result<BingImage> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(BING_API_URL)
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
                }

                val bodyStr = response.body?.string() ?: ""
                val json = JSONObject(bodyStr)
                val imagesArray = json.optJSONArray("images")
                if (imagesArray != null && imagesArray.length() > 0) {
                    val obj = imagesArray.getJSONObject(0)
                    val rawUrl = obj.optString("url")
                    val fullUrl = if (rawUrl.startsWith("http")) rawUrl else "$BING_BASE_URL$rawUrl"
                    val title = obj.optString("title", "Daily Wallpaper")
                    val copyright = obj.optString("copyright", "Microsoft Bing")
                    val date = obj.optString("enddate", "")

                    Result.success(
                        BingImage(
                            url = fullUrl,
                            title = title,
                            copyright = copyright,
                            date = date
                        )
                    )
                } else {
                    Result.failure(Exception("No image array in Bing response"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
