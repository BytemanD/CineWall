package com.cinewall.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.cinewall.app.model.MediaItem

/** 横向海报货架：标题 + 数量 + 查看全部 + 遥控器可左右移动的海报行 */
@Composable
fun MediaShelf(
    modifier: Modifier = Modifier,
    title: String,
    items: List<MediaItem>,
    onOpen: (MediaItem) -> Unit,
    onSeeAll: (() -> Unit)? = null
) {
    if (items.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title, style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface)

            Spacer(Modifier.weight(1f))
            if (onSeeAll != null) {
                Surface(
                    onClick = onSeeAll,
                    modifier = Modifier.padding(horizontal = 4.dp).tvTouchClick(onSeeAll)
                ) {
                    Text(
                        text = "全部 · ${items.size} →",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
        ) {
            items(items.take(14), key = { it.id }) { item ->
                TvPosterCard(item = item, onClick = { onOpen(item) })
            }
        }
    }
}
