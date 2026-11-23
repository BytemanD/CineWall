package com.cinewall.app.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import com.cinewall.app.ui.theme.blend
import com.cinewall.app.ui.theme.onColor

/** 主/次按钮共用同一套内边距，保证视觉尺寸完全一致。 */
val TvButtonContentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp)

/**
 * TV 风格主按钮：明确使用主题主色作为容器色（tv-material 默认按钮是 surfaceVariant，
 * 和背景太接近），聚焦时提亮并带一圈主色光晕。
 */
@Composable
fun PrimaryTvButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = TvButtonContentPadding
) {
    val primary = MaterialTheme.colorScheme.primary
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.tvTouchClick(onClick),
        shape = ButtonDefaults.shape(shape = RoundedCornerShape(28.dp)),
        colors = ButtonDefaults.colors(
            containerColor = primary,
            contentColor = onColor(primary),
            focusedContainerColor = primary.blend(Color.White, 0.18f),
            focusedContentColor = onColor(primary),
            pressedContainerColor = primary.blend(Color.Black, 0.16f),
            pressedContentColor = onColor(primary)
        ),
        glow = ButtonDefaults.glow(
            focusedGlow = Glow(primary.copy(alpha = 0.5f), 16.dp)
        ),
        contentPadding = contentPadding
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, maxLines = 1)
    }
}

/** 次级按钮：透明底 + 主色文字/描边，聚焦时填充淡主色；尺寸与主按钮一致。 */
@Composable
fun SecondaryTvButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = TvButtonContentPadding
) {
    val primary = MaterialTheme.colorScheme.primary
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.tvTouchClick(onClick),
        shape = ButtonDefaults.shape(shape = RoundedCornerShape(28.dp)),
        colors = ButtonDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = primary,
            focusedContainerColor = primary.copy(alpha = 0.18f),
            focusedContentColor = primary.blend(Color.White, 0.4f),
            pressedContainerColor = primary.copy(alpha = 0.28f),
            pressedContentColor = primary.blend(Color.White, 0.4f)
        ),
        glow = ButtonDefaults.glow(
            focusedGlow = Glow(primary.copy(alpha = 0.3f), 12.dp)
        ),
        contentPadding = contentPadding
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, maxLines = 1)
    }
}
