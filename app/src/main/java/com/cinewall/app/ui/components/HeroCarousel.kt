package com.cinewall.app.ui.components

import android.util.Log
import android.view.KeyEvent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions

import androidx.compose.material3.carousel.CarouselItemInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.HorizontalAlignmentLine
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Carousel
import androidx.tv.material3.CarouselState
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Glow
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.tv.material3.rememberCarouselState
import coil.compose.AsyncImage
import com.cinewall.app.model.MediaItem
import com.cinewall.app.model.MediaType
import com.cinewall.app.ui.theme.RatingGold
import java.nio.file.WatchEvent

/** 首页 Hero 轮播：使用 Compose for TV 官方 Carousel */
@Composable
fun HeroCarousel(
    items: List<MediaItem>,
    onPlay: (MediaItem) -> Unit,
    onDetails: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return
    val pagerState = rememberPagerState(pageCount = { items.size })
    HorizontalPager(state = pagerState,
        modifier = modifier.fillMaxWidth().height(460.dp).padding(10.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        pageSpacing = 16.dp

    ) { page ->
        val isCurrentPage = pagerState.currentPage == page
        HeroPage(item = items[page], isCurrentPage = isCurrentPage, onPlay = onPlay, onDetails = onDetails);
    };
    Row(
        modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
    ) {
        CapsuleIndicator(pagerState)
    }
}

@Composable
fun CapsuleIndicator(
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    capsuleWidth: Dp = 14.dp,
    capsuleHeight: Dp = 4.dp,
    spacing: Dp = 8.dp
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(pagerState.pageCount) { index ->
            val isActive = pagerState.currentPage == index
            val width by animateDpAsState(
                targetValue = if (isActive) capsuleWidth * 3 else capsuleWidth,
                animationSpec = tween(300),
                label = "capsuleWidth"
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = spacing / 2)
                    .width(width)
                    .height(capsuleHeight)
                    .clip(RoundedCornerShape(capsuleHeight / 2))
                    .background(MaterialTheme.colorScheme.secondary)
            )
        }
    }
}

@Composable
private fun HeroPage(
    item: MediaItem,
    isCurrentPage: Boolean,
    onPlay: (MediaItem) -> Unit,
    onDetails: (MediaItem) -> Unit
) {
    // 渐变终点跟随主题背景色：深色主题仍是暗色电影感，明亮主题过渡到暖白
    val surfaceColor = MaterialTheme.colorScheme.background
    val onSurface = MaterialTheme.colorScheme.onSurface
    val mutedText = Color.White
    val faintText = onSurface.copy(alpha = 0.55f)

    val imageUrl = item.backdropPath.takeIf { it.isNotBlank() }
        ?: item.posterPath.takeIf { it.isNotBlank() }


    Card(
        scale = CardDefaults.scale(focusedScale = 1.02f),
        onClick = { onPlay(item) },
        modifier = Modifier.fillMaxSize(),
////        shape = RoundedCornerShape(16.dp)
        glow = CardDefaults.glow(
            focusedGlow = if (isCurrentPage) {
                Glow(elevationColor = Color.White, elevation = 16.dp)
            } else Glow.None
//            // 获焦时显示白色光晕，扩散范围 16.dp
//            focusedGlow = Glow(
//                elevationColor = Color.Gray,
//                elevation = 4.dp
//            ),
//            // 按下时光晕稍微收敛
//            pressedGlow = Glow(
//                elevationColor = Color.White.copy(alpha = 0.5f),
//                elevation = 2.dp
//            )
            // 默认状态下不传或传 Glow.None 即可，表示无光晕
        )
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        val isFocused by interactionSource.collectIsFocusedAsState()
//        Box(
//            modifier = Modifier.fillMaxSize().background(
//                Brush.verticalGradient(
//                    listOf(Color.Transparent, surfaceColor.copy(alpha = 0.5f), surfaceColor)
//                )
//            )
//        )
//        Box(
//            modifier = Modifier.fillMaxSize().background(
//                Brush.horizontalGradient(
//                    listOf(surfaceColor.copy(alpha = 0.9f), Color.Transparent)
//                )
//            )
//        )
        Box() {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.linearGradient(
                        listOf(
                            Color.DarkGray,
                            surfaceColor.copy(alpha = 0.3f),
//                            surfaceColor.copy(alpha = 0.1f),
                            Color.Transparent),
//                        start = Offset.Zero,
//                        end = Offset.Infinite,

                        start = Offset(0f, Float.POSITIVE_INFINITY),
                        end = Offset(Float.POSITIVE_INFINITY, 0f)
                    )
                )
            )
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 56.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = item.displayTitle,
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (item.voteAverage > 0) {
                        Text("★ ${"%.1f".format(item.voteAverage)}", color = RatingGold)
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
//                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PrimaryTvButton(label = "▶  立即播放", onClick = { onPlay(item) },
                        modifier = Modifier.focusable(false))
//    //                SecondaryTvButton(label = "详细信息", onClick = { onDetails(item) })
//                }
            }
        }
    }
}
