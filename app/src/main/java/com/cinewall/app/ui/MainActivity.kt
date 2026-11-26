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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LinearProgressIndicator
//import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import com.cinewall.app.di.AppModule
import com.cinewall.app.repository.ConfigRepository
import com.cinewall.app.repository.MediaRepository
import com.cinewall.app.ui.components.CineWallLogo
import com.cinewall.app.ui.navigation.CineWallNavHost
import com.cinewall.app.ui.navigation.LoadingPage
import com.cinewall.app.ui.screen.viewmodels.ConfigViewModel
import com.cinewall.app.ui.theme.CineWallTheme
import com.cinewall.app.ui.theme.ThemeMode
import com.cinewall.app.utils.StoragePermissions
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppModule.init(applicationContext)
        val repository: MediaRepository = AppModule.repository
        val configRespository = ConfigRepository(applicationContext.applicationContext)

        val configViewModel = ConfigViewModel(configRespository)


        setContent {
            val mode = ThemeMode.entries.getOrElse(repository.loadThemeMode()) { ThemeMode.SYSTEM }
            val darkMode = when (mode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            RequestMediaPermissionsIfNeeded()
            var loading by remember { mutableStateOf(true) }
            LaunchedEffect(Unit) {
                configViewModel.load()
//                delay(4000)
                loading = false
            }

            CineWallTheme(
                darkMode = darkMode,
                paletteId = repository.loadThemePalette()
            ) {
                if (loading)
                    LoadingPage()
                else
                    CineWallNavHost(
                        repository = repository,
                        configRepository = configRespository,
                        configViewModel = configViewModel
                    )

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
