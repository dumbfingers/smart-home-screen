package com.smarthome.launcher.screensaver

import android.service.dreams.DreamService
import androidx.compose.ui.platform.ComposeView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class BingDailyDreamService : DreamService() {

    private val scope = CoroutineScope(Dispatchers.Main)
    private val client = BingImageClient()

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = false
        isFullscreen = true

        val composeView = ComposeView(this).apply {
            setContent {
                ScreensaverView(
                    bingImage = BingImageClient.FALLBACK_IMAGE,
                    onDismiss = { finish() }
                )
            }
        }

        setContentView(composeView)

        scope.launch {
            val result = client.fetchDailyImage()
            result.onSuccess { bingImage ->
                composeView.setContent {
                    ScreensaverView(
                        bingImage = bingImage,
                        onDismiss = { finish() }
                    )
                }
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        scope.cancel()
    }
}
