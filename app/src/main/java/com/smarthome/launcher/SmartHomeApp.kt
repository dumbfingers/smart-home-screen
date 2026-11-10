package com.smarthome.launcher

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SmartHomeApp : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
