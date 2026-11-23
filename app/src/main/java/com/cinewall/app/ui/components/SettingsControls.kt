package com.cinewall.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Switch
import androidx.tv.material3.SwitchDefaults
import androidx.tv.material3.Text
import com.cinewall.app.ui.theme.blend
import com.cinewall.app.ui.theme.onColor

/**
 * TDesign「填充型单选按钮」风格：每个选项都是一块填充块，选中项用主题主色强调，
 * 未选中项用中性表面色，避免整排花哨的彩色按钮。
 */
@Composable
fun <T> FilledRadioGroup(
    items: List<Pair<String, T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    // 外层容器比普通 surface 更实一点，未选中按钮再压暗一档，形成层次。
    val groupContainer = surfaceVariant.copy(alpha = 0.6f)
    val unselectedContainer = surfaceVariant.blend(Color.Black, 0.38f)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(groupContainer)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
            items.forEachIndexed { index, (label, value) ->
                if (index > 0) {
                    Box(
                        modifier = Modifier.fillMaxHeight().width(1.dp)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                    )
                }
                val isSelected = value == selected
                val container = if (isSelected) primary else unselectedContainer
                val content = if (isSelected) onColor(primary) else onSurfaceVariant
                Button(
                    onClick = { onSelect(value) },
                    modifier = Modifier.tvTouchClick { onSelect(value) },
                    shape = ButtonDefaults.shape(shape = RoundedCornerShape(0.dp)),
                    colors = ButtonDefaults.colors(
                        containerColor = container,
                        contentColor = content,
                        focusedContainerColor = container.blend(
                            if (isSelected) Color.White else primary,
                            if (isSelected) 0.16f else 0.3f
                        ),
                        focusedContentColor = content,
                        pressedContainerColor = container.blend(Color.Black, 0.14f),
                        pressedContentColor = content
                    ),
                    glow = ButtonDefaults.glow(
                        focusedGlow = Glow(primary.copy(alpha = 0.35f), 10.dp)
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                }
            }
        }
    }
}

/** 简单布尔配置统一用「文字说明 + 开关」，不再用开/关按钮模拟。 */
@Composable
fun SettingSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    val primary = MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f).tvTouchClick { onCheckedChange(!checked) }
        ) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = onColor(primary),
                checkedTrackColor = primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
            )
        )
    }
}

/** 单选配置统一用「文字说明 + radio 列表」，与 [SettingSwitchRow] 保持一致的卡片外观。 */
@Composable
fun <T> SettingRadioGroupRow(
    label: String,
    items: List<Pair<String, T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Row(
        modifier = modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        FilledRadioGroup(
            items = items,
            selected = selected,
            onSelect = onSelect
        )
    }
}

/**
 * 与 [SettingRadioGroupRow] 同款「文字说明 + 选项」行布局，
 * 但右侧选项由调用方渲染成卡片列表（例如主题配色），而不是 radioGroup。
 */
@Composable
fun <T> SettingCardListRow(
    label: String,
    items: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    itemContent: @Composable RowScope.(item: T, selected: Boolean, onClick: () -> Unit) -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(2.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items.forEach { item ->
                    itemContent(item, item == selected) { onSelect(item) }
                }
            }
        }
    }
}

