package com.cinewall.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.activity.compose.rememberLauncherForActivityResult
import com.cinewall.app.di.AppModule
import com.cinewall.app.repository.MediaRepository
import com.cinewall.app.ui.navigation.CineWallNavHost
import com.cinewall.app.ui.theme.CineWallTheme
import com.cinewall.app.ui.theme.ThemeMode
import com.cinewall.app.utils.StoragePermissions

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppModule.init(applicationContext)
        val repository: MediaRepository = AppModule.repository

        setContent {
            val mode = ThemeMode.entries.getOrElse(repository.loadThemeMode()) { ThemeMode.SYSTEM }
            val darkMode = when (mode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            RequestMediaPermissionsIfNeeded()

            CineWallTheme(
                darkMode = darkMode,
                paletteId = repository.loadThemePalette()
            ) {
                CineWallNavHost(repository = repository)
            }
        }
    }
}

/** 首次启动申请读取媒体文件权限；所有文件访问在设置页引导用户跳转系统设置 */
@Composable
private fun RequestMediaPermissionsIfNeeded() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* 结果会在设置页状态中刷新 */ }

    val needed = remember { !StoragePermissions.hasMediaPermissions(context) }
    LaunchedEffect(needed) {
        if (needed) launcher.launch(StoragePermissions.mediaPermissions())
    }
}
