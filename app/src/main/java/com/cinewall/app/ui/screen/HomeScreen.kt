package com.cinewall.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Card
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.cinewall.app.model.MediaItem
import com.cinewall.app.model.MediaType
import com.cinewall.app.repository.MediaRepository
import com.cinewall.app.ui.components.AppHeader
import com.cinewall.app.ui.components.EmptyLibraryState
import com.cinewall.app.ui.components.HeroCarousel
import com.cinewall.app.ui.components.MainTabs
import com.cinewall.app.ui.components.MediaShelf
import com.cinewall.app.ui.components.PrimaryTvButton
import com.cinewall.app.utils.aggregateForDisplay
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    repository: MediaRepository,
    onOpenDetail: (MediaItem) -> Unit,
    onPlay: (MediaItem) -> Unit,
    onNavigate: (String) -> Unit,
    onNavigateToLibrary: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var scanning by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }

    val allItems by repository.getAllMedia().collectAsState(initial = emptyList())
    val movieItems by repository.getMediaByType(MediaType.MOVIE).collectAsState(initial = emptyList())
    val showItems by repository.getMediaByType(MediaType.TV_SHOW).collectAsState(initial = emptyList())
    // 剧集按剧名聚合成单张卡片，每一集不再各占一个位置
    val all = remember(allItems) { allItems.aggregateForDisplay() }
    val movies = remember(movieItems) { movieItems.aggregateForDisplay() }
    val shows = remember(showItems) { showItems.aggregateForDisplay() }
    val folders by remember { mutableStateOf(repository.loadScanFolders()) }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(
        horizontal = 24.dp,
    )) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (all.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyLibraryState(
                        hasFolders = folders.isNotEmpty(),
                        onAddFolder = { onNavigate(MainTabs.SETTINGS) },
                        onScan = {
                            scanning = true
                            scope.launch {
                                val result = repository.scanFolders()
                                status = "扫描完成：${result.totalFound} 部"
                                scanning = false
                            }
                        }
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                ) {
                    val recent = all.sortedByDescending { it.scannedAt }.take(14)
                    val hero = (all.filter { it.backdropPath.isNotBlank() || it.posterPath.isNotBlank() }
                        .ifEmpty { all }).take(8)
                    HeroCarousel(items = hero, onPlay = onPlay, onDetails = onOpenDetail,
                        autoScrollDelay = repository.getAutoScrollDelay())
                    Spacer(Modifier.height(28.dp))
                    MediaShelf(
                        title = "最近添加",
                        items = recent,
                        onOpen = onOpenDetail,
                        onSeeAll = { onNavigateToLibrary("all") }
                    )
                    Spacer(Modifier.height(22.dp))
                    if (movies.isNotEmpty()) {
                        MediaShelf(
                            title = "电影",
                            items = movies.take(14),
                            onOpen = onOpenDetail,
                            onSeeAll = { onNavigateToLibrary("movies") }
                        )
                        Spacer(Modifier.height(22.dp))
                    }
                    if (shows.isNotEmpty()) {
                        MediaShelf(
                            title = "剧集",
                            items = shows.take(14),
                            onOpen = onOpenDetail,
                            onSeeAll = { onNavigateToLibrary("shows") }
                        )
                    }
                    Spacer(Modifier.height(48.dp))
                }
            }
        }

        if (status.isNotBlank()) {
            Text(
                text = status,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 48.dp, bottom = 8.dp)
                    .fillMaxWidth()
            )
        }
    }
}
