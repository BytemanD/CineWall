package com.cinewall.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Carousel
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.tv.material3.rememberCarouselState
import coil.compose.AsyncImage
import com.cinewall.app.model.MediaItem
import com.cinewall.app.model.MediaType

/** 首页 Hero 轮播：使用 Compose for TV 官方 Carousel */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HeroCarousel(
    items: List<MediaItem>,
    onPlay: (MediaItem) -> Unit,
    onDetails: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return
    val carouselState = rememberCarouselState(0)

    Carousel(
        itemCount = items.size,
        carouselState = carouselState,
        modifier = modifier.fillMaxWidth().height(460.dp),
        carouselIndicator = {},
        content = { index ->
            HeroPage(item = items[index], onPlay = onPlay, onDetails = onDetails)
        }
    )
}

@Composable
private fun HeroPage(
    item: MediaItem,
    onPlay: (MediaItem) -> Unit,
    onDetails: (MediaItem) -> Unit
) {
    val imageUrl = item.backdropPath.takeIf { it.isNotBlank() }
        ?: item.posterPath.takeIf { it.isNotBlank() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        // 渐变终点跟随主题背景色：深色主题仍是暗色电影感，明亮主题过渡到暖白
        val surfaceColor = MaterialTheme.colorScheme.background
        val onSurface = MaterialTheme.colorScheme.onSurface
        val mutedText = onSurface.copy(alpha = 0.78f)
        val faintText = onSurface.copy(alpha = 0.55f)

        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, surfaceColor.copy(alpha = 0.5f), surfaceColor)
                )
            )
        )
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.horizontalGradient(
                    listOf(surfaceColor.copy(alpha = 0.9f), Color.Transparent)
                )
            )
        )

        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 56.dp, bottom = 48.dp, end = 64.dp)
        ) {
            // 主色短横：TV 首页常见的强调线，强化当前配色存在感
            Box(
                Modifier.width(72.dp).height(6.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(3.dp))
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = item.displayTitle,
                style = MaterialTheme.typography.displaySmall,
                color = onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (item.voteAverage > 0) {
                    Text("★ ${"%.1f".format(item.voteAverage)}", color = MaterialTheme.colorScheme.primary)
                }
                if (item.year > 0) {
                    Text("·", color = faintText)
                    Text(item.year.toString(), color = mutedText)
                }
                Text("·", color = faintText)
                Text(
                    if (item.mediaType == MediaType.TV_SHOW) "剧集" else "电影",
                    color = mutedText
                )
                if (item.genres.isNotBlank()) {
                    Text("·", color = faintText)
                    Text(item.genres.take(28), color = mutedText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = item.overview.ifBlank { "暂无简介，文件：${item.fileName}" },
                style = MaterialTheme.typography.bodyLarge,
                color = mutedText,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(0.62f)
            )
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PrimaryTvButton(label = "▶  立即播放", onClick = { onPlay(item) })
                SecondaryTvButton(label = "详细信息", onClick = { onDetails(item) })
            }
        }
    }
}
