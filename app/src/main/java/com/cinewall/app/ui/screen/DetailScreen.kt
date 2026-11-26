package com.cinewall.app.ui.screen

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.Card
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.cinewall.app.model.MediaItem
import com.cinewall.app.model.MediaType
import com.cinewall.app.repository.MediaRepository
import com.cinewall.app.scanner.TvShowMatcher
import com.cinewall.app.ui.components.BodyText
import com.cinewall.app.ui.components.DefaultBtn
import com.cinewall.app.ui.components.Description
import com.cinewall.app.ui.components.HeadlineText
import com.cinewall.app.ui.components.IconBtn
import com.cinewall.app.ui.components.LabelText
import com.cinewall.app.ui.components.PrimaryTvButton
import com.cinewall.app.ui.components.SecondaryTvButton
import com.cinewall.app.ui.components.StaticChip
import com.cinewall.app.ui.components.TextSize
import com.cinewall.app.ui.components.TitleText
import com.cinewall.app.ui.components.TvCard
import com.cinewall.app.ui.components.tvTouchClick
import com.cinewall.app.ui.theme.PosterPlaceholder
import com.cinewall.app.ui.theme.RatingGold
import kotlinx.coroutines.launch

@Composable
fun DetailScreen(
    mediaId: String,
    repository: MediaRepository,
    onPlay: (MediaItem) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var item by remember { mutableStateOf<MediaItem?>(null) }
    var selectedSeason by remember { mutableStateOf<Int?>(null) }
    var status by remember { mutableStateOf("") }
    val all by repository.getAllMedia().collectAsState(initial = emptyList())
    // TMDb 返回的每季总集数：seasonNumber -> episodeCount
    var tmdbSeasonCounts by remember { mutableStateOf(emptyMap<Int, Int>()) }

    LaunchedEffect(mediaId) {
        item = repository.getItemById(mediaId)
    }

    // 详情页打开时，如果已刮削（有 TMDb ID），去接口查每季总集数
    LaunchedEffect(item) {
        val tmdbId = item?.externalId ?: 0
        if (tmdbId > 0) {
            scope.launch {
                val counts = repository.getTmdbSeasonCounts(tmdbId)
                tmdbSeasonCounts = counts
            }
        }
    }

    val data = item
    if (data == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("加载中…", style = MaterialTheme.typography.titleLarge)
        }
        return
    }

    val episodes = if (data.mediaType == MediaType.TV_SHOW) {
        val showTitle = TvShowMatcher.showTitle(data.fileName)
        all.filter {
            it.mediaType == MediaType.TV_SHOW &&
                (TvShowMatcher.showTitle(it.fileName) == showTitle || it.displayTitle == showTitle)
        }.sortedWith(compareBy({ it.seasonNumber }, { it.episodeNumber }))
    } else emptyList()

    val seasons = episodes.map { it.seasonNumber }.distinct().sorted()
    val activeSeason = selectedSeason ?: seasons.firstOrNull()
    val visibleEpisodes = if (activeSeason == null) episodes else episodes.filter { it.seasonNumber == activeSeason }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            DetailHero(
                item = data,
                onPlay = { onPlay(data) },
                onScrape = {
                        scope.launch {
                            val result = repository.scrapeItem(data)
                            item = result
                            status = if (result.scraped) "\u300c${result.displayTitle}\u300d\u5267\u5275\u5b8c\u6210" else "\u672a\u627e\u5230\u5339\u914d\u6570\u636e"
                        }
                    },
                onBack = onNavigateBack
            )

            if (data.mediaType == MediaType.TV_SHOW && episodes.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Column(modifier = Modifier.padding(horizontal = 56.dp)) {
                    LabelText("共 ${episodes.size} 集")
//                    Row(verticalAlignment = Alignment.CenterVertically) {
////                        HeadlineText("剧集")
////                        Spacer(Modifier.width(12.dp))
//                    }
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        seasons.forEach { season ->
                            val localCount = episodes.count { it.seasonNumber == season }
                            val tmdbCount = tmdbSeasonCounts[season] ?: 0
                            val label = if (season > 0) {
                                "第 $season 季"
                            } else {
                                "其他 $localCount"
                            }
                            val selectSeason = { selectedSeason = season }
                            if (season == activeSeason) {
                                Button(
                                    onClick = selectSeason,
                                    modifier = Modifier.tvTouchClick(selectSeason)
                                ) { Text(label)
                                    Spacer(Modifier.width(4.dp))
                                    StaticChip("$localCount/$tmdbCount", rounded = true)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = selectSeason,
                                    modifier = Modifier.tvTouchClick(selectSeason)
                                ) { Text(label) }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    visibleEpisodes.forEach { ep ->
                        EpisodeRow(episode = ep, onClick = { onPlay(ep) })
                        Spacer(Modifier.height(8.dp))
                    }
                }
            } else {
                Spacer(Modifier.height(10.dp))
                FileInfoCard(item = data, modifier = Modifier.padding(horizontal = 56.dp))
            }
            Spacer(Modifier.height(56.dp))
        }

        if (status.isNotBlank()) {
            Text(
                text = status,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 56.dp, bottom = 16.dp)
            )
        }
    }
}

@Composable
private fun DetailHero(
    item: MediaItem,
    onPlay: () -> Unit,
    onScrape: () -> Unit,
    onBack: () -> Unit
) {
    val imageUrl = item.backdropPath.takeIf { it.isNotBlank() } ?: item.posterPath.takeIf { it.isNotBlank() }

    Box(modifier = Modifier.fillMaxWidth().height(460.dp)) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            placeholder = ColorPainter(PosterPlaceholder)
        )
        val surfaceColor = MaterialTheme.colorScheme.background
        val onSurface = MaterialTheme.colorScheme.onSurface
        val mutedText = onSurface.copy(alpha = 0.8f)
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

        Row(
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 56.dp, end = 56.dp, bottom = 24.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Box(
                modifier = Modifier.size(width = 220.dp, height = 330.dp).clip(RoundedCornerShape(16.dp))
            ) {
                AsyncImage(
                    model = item.posterPath.takeIf { it.isNotBlank() },
                    contentDescription = item.displayTitle,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    placeholder = ColorPainter(PosterPlaceholder)
                )
            }
            Spacer(Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f).padding(bottom = 8.dp)) {
                StaticChip(if (item.mediaType == MediaType.TV_SHOW) "剧集" else "电影")
                Spacer(Modifier.height(12.dp))
                Text(
                    text = item.displayTitle,
                    color = onSurface,
                    style = MaterialTheme.typography.displaySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (item.voteAverage > 0) {
                        Text("★ ${"%.1f".format(item.voteAverage)}", color = RatingGold)
                    }
                    if (item.year > 0) Text(item.year.toString(), color = mutedText)
                    if (item.genres.isNotBlank()) {
                        Text(item.genres.take(30), color = mutedText, maxLines = 1)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = item.overview.ifBlank { "暂无简介。文件：${item.fileName}" },
                    color = mutedText,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(0.78f)
                )
                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PrimaryTvButton(label = "▶  播放", onClick = onPlay)
                    SecondaryTvButton(label = "重新刮削", onClick = onScrape)
                }
            }
        }

        Box(Modifier.align(Alignment.TopStart).padding(34.dp)) {
            IconBtn(Icons.AutoMirrored.Filled.ArrowBack, onBack)
        }
    }
}

@Composable
private fun EpisodeRow(episode: MediaItem, onClick: () -> Unit) {
    val imageUrl = episode.backdropPath.takeIf { it.isNotBlank() } ?: episode.posterPath.takeIf { it.isNotBlank() }
    val code = if (episode.seasonNumber > 0 && episode.episodeNumber > 0)
        "S${"%02d".format(episode.seasonNumber)}E${"%02d".format(episode.episodeNumber)}"
    else "第 ${episode.episodeNumber} 集"

    TvCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(width = 148.dp, height = 84.dp).clip(RoundedCornerShape(9.dp))) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    placeholder = ColorPainter(PosterPlaceholder)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = code,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = episode.episodeTitle.ifBlank { episode.displayTitle },
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun FileInfoCard(item: MediaItem, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {

        Description(
            modifier = Modifier.padding(22.dp),
            title = "媒体信息",
            items = mapOf(
                "文件名" to item.fileName,
                "路径" to item.filePath,
                "大小" to formatSize(item.fileSize),
                "时长" to formatDuration(item.durationMs),
                "来源" to item.source,
            ),
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.padding(bottom = 8.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(72.dp)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes <= 0 -> "未知"
    bytes >= 1024L * 1024 * 1024 -> "%.2f GB".format(bytes / 1024.0 / 1024 / 1024)
    else -> "%.1f MB".format(bytes / 1024.0 / 1024)
}

private fun formatDuration(ms: Long): String {
    if (ms < 0 ) return "-"
    val minutes = ms / 60000
    return "${minutes / 60} 小时 ${minutes % 60} 分"
}
