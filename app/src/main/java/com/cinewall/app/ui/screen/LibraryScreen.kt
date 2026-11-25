package com.cinewall.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
//import androidx.compose.material3.SegmentedButton
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
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.cinewall.app.model.MediaItem
import com.cinewall.app.model.MediaType
import com.cinewall.app.repository.MediaRepository
import com.cinewall.app.ui.components.EmptyLibraryState
import com.cinewall.app.ui.components.ChipGroup
import com.cinewall.app.ui.components.MainTabs
import com.cinewall.app.ui.components.TvPosterCard
import com.cinewall.app.utils.aggregateForDisplay
import kotlinx.coroutines.launch

private val KINDS = listOf("all", "movies", "shows")
private val KIND_LABELS = listOf("全部", "电影", "剧集")
private val SORTS = listOf("最近添加", "标题", "评分", "年份")

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LibraryScreen(
    repository: MediaRepository,
    initialKind: String = "all",
    onOpenDetail: (MediaItem) -> Unit,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var kind by remember(initialKind) {
        mutableStateOf(KINDS.indexOf(initialKind).coerceAtLeast(0))
    }
    var sort by remember { mutableStateOf(0) }

    val allItems by repository.getAllMedia().collectAsState(initial = emptyList())
    val movieItems by repository.getMediaByType(MediaType.MOVIE).collectAsState(initial = emptyList())
    val showItems by repository.getMediaByType(MediaType.TV_SHOW).collectAsState(initial = emptyList())
    // 与首页一致：同一部剧只显示一个卡片
    val all = remember(allItems) { allItems.aggregateForDisplay() }
    val movies = remember(movieItems) { movieItems.aggregateForDisplay() }
    val shows = remember(showItems) { showItems.aggregateForDisplay() }
    val folders = repository.loadScanFolders()

    val source = when (kind) {
        1 -> movies
        2 -> shows
        else -> all
    }
    val items = when (sort) {
        0 -> source.sortedByDescending { it.scannedAt }
        1 -> source.sortedBy { it.displayTitle }
        2 -> source.sortedByDescending { it.voteAverage }
        else -> source.sortedByDescending { it.year }
    }

    Box(modifier = modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)) {
        Column(modifier = modifier.padding(horizontal = 16.dp)) {
            ChipGroup(KIND_LABELS, onChanged = { v -> kind = v }, label = "类别:")
            ChipGroup(SORTS, onChanged = { v -> sort = v }, label = "排序:")

            if (items.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyLibraryState(
                        hasFolders = folders.isNotEmpty(),
                        onAddFolder = { onNavigate(MainTabs.SETTINGS) },
                        onScan = { scope.launch { repository.scanFolders() } },
                        title = "这里还没有内容",
                        description = if (folders.isEmpty())
                            "添加一个媒体目录，CineWall 会自动扫描视频并整理海报墙。"
                        else "没有符合当前筛选条件的条目，试试重新扫描。"
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 148.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(horizontal = 30.dp),
                    contentPadding = PaddingValues(bottom = 56.dp, top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        TvPosterCard(item = item, width = 148.dp, showMeta = false, onClick = { onOpenDetail(item) })
                    }
                }
            }
        }

        Text(
            text = "${items.size} 部",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 48.dp, bottom = 12.dp)
        )
    }
}
