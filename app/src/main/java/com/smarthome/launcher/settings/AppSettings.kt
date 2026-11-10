package com.smarthome.launcher.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class AppSettings(context: Context) {
    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "smart_home_launcher_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        // Fallback for mock/test environments without Android Keystore
        context.getSharedPreferences("smart_home_launcher_prefs", Context.MODE_PRIVATE)
    }

    var haUrl: String
        get() = prefs.getString("ha_url", "") ?: ""
        set(value) = prefs.edit().putString("ha_url", value.trim()).apply()

    var haToken: String
        get() = prefs.getString("ha_token", "") ?: ""
        set(value) = prefs.edit().putString("ha_token", value.trim()).apply()

    var frigateUrl: String
        get() = prefs.getString("frigate_url", "") ?: ""
        set(value) = prefs.edit().putString("frigate_url", value.trim()).apply()

    var frigateCameras: String
        get() = prefs.getString("frigate_cameras", "front_door,driveway,backyard") ?: "front_door,driveway,backyard"
        set(value) = prefs.edit().putString("frigate_cameras", value.trim()).apply()

    var screensaverTimeoutMinutes: Int
        get() = prefs.getInt("screensaver_timeout_minutes", 2)
        set(value) = prefs.edit().putInt("screensaver_timeout_minutes", value).apply()

    var screensaverEnabled: Boolean
        get() = prefs.getBoolean("screensaver_enabled", true)
        set(value) = prefs.edit().putBoolean("screensaver_enabled", value).apply()

    val isHaConfigured: Boolean
        get() = haUrl.isNotBlank() && haToken.isNotBlank()

    val isFrigateConfigured: Boolean
        get() = frigateUrl.isNotBlank()
}
