@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.cinewall.app.ui.player

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.IconButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Tab
import androidx.tv.material3.TabRow
import androidx.tv.material3.Text
import com.cinewall.app.model.PlaybackSettings
import com.cinewall.app.ui.components.tvTouchClick
import com.cinewall.app.ui.theme.onColor
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 全屏手势层：
 * - 左右半屏上下拖动 → 亮度 / 音量
 * - 中间横向拖动 → 预览快进/快退，松手才真正跳转，拖动期间上方显示时间提示
 * - 单击显示/隐藏控制条，双击中间暂停、双击左右 ±步长
 * - 遥控器菜单键打开配置面板
 */
@Composable
fun PlayerGestures(
    enabled: Boolean,
    seekRangeMs: Long,
    onTap: () -> Unit,
    onDoubleTap: (Float) -> Unit,
    onMenu: () -> Unit,
    onBrightnessDelta: (Float) -> Unit,
    onVolumeDelta: (Float) -> Unit,
    onSeekStart: () -> Unit,
    onSeekPreview: (Long) -> Unit,
    onSeekEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentTap by rememberUpdatedState(onTap)
    val currentDoubleTap by rememberUpdatedState(onDoubleTap)
    val currentMenu by rememberUpdatedState(onMenu)
    val currentBrightness by rememberUpdatedState(onBrightnessDelta)
    val currentVolume by rememberUpdatedState(onVolumeDelta)
    val currentSeekStart by rememberUpdatedState(onSeekStart)
    val currentSeekPreview by rememberUpdatedState(onSeekPreview)
    val currentSeekEnd by rememberUpdatedState(onSeekEnd)
    val currentSeekRange by rememberUpdatedState(seekRangeMs)

    Box(
        modifier = modifier.fillMaxSize().focusable().onKeyEvent { event ->
            if (!enabled || event.type != KeyEventType.KeyDown) return@onKeyEvent false
            when (event.key.keyCode.toInt()) {
                KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_INFO -> {
                    currentMenu()
                    true
                }
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                    currentDoubleTap(0.5f)
                    true
                }
                KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    currentDoubleTap(0.9f)
                    true
                }
                KeyEvent.KEYCODE_MEDIA_REWIND, KeyEvent.KEYCODE_DPAD_LEFT -> {
                    currentDoubleTap(0.1f)
                    true
                }
                else -> false
            }
        }.pointerInput(enabled) {
            if (!enabled) return@pointerInput
            val slop = viewConfiguration.touchSlop
            var lastTapTime = 0L
            var lastTapPos = Offset.Zero

            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val pointerId = down.id
                val start = down.position
                var last = start
                // 0=未决定 1=忽略 2=横向拖动(seek) 3=左侧亮度 4=右侧音量
                var mode = 0
                var seekAccum = 0f
                val leftEdge = size.width * 0.32f
                val rightEdge = size.width * 0.68f
                val totalMs = currentSeekRange.takeIf { it > 0 } ?: 60_000L

                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                    val pos = change.position
                    val dxTotal = pos.x - start.x
                    val dyTotal = pos.y - start.y

                    if (mode == 0 && (abs(dxTotal) > slop || abs(dyTotal) > slop)) {
                        mode = when {
                            abs(dxTotal) >= abs(dyTotal) -> 2
                            start.x < leftEdge -> 3
                            start.x > rightEdge -> 4
                            else -> 1
                        }
                        if (mode == 2) currentSeekStart()
                    }

                    when (mode) {
                        2 -> {
                            seekAccum += pos.x - last.x
                            currentSeekPreview((seekAccum / size.width * totalMs).toLong())
                        }
                        3 -> currentBrightness(-(pos.y - last.y) / size.height * 1.6f)
                        4 -> currentVolume(-(pos.y - last.y) / size.height * 1.6f)
                    }

                    last = pos
                    change.consume()

                    if (!change.pressed) {
                        if (mode == 2) currentSeekEnd()
                        val moved = abs(dxTotal) > slop || abs(dyTotal) > slop
                        if (!moved) {
                            val now = System.currentTimeMillis()
                            if (now - lastTapTime < 260L && (pos - lastTapPos).getDistance() < 80f) {
                                currentDoubleTap(pos.x / size.width)
                                lastTapTime = 0L
                            } else {
                                lastTapTime = now
                                lastTapPos = pos
                                currentTap()
                            }
                        }
                        break
                    }
                }
            }
        }
    )
}

/** 半透明玻璃按钮，用于面板选项。 */
@Composable
fun GlassButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false
) {
    val primary = MaterialTheme.colorScheme.primary
    Button(
        onClick = onClick,
        modifier = modifier.tvTouchClick(onClick),
        shape = ButtonDefaults.shape(shape = RoundedCornerShape(20.dp)),
        colors = ButtonDefaults.colors(
            containerColor = if (active) primary else Color.Black.copy(alpha = 0.45f),
            contentColor = if (active) onColor(primary) else Color.White,
            focusedContainerColor = primary.copy(alpha = 0.88f),
            focusedContentColor = onColor(primary),
            pressedContainerColor = primary.copy(alpha = 0.72f),
            pressedContentColor = onColor(primary)
        ),
        glow = ButtonDefaults.glow(focusedGlow = Glow(primary.copy(alpha = 0.45f), 12.dp)),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

/** 纯图标的半透明按钮，用于左上角返回与右上角配置入口。 */
@Composable
private fun GlassIconButton(
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    IconButton(
        onClick = onClick,
        modifier = modifier.tvTouchClick(onClick),
        colors = IconButtonDefaults.colors(
            containerColor = Color.Black.copy(alpha = 0.45f),
            contentColor = Color.White,
            focusedContainerColor = primary.copy(alpha = 0.88f),
            focusedContentColor = onColor(primary),
            pressedContainerColor = primary.copy(alpha = 0.72f),
            pressedContentColor = onColor(primary)
        ),
        glow = ButtonDefaults.glow(focusedGlow = Glow(primary.copy(alpha = 0.45f), 12.dp))
    ) {
        Icon(imageVector = imageVector, contentDescription = contentDescription)
    }
}

/** 横向拖动时显示在视频上方的「前进 / 后退」提示。 */
@Composable
fun SeekHud(text: String, modifier: Modifier = Modifier) {
    if (text.isBlank()) return
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(
            Modifier.padding(top = 100.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Black.copy(alpha = 0.78f))
                .padding(horizontal = 28.dp, vertical = 14.dp)
        ) {
            Text(text, style = MaterialTheme.typography.titleLarge, color = Color.White)
        }
    }
}

/** 控制条 + 底部快捷操作栏。 */
@Composable
fun PlayerControls(
    title: String,
    position: Long,
    duration: Long,
    isPlaying: Boolean,
    scaleMode: ScaleMode,
    brightness: Float,
    volume: Float,
    toast: String,
    onBack: () -> Unit,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onOpenPanel: () -> Unit,
    onCycleScale: () -> Unit,
    onSeekFraction: (Float) -> Unit,
    onKeySeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier.fillMaxSize()) {
        // 顶部渐变遮罩
        Box(
            Modifier.fillMaxWidth().height(120.dp).align(Alignment.TopCenter).background(
                Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.78f), Color.Transparent))
            )
        )
        Row(
            modifier = Modifier.align(Alignment.TopStart).fillMaxWidth().padding(28.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GlassIconButton(Icons.Filled.ArrowBack, "返回", onBack)
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            GlassIconButton(Icons.Filled.Settings, "播放设置", onOpenPanel)
        }

        // 底部渐变遮罩 + 进度条 + 快捷按钮
        Box(
            Modifier.fillMaxWidth().height(200.dp).align(Alignment.BottomCenter).background(
                Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)))
            )
        )
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${formatTime(position)} / ${formatTime(duration)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "亮度 ${(brightness * 100).roundToInt()} · 音量 ${(volume * 100).roundToInt()}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.75f)
                )
            }

            SeekBar(
                position = position,
                duration = duration,
                onSeekFraction = onSeekFraction,
                onKeySeek = onKeySeek,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GlassButton(if (isPlaying) "❚❚ 暂停" else "▶ 播放", onTogglePlay)
                GlassButton("⏮ 上一集", onPrevious)
                GlassButton("⏭ 下一集", onNext)
                Spacer(Modifier.weight(1f))
                GlassButton("画面：${scaleMode.label}", onCycleScale)
            }
        }

        if (toast.isNotBlank()) {
            Box(
                modifier = Modifier.align(Alignment.Center)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 28.dp, vertical = 14.dp)
            ) {
                Text(toast, style = MaterialTheme.typography.titleLarge, color = Color.White)
            }
        }
    }
}

/** 可点击/拖动，也支持遥控器左右键快进退的进度条（高度减半）。 */
@Composable
private fun SeekBar(
    position: Long,
    duration: Long,
    onSeekFraction: (Float) -> Unit,
    onKeySeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val fraction = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
    Box(
        modifier = modifier.height(13.dp).clip(RoundedCornerShape(7.dp))
            .background(Color.White.copy(alpha = 0.24f))
            .focusable()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key.keyCode.toInt()) {
                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        onKeySeek(-5000L); true
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        onKeySeek(5000L); true
                    }
                    else -> false
                }
            }
            .pointerInput(duration) {
                detectDragGestures(
                    onDragStart = { onSeekFraction((it.x / size.width.toFloat()).coerceIn(0f, 1f)) },
                    onDrag = { change, _ ->
                        onSeekFraction((change.position.x / size.width.toFloat()).coerceIn(0f, 1f))
                    }
                )
            }
    ) {
        Box(
            Modifier.fillMaxWidth(fraction).fillMaxHeight()
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}

/** 右侧滑出的配置面板，按「播放 / 画面声音 / 字幕音轨」三个 Tab 分类。 */
@Composable
fun SettingsPanel(
    settings: PlaybackSettings,
    textTracks: List<TrackOption>,
    audioTracks: List<TrackOption>,
    brightness: Float,
    volume: Float,
    onClose: () -> Unit,
    onSpeed: (Float) -> Unit,
    onSeekStep: (Int) -> Unit,
    onScaleMode: (ScaleMode) -> Unit,
    onTextTrack: (TrackOption) -> Unit,
    onAudioTrack: (TrackOption) -> Unit,
    onBrightnessDelta: (Float) -> Unit,
    onVolumeDelta: (Float) -> Unit,
    onToggleAutoSubtitle: () -> Unit,
    onToggleAutoNext: () -> Unit,
    onToggleRememberSpeed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tabTitles = listOf("播放", "画面声音", "字幕音轨")
    var selectedTab by remember { mutableStateOf(0) }

    Box(modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
        Column(
            modifier = Modifier.fillMaxHeight().width(460.dp)
                .background(Color.Black.copy(alpha = 0.94f))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "播放设置",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                GlassButton("✕ 关闭", onClose)
            }

            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                containerColor = Color.Transparent
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = index == selectedTab,
                        onClick = { selectedTab = index },
                        onFocus = {},
                        modifier = Modifier.tvTouchClick { selectedTab = index }
                    ) {
                        Text(
                            title,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        SectionTitle("播放速度")
                        ChoiceGrid(
                            listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 3.0f).map {
                                "${it}x" to { onSpeed(it) }
                            },
                            selectedIndex = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 3.0f)
                                .indexOf(settings.speed)
                        )

                        SectionTitle("双击快进 / 快退步长")
                        ChoiceGrid(
                            listOf(5, 10, 15, 30).map { "${it}s" to { onSeekStep(it) } },
                            selectedIndex = listOf(5, 10, 15, 30).indexOf(settings.seekStepSeconds)
                        )

                        SectionTitle("播放偏好")
                        ChoiceGrid(
                            listOf(
                                "自动下一集：${if (settings.autoNext) "开" else "关"}" to onToggleAutoNext,
                                "记住速度：${if (settings.rememberSpeed) "开" else "关"}" to onToggleRememberSpeed
                            ),
                            selectedIndex = -1
                        )
                    }
                    1 -> {
                        SectionTitle("画面比例")
                        ChoiceGrid(
                            ScaleMode.entries.map { it.label to { onScaleMode(it) } },
                            selectedIndex = ScaleMode.entries.indexOf(ScaleMode.from(settings.scaleMode))
                        )

                        SectionTitle("亮度")
                        StepperRow("亮度 ${(brightness * 100).roundToInt()}%", onBrightnessDelta)

                        SectionTitle("音量")
                        StepperRow("音量 ${(volume * 100).roundToInt()}%", onVolumeDelta)
                    }
                    2 -> {
                        SectionTitle("字幕轨")
                        ChoiceGrid(
                            textTracks.map { it.label to { onTextTrack(it) } },
                            selectedIndex = textTracks.indexOfFirst { it.selected }
                        )

                        SectionTitle("音轨")
                        ChoiceGrid(
                            audioTracks.map { it.label to { onAudioTrack(it) } },
                            selectedIndex = audioTracks.indexOfFirst { it.selected }
                        )

                        SectionTitle("默认字幕")
                        ChoiceGrid(
                            listOf("同名字幕：${if (settings.autoSubtitle) "开" else "关"}" to onToggleAutoSubtitle),
                            selectedIndex = -1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun ChoiceGrid(items: List<Pair<String, () -> Unit>>, selectedIndex: Int) {
    items.chunked(3).forEach { row ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            row.forEach { item ->
                GlassButton(
                    label = item.first,
                    onClick = item.second,
                    active = items.indexOf(item) == selectedIndex,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StepperRow(label: String, onDelta: (Float) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        GlassButton("−", { onDelta(-0.05f) })
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Color.White,
            modifier = Modifier.weight(1f))
        GlassButton("+", { onDelta(0.05f) })
    }
}

fun formatTime(ms: Long): String {
    if (ms <= 0) return "00:00"
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
