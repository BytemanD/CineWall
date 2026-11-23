package com.cinewall.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.cinewall.app.model.MediaItem
import com.cinewall.app.model.MediaType
import com.cinewall.app.ui.theme.PosterPlaceholder
import com.cinewall.app.ui.theme.RatingGold

/** Compose for TV 海报卡片：使用 tv-material Card，焦点时官方自带放大/描边/发光 */
@Composable
fun TvPosterCard(
    item: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 176.dp,
    showMeta: Boolean = true,
    watchFraction: Float = 0f
) {
    val posterHeight = width * 1.42f

    Column(modifier = modifier.width(width), horizontalAlignment = Alignment.Start) {
        Card(
            onClick = onClick,
            modifier = Modifier.width(width).height(posterHeight).tvTouchClick(onClick),
            shape = CardDefaults.shape(shape = RoundedCornerShape(14.dp))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = item.posterPath.takeIf { it.isNotBlank() },
                    contentDescription = item.displayTitle,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    placeholder = ColorPainter(PosterPlaceholder),
                    error = ColorPainter(PosterPlaceholder)
                )

                if (item.voteAverage > 0) {
                    Badge(
                        text = "★ ${"%.1f".format(item.voteAverage)}",
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                    )
                }
                if (item.mediaType == MediaType.TV_SHOW) {
                    Badge(
                        text = if (item.seasonNumber > 0) "S${item.seasonNumber}" else "剧集",
                        modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)
                    )
                }
                if (watchFraction > 0.02f) {
                    LinearProgressIndicator(
                        progress = { watchFraction.coerceIn(0f, 1f) },
                        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(4.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color(0x99000000)
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(
            text = item.displayTitle,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
        if (showMeta) {
            Text(
                text = item.displaySubtitle.ifBlank {
                    if (item.mediaType == MediaType.TV_SHOW) "剧集" else "电影"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun Badge(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xB3000000))
            .padding(horizontal = 9.dp, vertical = 5.dp)
    ) {
        Text(
            text = text,
            color = if (text.startsWith("★")) RatingGold else Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
