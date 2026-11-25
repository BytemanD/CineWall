package com.cinewall.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.TextStyle
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.FilterChip
import androidx.tv.material3.FilterChipDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.SelectableChipColors
import androidx.tv.material3.Text


@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ChipGroup(
    items: List<String>,
    defaultSelected: Int = 0,
    onChanged: (Int) -> Unit,
    label: String? = null,
    labelStyle: TextStyle = MaterialTheme.typography.titleMedium,
) {
    var selected by remember { mutableIntStateOf(defaultSelected) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(2.dp),
    ) {
        if (label != null) {
            Text(label, style = labelStyle.copy(color = MaterialTheme.colorScheme.onSurface))
            Spacer(Modifier.width(2.dp))
        }
        items.forEachIndexed { index, label ->
            FilterChip(
                selected = selected == index, // 2. 绑定选中状态
                onClick = {
                    selected = index // 3. 点击时切换状态
                    onChanged(selected)
                },
                modifier = Modifier.tvTouchClick({
                    selected = index // 3. 点击时切换状态
                    onChanged(selected)
                }),
                content = { Text(label) }
            )
            Spacer(Modifier.width(1.dp))
        }
    }
}
