package com.cinewall.app.ui.player

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.cinewall.app.di.AppModule
import com.cinewall.app.ui.theme.CineWallTheme

class PlayerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!AppModule.isInitialized) AppModule.init(applicationContext)
        val mediaId = intent.getStringExtra("media_id") ?: ""

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.setBackgroundDrawableResource(android.R.color.black)
        hideSystemBars()

        setContent {
            CineWallTheme(
                darkMode = true,
                paletteId = AppModule.repository.loadThemePalette()
            ) {
                PlayerScreen(
                    mediaId = mediaId,
                    repository = AppModule.repository,
                    onNavigateBack = { finish() },
                    onBrightnessChanged = { value ->
                        window.attributes = window.attributes.apply {
                            screenBrightness = value.coerceIn(0.05f, 1f)
                        }
                    }
                )
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}
