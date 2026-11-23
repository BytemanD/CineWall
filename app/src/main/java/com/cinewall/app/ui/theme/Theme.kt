package com.cinewall.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.tv.material3.MaterialTheme

/** 0 = 跟随系统, 1 = 明亮, 2 = 夜间 */
enum class ThemeMode(val value: Int) {
    SYSTEM(0), LIGHT(1), DARK(2)
}

/** 供权限状态等处读取当前配色的 success / danger 语义色 */
val LocalThemePalette = compositionLocalOf { ThemePalette.CLASSIC }

/** 使用 Compose for TV 的 MaterialTheme（非手机 Material3） */
@Composable
fun CineWallTheme(
    darkMode: Boolean = isSystemInDarkTheme(),
    paletteId: String = ThemePalette.CLASSIC.id,
    content: @Composable () -> Unit
) {
    val palette = ThemePalette.fromId(paletteId)
    CompositionLocalProvider(LocalThemePalette provides palette) {
        MaterialTheme(
            colorScheme = rememberThemeColorScheme(palette, darkMode),
            typography = CineWallTypography,
            content = content
        )
    }
}
