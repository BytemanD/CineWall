@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.cinewall.app.ui.player

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.IconButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Switch
import androidx.tv.material3.Tab
import androidx.tv.material3.TabDefaults
import androidx.tv.material3.TabRow
import androidx.tv.material3.TabRowDefaults
import androidx.tv.material3.Text
import com.cinewall.app.model.LONG_PRESS_SPEED_OPTIONS
import com.cinewall.app.model.PlaybackSettings
import com.cinewall.app.ui.components.tvTouchClick
import com.cinewall.app.ui.theme.onColor
import kotlin.math.abs
import kotlin.math.roundToInt

/** 播放器里统一使用的倍速档位和画面比例档位。 */
val PLAYBACK_SPEEDS = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 3.0f)
val SCALE_MODES = ScaleMode.entries

/** 剧集选择器中的一集。 */
data class EpisodeOption(val id: String, val label: String, val current: Boolean)

/**
 * 全屏手势层：
 * - 左右半屏上下拖动 → 亮度 / 音量
 * - 中间横向拖动 → 预览快进/快退，松手才真正跳转，拖动期间上方显示时间提示
 * - 单击显示/隐藏控制条，双击中间暂停、双击左右 ±步长
 * - 遥控器菜单键打开配置面板
 */
/**
 * 遥控器按键处理：仅用于面板打开期间的菜单键关闭面板。
 * 播放/暂停、快进/后退已在 PlayerScreen 根层 onPreviewKeyEvent 中统一拦截。
 *
 * - 控制条显示时：directional 按键交给控制条按钮内部 D-Pad 导航，不拦截。
 * - 面板打开时：directional 按键交给面板内部导航，不拦截。
 */
@Composable
fun PlayerKeyHandler(
    enabled: Boolean,
    controlsVisible: Boolean,
    onMenu: () -> Unit,
    menuEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val currentMenu by rememberUpdatedState(onMenu)

    Box(
        modifier = modifier.fillMaxSize().focusable().onPreviewKeyEvent { event ->
            Log.i("APP", "onKeyEvent: ${event.nativeKeyEvent}")
            if (!enabled || event.type != KeyEventType.KeyUp) return@onPreviewKeyEvent false
            val keyCode = event.key.nativeKeyCode
            // 菜单键无论面板是否打开都要响应：打开 / 关闭面板。
            if (menuEnabled && (keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_INFO)) {
                currentMenu()
                return@onPreviewKeyEvent true
            }
            // 控制条显示时，方向键交给控制条按钮内部导航，不拦截。
            if (controlsVisible) {
                return@onPreviewKeyEvent false
            }
            // 控制条不显示时，非菜单按键不再消费，交给根层 onPreviewKeyEvent 处理播放控制。
            false
        }
    )
}

/**
 * 全屏触摸手势层：放在控制条之上，控制条显隐都不影响长按 / 亮度 / 音量 / 点按 / 拖动。
 * 只避开顶部标题区和底部控制区，避免挡住返回、设置、进度条等按钮。
 */
@Composable
fun PlayerGestures(
    enabled: Boolean,
    seekRangeMs: Long,
    onTap: () -> Unit,
    onDoubleTap: (Float) -> Unit,
    onLongPressStart: () -> Unit,
    onLongPressEnd: () -> Unit,
    onBrightnessDelta: (Float) -> Unit,
    onVolumeDelta: (Float) -> Unit,
    onSeekStart: () -> Unit,
    onSeekPreview: (Long) -> Unit,
    onSeekEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentTap by rememberUpdatedState(onTap)
    val currentDoubleTap by rememberUpdatedState(onDoubleTap)
    val currentLongPressStart by rememberUpdatedState(onLongPressStart)
    val currentLongPressEnd by rememberUpdatedState(onLongPressEnd)
    val currentBrightness by rememberUpdatedState(onBrightnessDelta)
    val currentVolume by rememberUpdatedState(onVolumeDelta)
    val currentSeekStart by rememberUpdatedState(onSeekStart)
    val currentSeekPreview by rememberUpdatedState(onSeekPreview)
    val currentSeekEnd by rememberUpdatedState(onSeekEnd)
    val currentSeekRange by rememberUpdatedState(seekRangeMs)
    val longPressHandler = remember { Handler(Looper.getMainLooper()) }

    // 手势层只覆盖中间视频区域，顶部标题和底部控制条完全不被遮挡，
    // 这样按钮、进度条既能触摸也能用遥控器聚焦。
    Box(
        modifier.fillMaxSize()
            .padding(top = 120.dp, bottom = 240.dp)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                val slop = viewConfiguration.touchSlop
                var lastTapTime = 0L
                var lastTapPos = Offset.Zero

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val pointerId = down.id
            val start = down.position
            var last = start
            // 0=未决定 1=忽略(长按) 2=横向拖动(seek) 3=左侧亮度 4=右侧音量
            var mode = 0
            var longPressTriggered = false
            var seekAccum = 0f
            val leftEdge = size.width * 0.32f
            val rightEdge = size.width * 0.68f
            val totalMs = currentSeekRange.takeIf { it > 0 } ?: 60_000L

            // 按住不动 500ms 触发临时倍速；一旦拖动就取消。
            // awaitEachGesture 是受限协程作用域，不能用 launch，因此用 Handler。
            val longPressRunnable = Runnable {
                if (mode == 0) {
                    longPressTriggered = true
                    mode = 1
                    currentLongPressStart()
                }
            }
            longPressHandler.postDelayed(longPressRunnable, 500L)

            try {
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                    val pos = change.position
                    val dxTotal = pos.x - start.x
                    val dyTotal = pos.y - start.y

                    if (mode == 0 && (abs(dxTotal) > slop || abs(dyTotal) > slop)) {
                        longPressHandler.removeCallbacks(longPressRunnable)
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
                        longPressHandler.removeCallbacks(longPressRunnable)
                        if (mode == 2) currentSeekEnd()
                        if (longPressTriggered) {
                            currentLongPressEnd()
                            break
                        }
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
            } finally {
                longPressHandler.removeCallbacks(longPressRunnable)
            }
        }
    })
}

/** 亮度 / 音量调节类型。 */
enum class AdjustmentKind { BRIGHTNESS, VOLUME }

/** 当前正在调节的项目与档位（0f ~ 1f）。 */
data class AdjustmentInfo(val kind: AdjustmentKind, val level: Float)

/** 屏幕中央的调节提示：只用图标 + 细小进度条，不显示文字。 */
@Composable
fun AdjustmentHud(info: AdjustmentInfo?, modifier: Modifier = Modifier) {
    if (info == null) return
    val icon = if (info.kind == AdjustmentKind.BRIGHTNESS) {
        Icons.Filled.WbSunny
    } else {
        Icons.Filled.VolumeUp
    }
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.clip(RoundedCornerShape(24.dp))
                .background(Color.Black.copy(alpha = 0.62f))
                .padding(horizontal = 24.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Color.White)
            Box(
                Modifier.width(120.dp).height(6.dp).clip(RoundedCornerShape(3.dp))
                    .background(Color.White.copy(alpha = 0.25f))
            ) {
                Box(
                    Modifier.fillMaxHeight()
                        .fillMaxWidth(info.level.coerceIn(0f, 1f))
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.White)
                )
            }
        }
    }
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

/** 进度条下方的透明快捷按钮，平时不抢画面，聚焦时才高亮。 */
@Composable
private fun TransparentQuickButton(label: String, onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    Button(
        onClick = onClick,
        modifier = Modifier.tvTouchClick(onClick),
        shape = ButtonDefaults.shape(shape = RoundedCornerShape(14.dp)),
        colors = ButtonDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = Color.White.copy(alpha = 0.82f),
            focusedContainerColor = primary.copy(alpha = 0.28f),
            focusedContentColor = Color.White,
            pressedContainerColor = primary.copy(alpha = 0.38f),
            pressedContentColor = Color.White
        ),
        glow = ButtonDefaults.glow(focusedGlow = Glow(primary.copy(alpha = 0.25f), 8.dp)),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = Color.White)
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
    fileName: String,
    position: Long,
    duration: Long,
    isPlaying: Boolean,
    speed: Float,
    scaleMode: ScaleMode,
    brightness: Float,
    volume: Float,
    visible: Boolean,
    toast: String,
    onBack: () -> Unit,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onOpenPanel: () -> Unit,
    onCycleSpeed: () -> Unit,
    onCycleScale: () -> Unit,
    episodes: List<EpisodeOption>,
    onOpenEpisodes: () -> Unit,
    onSeekFraction: (Float) -> Unit,
    onKeySeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {

    val rootFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
//        if (visible) {
//        }
        Log.i("APP", "controls visible")
        rootFocus.requestFocus()
        Log.i("KEY", "controls requestFocus 已调用")
    }

    Box(modifier.fillMaxSize()
//        .focusRequester(rootFocus)
//        .focusable()
        .onFocusChanged {
            Log.i("APP", "controls isFocused=${it.isFocused} hasFocus=${it.hasFocus}")
        }) {
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                // 剧集在标题下方显示当前文件名，方便确认具体是哪一集。
                if (episodes.isNotEmpty() && fileName.isNotBlank()) {
                    Text(
                        fileName,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.72f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
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
            }

            SeekBar(
                position = position,
                duration = duration,
                onSeekFraction = onSeekFraction,
                onKeySeek = onKeySeek,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth().focusGroup(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GlassButton(
                    if (isPlaying) "❚❚ 暂停" else "▶ 播放",
                    onTogglePlay,
                    modifier.focusRequester(focusRequester = rootFocus)
                )
                GlassButton("⏮ 上一集", onPrevious)
                GlassButton("⏭ 下一集", onNext)
                if (episodes.isNotEmpty()) {
                    GlassButton("剧集", onOpenEpisodes)
                }
                Spacer(Modifier.weight(1f))
                // 透明快捷按钮放在同一行右侧：同一个按钮循环切换倍速 / 画面比例。
                TransparentQuickButton("${speed}x", onCycleSpeed)
                TransparentQuickButton("${scaleMode.label}", onCycleScale)
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

/** 剧集列表弹层：点击某一集直接切换播放。 */
@Composable
fun EpisodePickerDialog(
    episodes: List<EpisodeOption>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.heightIn(max = 620.dp).width(720.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Black.copy(alpha = 0.82f))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "选择剧集",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                GlassButton("关闭", onDismiss)
            }
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                episodes.forEach { episode ->
                    GlassButton(
                        label = episode.label,
                        onClick = { onSelect(episode.id) },
                        modifier = Modifier.fillMaxWidth(),
                        active = episode.current
                    )
                }
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
                if (event.type != KeyEventType.KeyUp) return@onKeyEvent false
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
    onUpdateSettings:  ((PlaybackSettings) -> PlaybackSettings) -> Unit,
    textTracks: List<TrackOption>,
    audioTracks: List<TrackOption>,
    brightness: Float,
    volume: Float,
    onClose: () -> Unit,
    onInteract: () -> Unit,
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
    onLongPressSpeed: (Float) -> Unit,
    onToggleShowCurrentTime: () -> Unit,
    onToggleShowProgressInfo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tabTitles = listOf("播放", "画面声音", "字幕", "音轨")
    var selectedTab by remember { mutableIntStateOf(0) }

    val rootFocus = remember { FocusRequester() }
//    LaunchedEffect(Unit) {
//        rootFocus.requestFocus()
//    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                Log.i("PLAYER", "panel pointer")
                detectTapGestures { offset ->
                    val panelEdgeX = size.width - 460.dp.toPx();
                    Log.i("PLAYER", "panel pointer ${offset.x}, ${panelEdgeX}")
                    // 点击面板左侧（面板外区域）即关闭。
                    if (offset.x < panelEdgeX) onClose()
                }
            },
        contentAlignment = Alignment.CenterEnd
    ) {
        Column(
            modifier = Modifier.fillMaxHeight().width(460.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {}

            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth().height(36.dp),
            ) {
                tabTitles.forEachIndexed { index, title ->
                    var tabModifier = Modifier.tvTouchClick {
                        onInteract()
                        selectedTab = index
                    }
                    if (index == 0) {
                        tabModifier = tabModifier.focusRequester(rootFocus)
                        LaunchedEffect(Unit) {
                            rootFocus.requestFocus()
                        }
                    }
                    Tab(
                        selected = index == selectedTab,
                        onClick = {
                            onInteract()
                            selectedTab = index
                        },
                        onFocus = { onInteract() },
                        // 面板是半透明黑背景，未选中 Tab 不能跟随亮色主题的深色 onSurface。
//                        colors = TabDefaults.underlinedIndicatorTabColors(
//                            contentColor = Color.White,
//                            selectedContentColor = MaterialTheme.colorScheme.primary,
//                            focusedContentColor = MaterialTheme.colorScheme.primary,
//                            inactiveContentColor = Color.Red
////                            contentColor = Color.White,
////                            inactiveContentColor = Color.White.copy(alpha = 0.72f),
////                            focusedContentColor = Color.White
//                        ),
                        modifier = tabModifier
                    ) {
                        Text(
                            title,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
            HorizontalDivider(
//                thickness = 2.dp,               // 控制粗细
//                color = Color.Gray,             // 控制颜色
//                modifier = Modifier.padding(horizontal = 16.dp) // 控制边距
            )
            Column(
                modifier = Modifier.fillMaxWidth().padding(8.dp).weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        SectionTitle("播放速度")
                        ChoiceGrid(
                            PLAYBACK_SPEEDS.map { "${it}x" to { onSpeed(it) } },
                            selectedIndex = PLAYBACK_SPEEDS.indexOf(settings.speed)
                        )

                        SectionTitle("长按临时倍速")
                        ChoiceGrid(
                            LONG_PRESS_SPEED_OPTIONS.map { speed ->
                                "${speed}x" to { onLongPressSpeed(speed) }
                            },
                            selectedIndex = LONG_PRESS_SPEED_OPTIONS.indexOf(settings.longPressSpeed)
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
                                "记住速度：${if (settings.rememberSpeed) "开" else "关"}" to onToggleRememberSpeed,
                                "当前时间：${if (settings.showCurrentTime) "开" else "关"}" to onToggleShowCurrentTime,
                                "视频进度：${if (settings.showProgressInfo) "开" else "关"}" to onToggleShowProgressInfo
                            ),
                            selectedIndex = -1
                        )
                    }
                    1 -> {
                        SectionTitle("画面比例")
                        ChoiceGrid(
                            SCALE_MODES.map { it.label to { onScaleMode(it) } },
                            selectedIndex = SCALE_MODES.indexOf(ScaleMode.from(settings.scaleMode))
                        )

                        SectionTitle("亮度")
                        StepperRow("亮度 ${(brightness * 100).roundToInt()}%", onBrightnessDelta)

                        SectionTitle("音量")
                        StepperRow("音量 ${(volume * 100).roundToInt()}%", onVolumeDelta)
                        SectionTitle("显示")
                        SwitchRow(
                            "系统时间",
                            settings.showCurrentTime,
                            onCheckedChange= { newValue ->
                                onUpdateSettings { it.copy(showCurrentTime = newValue) }
                            }
                        )
                        SwitchRow(
                            "进度",
                            settings.showProgressInfo,
                            onCheckedChange= { newValue ->
                                onUpdateSettings { it.copy(showProgressInfo = newValue) }
                            }
                        )
                    }
                    2 -> {
                        SectionTitle("字幕轨")
                        TrackOptionList(textTracks, onTextTrack)

                        SectionTitle("默认字幕")
                        ChoiceGrid(
                            listOf("同名字幕：${if (settings.autoSubtitle) "开" else "关"}" to onToggleAutoSubtitle),
                            selectedIndex = -1
                        )
                    }
                    3 -> {
                        SectionTitle("音轨")
                        TrackOptionList(audioTracks, onAudioTrack)
                    }
                }
            }
        }
    }
}

/** 播放画面右上角的状态信息：当前时间和/或视频进度。 */
@Composable
fun PlaybackStatusOverlay(
    currentTime: String,
    position: Long,
    duration: Long,
    showCurrentTime: Boolean,
    showProgress: Boolean,
    modifier: Modifier = Modifier
) {
    if (!showCurrentTime && !showProgress) return
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
        Column(
            modifier = Modifier.padding(top = 28.dp, end = 92.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (showCurrentTime) {
                Text(
                    currentTime,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }
            if (showProgress) {
                Text(
                    "${formatTime(position)} / ${formatTime(duration)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.88f)
                )
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
    items.chunked(4).forEach { row ->
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

/** 字幕 / 音轨用纵向列表展示，选中项高亮并带勾。 */
@Composable
private fun TrackOptionList(
    options: List<TrackOption>,
    onSelect: (TrackOption) -> Unit
) {
    if (options.isEmpty()) {
        Text(
            "暂无可选轨道",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.6f)
        )
        return
    }
    val primary = MaterialTheme.colorScheme.primary
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            val active = option.selected
            Button(
                onClick = { onSelect(option) },
                modifier = Modifier.fillMaxWidth().tvTouchClick { onSelect(option) },
                shape = ButtonDefaults.shape(shape = RoundedCornerShape(12.dp)),
                colors = ButtonDefaults.colors(
                    containerColor = if (active) primary else Color.Black.copy(alpha = 0.45f),
                    contentColor = if (active) onColor(primary) else Color.White,
                    focusedContainerColor = primary.copy(alpha = 0.88f),
                    focusedContentColor = onColor(primary),
                    pressedContainerColor = primary.copy(alpha = 0.72f),
                    pressedContentColor = onColor(primary)
                ),
                glow = ButtonDefaults.glow(focusedGlow = Glow(primary.copy(alpha = 0.45f), 12.dp)),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        option.label,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (active) {
                        Text("✓", style = MaterialTheme.typography.titleMedium)
                    }
                }
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
@Composable
private fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Color.White,
            modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Switch(checked, onCheckedChange)
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
