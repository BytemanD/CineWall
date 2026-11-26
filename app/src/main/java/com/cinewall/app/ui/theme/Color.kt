package com.cinewall.app.ui.theme

import android.os.Build
import androidx.compose.runtime.Composable
import com.materialkolor.rememberDynamicColorScheme
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.ColorScheme
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import androidx.tv.material3.lightColorScheme

/**
 * 主题配色，移植自 Flutter 项目 lib/core/common/themes.dart。
 * primary / secondary / success / danger 与原项目一一对应。
 */
enum class ThemePalette(
    val id: String,
    val label: String,
    val hint: String,
    val primary: Long,
    val secondary: Long,
    val success: Long,
    val danger: Long
) {
    CLASSIC("classic", "经典", "黑金原色", 0xFFD4AF37, 0xFF8B6F47, 0xFF4CAF50, 0xFFE07373),
    BYZANTIUM("byzantium", "拜占庭", "紫金帝王色", 0xFF4F3A70, 0xFFA87F35, 0xFF7BD3A5, 0xFFE07373),
    BEAN_SAND("beanSand", "豆沙墨", "豆沙粉 & 墨青灰", 0xFFC88697, 0xFF31424D, 0xFF2E7D5B, 0xFFC24E4E),
    PINE_AMBER("pineAmber", "松针琥珀", "松针绿 & 琥珀金", 0xFF365B4C, 0xFFD9A441, 0xFF2E7D5B, 0xFFC24E4E),
    POMEGRANATE("pomegranate", "石榴奶杏", "石榴红 & 奶杏白", 0xFFB5485D, 0xFFC9B2A0, 0xFF2E7D5B, 0xFFC24E4E),
    WISTERIA("wisteria", "藤紫", "紫藤花 & 橘", 0xFF382372, 0xFFFF7200, 0xFF4CAF50, 0xFFE07373);

    companion object {
        fun fromId(id: String?): ThemePalette = entries.find { it.id == id } ?: CLASSIC
    }
}

/** 经典深色：电视场景常用的深背景 + 高对比焦点色 */
val DarkTvColorScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFFD4AF37),
    onPrimary = Color(0xFF1B1608),
    primaryContainer = Color(0xFF3A3117),
    onPrimaryContainer = Color(0xFFF3E4BC),
    secondary = Color(0xFFE9C57E),
    onSecondary = Color(0xFF1B1608),
    secondaryContainer = Color(0xFF2A2413),
    onSecondaryContainer = Color(0xFFF3E4BC),
    tertiary = Color(0xFFA87F35),
    onTertiary = Color(0xFF1B1608),
    tertiaryContainer = Color(0xFF3A2D16),
    onTertiaryContainer = Color(0xFFE8D9B8),
    background = Color(0xFF07080B),
    onBackground = Color(0xFFF5F4F1),
    surface = Color(0xFF0B0C10),
    onSurface = Color(0xFFF5F4F1),
    surfaceVariant = Color(0xFF14161C),
    onSurfaceVariant = Color(0xFFB9B8B3),
    error = Color(0xFFE07373),
    onError = Color(0xFF1B0B0B),
    errorContainer = Color(0xFF5A2626),
    onErrorContainer = Color(0xFFFFDADA),
    border = Color(0xFF3A3D45),
    borderVariant = Color(0xFF24262D),
    scrim = Color(0xB3000000)
)

/** 经典明亮：暖白 + 墨金 */
val LightTvColorScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF8B6F47),
    onPrimary = Color(0xFFFFFDF7),
    primaryContainer = Color(0xFFE8D9B8),
    onPrimaryContainer = Color(0xFF3A2D16),
    secondary = Color(0xFFA87F35),
    onSecondary = Color(0xFFFFFDF7),
    secondaryContainer = Color(0xFFF1E5C8),
    onSecondaryContainer = Color(0xFF3A2D16),
    tertiary = Color(0xFFD4AF37),
    onTertiary = Color(0xFF3A2D16),
    tertiaryContainer = Color(0xFFF5E7C4),
    onTertiaryContainer = Color(0xFF3A2D16),
    background = Color(0xFFF4F0E8),
    onBackground = Color(0xFF1B1A17),
    surface = Color(0xFFFAF7F0),
    onSurface = Color(0xFF1B1A17),
    surfaceVariant = Color(0xFFF0EBE0),
    onSurfaceVariant = Color(0xFF5F5B52),
    error = Color(0xFFB5485D),
    onError = Color(0xFFFFFDF7),
    errorContainer = Color(0xFFF8DADD),
    onErrorContainer = Color(0xFF601B28),
    border = Color(0xFFD8D2C4),
    borderVariant = Color(0xFFE6E0D3),
    scrim = Color(0x99000000)
)

/**
 * 基于主色（seed color）动态生成 Material You 色系，仅覆盖次色 secondary。
 * Android 12+ 使用 rememberDynamicColorScheme；低版本降级为原有手动调色方案。
 */
@Composable
fun rememberThemeColorScheme(
    palette: ThemePalette,
    dark: Boolean
): ColorScheme {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        return legacyThemeColorScheme(palette, dark)
    }

    val seedColor = Color(palette.primary)
    val secondaryColor = Color(palette.secondary)
    val base = if (dark) DarkTvColorScheme else LightTvColorScheme
    // material-kolor 返回的是 Compose Material3 ColorScheme，这里映射到 Compose for TV 的 ColorScheme
    val dynamic = rememberDynamicColorScheme(
        seedColor = seedColor,
        isDark = dark,
        isAmoled = false
    )

    return base.copy(
        primary = dynamic.primary,
        onPrimary = dynamic.onPrimary,
        primaryContainer = dynamic.primaryContainer,
        onPrimaryContainer = dynamic.onPrimaryContainer,
        inversePrimary = dynamic.inversePrimary,
        secondary = secondaryColor,
        onSecondary = onColor(secondaryColor),
        secondaryContainer = dynamic.secondaryContainer,
        onSecondaryContainer = dynamic.onSecondaryContainer,
        tertiary = dynamic.tertiary,
        onTertiary = dynamic.onTertiary,
        tertiaryContainer = dynamic.tertiaryContainer,
        onTertiaryContainer = dynamic.onTertiaryContainer,
        background = dynamic.background,
        onBackground = dynamic.onBackground,
        surface = dynamic.surface,
        onSurface = dynamic.onSurface,
        surfaceVariant = dynamic.surfaceVariant,
        onSurfaceVariant = dynamic.onSurfaceVariant,
        surfaceTint = dynamic.surfaceTint,
        inverseSurface = dynamic.inverseSurface,
        inverseOnSurface = dynamic.inverseOnSurface,
        error = dynamic.error,
        onError = dynamic.onError,
        errorContainer = dynamic.errorContainer,
        onErrorContainer = dynamic.onErrorContainer
    )
}

/** 原有的手动调色方案，作为 API < 31 降级。 */
private fun legacyThemeColorScheme(palette: ThemePalette, dark: Boolean): ColorScheme {
    if (palette == ThemePalette.CLASSIC) {
        return if (dark) DarkTvColorScheme else LightTvColorScheme
    }
    val base = if (dark) DarkTvColorScheme else LightTvColorScheme
    val primary = Color(palette.primary)
    val secondary = Color(palette.secondary)
    val danger = Color(palette.danger)
    return base.copy(
        primary = primary,
        onPrimary = onColor(primary),
        primaryContainer = if (dark) primary.blend(base.surface, 0.74f) else primary.blend(Color.White, 0.86f),
        onPrimaryContainer = if (dark) primary.blend(Color.White, 0.7f) else primary.blend(Color.Black, 0.72f),
        secondary = secondary,
        onSecondary = onColor(secondary),
        secondaryContainer = if (dark) secondary.blend(base.surface, 0.78f) else secondary.blend(Color.White, 0.88f),
        onSecondaryContainer = if (dark) secondary.blend(Color.White, 0.68f) else secondary.blend(Color.Black, 0.7f),
        tertiary = primary.blend(secondary, 0.45f),
        onTertiary = onColor(primary.blend(secondary, 0.45f)),
        tertiaryContainer = if (dark) primary.blend(base.surface, 0.86f) else secondary.blend(Color.White, 0.9f),
        onTertiaryContainer = if (dark) primary.blend(Color.White, 0.6f) else secondary.blend(Color.Black, 0.7f),
        // TV 组件（尤其 Button）默认用 surfaceVariant 作容器色，这里染上主色以形成整体氛围
        surfaceVariant = if (dark) primary.blend(base.surface, 0.84f) else primary.blend(base.surface, 0.9f),
        onSurfaceVariant = if (dark) base.onSurfaceVariant.blend(
            primary,
            0.3f
        ) else base.onSurfaceVariant.blend(primary, 0.35f),
        border = primary.blend(base.surface, if (dark) 0.45f else 0.62f),
        borderVariant = primary.blend(base.surface, if (dark) 0.7f else 0.82f),
        error = danger,
        onError = onColor(danger),
        errorContainer = if (dark) danger.blend(base.surface, 0.75f) else danger.blend(Color.White, 0.86f),
        onErrorContainer = if (dark) danger.blend(Color.White, 0.7f) else danger.blend(Color.Black, 0.72f)
    )
}

/** 按背景明暗返回可读的前景色。 */
fun onColor(background: Color): Color {
    val luminance = 0.299f * background.red + 0.587f * background.green + 0.114f * background.blue
    return if (luminance > 0.55f) Color(0xFF17130A) else Color(0xFFFFFFFF)
}

/** 颜色混合，amount=0 取原色，amount=1 取目标色。 */
fun Color.blend(target: Color, amount: Float): Color {
    val f = amount.coerceIn(0f, 1f)
    return Color(
        red = red + (target.red - red) * f,
        green = green + (target.green - green) * f,
        blue = blue + (target.blue - blue) * f,
        alpha = alpha + (target.alpha - alpha) * f
    )
}

/** 海报/背景图上叠加文字时使用的固定色 */
val ImageScrimStart = Color(0x3307080B)
val ImageScrimEnd = Color(0xFF07080B)
val PosterPlaceholder = Color(0xFF1E2027)
val RatingGold = Color(0xFFF57C00)

//#, #cb34db, #, #45db34
//#3498db, #7734db, #db3498, #db7734, #98db34, #
var COLOR_WARNING = Color(0xFFdb7734)
var COLOR_INFO = Color(0xFF3498db)
var COLOR_SUCCESS = Color(0xFF34db77)
