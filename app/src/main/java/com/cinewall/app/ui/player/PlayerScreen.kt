package com.cinewall.app.ui.player

import android.util.Log
import android.content.Context
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.cinewall.app.model.MediaType
import com.cinewall.app.model.PlaybackSettings
import com.cinewall.app.repository.MediaRepository
import com.cinewall.app.scanner.TvShowMatcher
import com.cinewall.app.ui.components.tvTouchClick
import com.cinewall.app.utils.SubtitleFinder
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.MediaItem as ExoMediaItem
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt
import java.util.Date
import androidx.lifecycle.compose.LifecycleStartEffect
import kotlinx.coroutines.launch

@Composable
fun PlayerScreen(
    modifier: Modifier = Modifier,
    mediaId: String,
    repository: MediaRepository,
    onNavigateBack: () -> Unit,
    onBrightnessChanged: (Float) -> Unit = {},
) {
    val focusRequester = remember { FocusRequester() }

    val scope = rememberCoroutineScope()

    var currentId by remember(mediaId) { mutableStateOf(mediaId) }
    var settings by remember { mutableStateOf(repository.loadPlaybackSettings()) }
    val all by repository.getAllMedia().collectAsState(initial = emptyList())

    val item = all.firstOrNull { it.id == currentId }

    // 同剧名剧集按季/集排序，供上一集/下一集和播完连播使用
    val episodes = remember(all, item) {
        val current = item
        if (current == null || current.mediaType != MediaType.TV_SHOW) {
            emptyList()
        } else {
            val showTitle = TvShowMatcher.showTitle(current.fileName)
            all.filter {
                it.mediaType == MediaType.TV_SHOW &&
                    TvShowMatcher.showTitle(it.fileName) == showTitle
            }.sortedWith(compareBy<com.cinewall.app.model.MediaItem> { it.seasonNumber }
                .thenBy { it.episodeNumber })
        }
    }
    val currentIndex = episodes.indexOfFirst { it.id == currentId }
    val nextId = episodes.getOrNull(currentIndex + 1)?.id
    val previousId = if (currentIndex > 0) episodes[currentIndex - 1].id else null

    fun updateSettings(transform: (PlaybackSettings) -> PlaybackSettings) {
        val updated = transform(settings)
        settings = updated
        repository.savePlaybackSettings(updated)
    }

    // 播放器根布局写死纯黑，不跟随主题，避免视频周围出现白边/彩色边
    Box(modifier = modifier.fillMaxSize()
        .background(Color.Black)
    ) {
        val filePath = item?.filePath.orEmpty()
        if (filePath.isNotEmpty() && item != null) {
            // filePath 变化（换集）时重建播放器；改速度/字幕不会打断播放
            key(filePath) {
                PlaybackContent(
                    mediaId = currentId,
                    title = item.displayTitle,
                    fileName = item.fileName,
                    filePath = filePath,
                    settings = settings,
                    initialPositionMs = remember(currentId) {
                        repository.loadPlaybackPosition(currentId)
                    },
                    previousId = previousId,
                    nextId = nextId,
                    episodes = remember(episodes, currentId) {
                        episodes.map { episode ->
                            val number = if (episode.seasonNumber > 0 || episode.episodeNumber > 0) {
                                "S%02dE%02d ".format(episode.seasonNumber, episode.episodeNumber)
                            } else {
                                ""
                            }
                            EpisodeOption(
                                id = episode.id,
                                label = number + episode.episodeTitle.ifBlank { episode.displayTitle },
                                current = episode.id == currentId
                            )
                        }
                    },
                    onSwitchEpisode = { currentId = it },
                    onNavigateBack = onNavigateBack,
                    onBrightnessChanged = onBrightnessChanged,
                    onSavePosition = { positionMs ->
                        repository.savePlaybackPosition(currentId, positionMs)
                    },
                    onUpdateSettings = { transform -> updateSettings(transform) }
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp)
                    .focusRequester(focusRequester)
                    .focusable(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "无法播放该媒体文件",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = onNavigateBack, modifier = Modifier.tvTouchClick(onNavigateBack)) {
                    Text("返回")
                }
            }
        }
    }
    // 3. 在页面进入时自动请求焦点
    LaunchedEffect(Unit) {
        // 核心防坑：延迟一帧再请求焦点，防止组件尚未完成组合导致崩溃或焦点丢失
        scope.launch {
            withFrameNanos {
                focusRequester.requestFocus()
            }
        }
    }
}

@Composable
private fun PlaybackContent(
    mediaId: String,
    title: String,
    fileName: String,
    filePath: String,
    settings: PlaybackSettings,
    initialPositionMs: Long,
    previousId: String?,
    nextId: String?,
    episodes: List<EpisodeOption>,
    onSwitchEpisode: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onBrightnessChanged: (Float) -> Unit,
    onSavePosition: (Long) -> Unit,
    onUpdateSettings: ((PlaybackSettings) -> PlaybackSettings) -> Unit
) {
    val rootFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        rootFocus.requestFocus()
        Log.i("KEY", "************ requestFocus 已调用")
    }
    val context = LocalContext.current
    val controller = remember { PlayerController(context) }
    val currentSettings by rememberUpdatedState(settings)
    val currentNextId by rememberUpdatedState(nextId)
    val currentSwitch by rememberUpdatedState(onSwitchEpisode)
    val currentUpdate by rememberUpdatedState(onUpdateSettings)
    val currentSavePosition by rememberUpdatedState(onSavePosition)
    val clockFormat = remember { android.text.format.DateFormat.getTimeFormat(context) }

    var controlsVisible by remember { mutableStateOf(true) }
    var panelOpen by remember { mutableStateOf(false) }
    var episodePickerOpen by remember { mutableStateOf(false) }
    // 每次面板交互都 +1，用于重启“10 秒无操作自动关闭”计时。
    var panelInteractionTick by remember { mutableStateOf(0) }
    var toast by remember { mutableStateOf("") }
    // 亮度/音量调节时屏幕中央的图标提示。
    var adjustment by remember { mutableStateOf<AdjustmentInfo?>(null) }
    // 横向拖动预览：松手前不真正跳转，松手才 seek
    var seekBase by remember { mutableStateOf<Long?>(null) }
    var seekTarget by remember { mutableStateOf(0L) }
    var seekDelta by remember { mutableStateOf(0L) }
    var seekHud by remember { mutableStateOf("") }
    var tracksVersion by remember { mutableStateOf(0) }
    var position by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentTimeText by remember { mutableStateOf("") }
    // 长按前的真实播放速度；非 null 表示当前正处于长按临时倍速状态。
    val longPressOriginalSpeed = remember { mutableStateOf<Float?>(null) }
    // 每 5 秒落盘一次即可，避免频繁写 SharedPreferences。
    var lastSavedPosition by remember { mutableStateOf(0L) }

    var brightness by remember { mutableStateOf(-1f) } // -1=跟随系统
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    val maxVolume = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 0
    var volume by remember {
        val current = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
        mutableStateOf(if (maxVolume > 0) current.toFloat() / maxVolume else 0.5f)
    }

    fun notePanelInteraction() {
        panelInteractionTick++
    }

    fun openPanel() {
        Log.i("APP", "openPanel: ${panelOpen}")

        panelOpen = true
        controlsVisible = false
    }

    fun togglePanel() {
        Log.i("APP", "togglePanel panelOpen: ${panelOpen}")
        if (panelOpen) panelOpen = false else openPanel()
    }

    LifecycleStartEffect(Unit) {
        controller.play()
        onStopOrDispose {
            controller.pause()      // 只暂停，保留进度，回前台秒续播
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            // 离开播放器 / 切换剧集时，把最后的位置也存下来。
            val last = controller.position
            if (last > 0L) currentSavePosition(last)
            // ExoPlayer.release() 在主线程可能耗时；等滑出动画结束再释放，
            // 避免退出时先卡住几秒才看到应用页面。
            controller.pause()
            Handler(Looper.getMainLooper()).postDelayed({
                controller.release()
            }, 300L)
        }
    }

    LaunchedEffect(Unit) {
        controller.autoNext = currentSettings.autoNext
        controller.rememberSpeed = currentSettings.rememberSpeed
        controller.onEnded = {
            // 正常播完不保留进度，下次从头开始（或由自动下一集接管）。
            currentSavePosition(0L)
            currentNextId?.let { currentSwitch(it) }
        }
        controller.onSpeedChanged = { speed ->
            // 长按临时倍速不写回设置，松开后恢复的那一帧也不写。
            if (longPressOriginalSpeed.value == null) {
                currentUpdate { it.copy(speed = speed) }
            }
        }

        val builder = ExoMediaItem.Builder().setUri(filePath)
        if (currentSettings.autoSubtitle) {
            SubtitleFinder.find(context, filePath, fileName)?.let { subtitle ->
                builder.setSubtitleConfigurations(
                    listOf(
                        ExoMediaItem.SubtitleConfiguration.Builder(subtitle.uri)
                            .setMimeType(subtitle.mimeType)
                            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                            .build()
                    )
                )
            }
        }
        controller.prepare(
            builder.build(),
            currentSettings.speed,
            ScaleMode.from(currentSettings.scaleMode),
            initialPositionMs
        )
    }

    LaunchedEffect(settings.autoNext, settings.rememberSpeed) {
        controller.autoNext = settings.autoNext
        controller.rememberSpeed = settings.rememberSpeed
    }

    // 进度轮询（不通过 listener 触发重组，开销可控）
    LaunchedEffect(Unit) {
        while (true) {
            position = controller.position
            duration = controller.duration
            isPlaying = controller.isPlaying
            if (position > 0L && kotlin.math.abs(position - lastSavedPosition) >= 5_000L) {
                lastSavedPosition = position
                currentSavePosition(position)
            }
            delay(250)
        }
    }

    // 右上角当前时间，每 30 秒刷新一次。
    LaunchedEffect(Unit) {
        while (true) {
            currentTimeText = clockFormat.format(Date())
            delay(30_000)
        }
    }

    LaunchedEffect(controlsVisible, panelOpen) {
        if (controlsVisible && !panelOpen) {
            delay(4000)
            controlsVisible = false
        }
    }

    // 面板打开后，10 秒内没有任何操作则自动关闭；任何面板交互都会重置计时。
    LaunchedEffect(panelOpen, panelInteractionTick) {
        if (!panelOpen) return@LaunchedEffect
        delay(10_000)
        panelOpen = false
    }

    LaunchedEffect(toast) {
        if (toast.isNotBlank()) {
            delay(900)
            toast = ""
        }
    }

    LaunchedEffect(adjustment) {
        if (adjustment != null) {
            delay(900)
            adjustment = null
        }
    }

    fun changeBrightness(delta: Float) {
        val next = if (brightness < 0f) 0.5f + delta else (brightness + delta).coerceIn(0.05f, 1f)
        brightness = next
        onBrightnessChanged(next)
        adjustment = AdjustmentInfo(AdjustmentKind.BRIGHTNESS, next)
    }

    fun changeVolume(delta: Float) {
        if (maxVolume <= 0 || audioManager == null) return
        val next = (volume + delta).coerceIn(0f, 1f)
        volume = next
        audioManager.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            (next * maxVolume).roundToInt(),
            0
        )
        adjustment = AdjustmentInfo(AdjustmentKind.VOLUME, next)
    }

    fun startSeek() {
        seekBase = controller.position
        seekTarget = controller.position
        seekDelta = 0L
        controlsVisible = true
    }

    fun previewSeek(deltaMs: Long) {
        val base = seekBase ?: controller.position.also { seekBase = it }
        val total = controller.duration
        val target = (base + deltaMs).coerceIn(0L, total.takeIf { it > 0 } ?: Long.MAX_VALUE)
        seekTarget = target
        seekDelta = deltaMs
        val action = if (deltaMs >= 0) "前进" else "后退"
        seekHud = "$action ${formatTime(abs(deltaMs))}  →  ${formatTime(target)} / ${formatTime(total)}"
        controlsVisible = true
    }

    fun endSeek() {
        if (seekBase != null) {
            controller.seekTo(seekTarget)
            toast = if (seekDelta >= 0) {
                "前进 ${formatTime(abs(seekDelta))}"
            } else {
                "后退 ${formatTime(abs(seekDelta))}"
            }
        }
        seekBase = null
        seekHud = ""
    }

    fun setSpeed(speed: Float) {
        controller.setSpeed(speed)
        onUpdateSettings { it.copy(speed = speed) }
        toast = "播放速度 ${speed}x"
    }

    fun setScale(mode: ScaleMode) {
        controller.setScaleMode(mode)
        onUpdateSettings { it.copy(scaleMode = mode.name) }
        toast = mode.label
    }

    fun cycleSpeed() {
        val currentIndex = PLAYBACK_SPEEDS.indexOf(currentSettings.speed).let {
            if (it >= 0) it else PLAYBACK_SPEEDS.indexOf(1.0f)
        }
        val next = PLAYBACK_SPEEDS[(currentIndex + 1) % PLAYBACK_SPEEDS.size]
        setSpeed(next)
    }

    fun cycleScale() {
        val currentIndex = SCALE_MODES.indexOf(ScaleMode.from(currentSettings.scaleMode))
        val next = SCALE_MODES[(currentIndex + 1) % SCALE_MODES.size]
        setScale(next)
    }

    fun handleDoubleTap(fraction: Float) {
        val step = currentSettings.seekStepSeconds * 1000L
        when {
            fraction < 0.35f -> {
                controller.seekBy(-step)
                toast = "« ${currentSettings.seekStepSeconds}s"
            }
            fraction > 0.65f -> {
                controller.seekBy(step)
                toast = "${currentSettings.seekStepSeconds}s »"
            }
            else -> {
                controller.playPause()
                toast = if (controller.isPlaying) "▶ 播放" else "❚❚ 暂停"
            }
        }
        controlsVisible = true
    }

    val textTracks = remember(panelOpen, tracksVersion) {
        if (panelOpen) controller.trackOptions(C.TRACK_TYPE_TEXT) else emptyList()
    }
    val audioTracks = remember(panelOpen, tracksVersion) {
        if (panelOpen) controller.trackOptions(C.TRACK_TYPE_AUDIO) else emptyList()
    }

    // 仅当面板显示时才拦截返回键
    BackHandler(enabled = panelOpen || controlsVisible) {
        Log.i("APP", "---------------------------------")
        if (panelOpen) {
            panelOpen = false
        } else if (controlsVisible) {
            controlsVisible = false
        }
    }

    LaunchedEffect(Unit) {
//        if (visible) {
//        }
        Log.i("APP", "controls visible")
        rootFocus.requestFocus()
        Log.i("KEY", "controls requestFocus 已调用")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(rootFocus)
            .focusable()
            .onFocusChanged {
                Log.i("APP", "根容器 isFocused=${it.isFocused} hasFocus=${it.hasFocus}")
            }
//            .onPreviewKeyEvent { event ->
//                Log.i("APP", "preview: key=${event.key} type=${event.type}")
//                false        // ← 先返回 false，别消费，让事件继续走
//            }
//            .onKeyEvent { event ->
//                Log.i("APP", "bubble:  key=${event.key} type=${event.type}")
//                false
//            }
            // 遥控器按键优先在根层拦截，确保无论焦点在控制条哪个按钮上，
            // 确定键始终暂停/播放、左右键始终前进/后退、菜单键开关面板、
            // 方向键下开关控制条，不会被控制条按钮抢走焦点导致按键无效。
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val keyCode = event.key.keyCode.toInt()

                // 菜单键始终开关配置面板，优先级最高
                if (keyCode == KeyEvent.KEYCODE_MENU) {
                    togglePanel()
                    return@onPreviewKeyEvent true
                }

                // 右侧配置面板或剧集选择器打开时，方向键交给面板内部导航，不拦截
                if (panelOpen || episodePickerOpen) {
                    return@onPreviewKeyEvent false
                }

                // 底部控制条显示时，方向键交给控制条按钮内部 D-Pad 导航，不拦截。
                // 焦点在播放/暂停、上一集、下一集等按钮上时，确定键会触发按钮点击。
                if (controlsVisible) {
                    return@onPreviewKeyEvent false
                }

                // 控制条和面板都不显示时，按键控制播放
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                        controlsVisible = true
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                        controller.playPause()
                        true
                    }
                    KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        controller.seekBy(currentSettings.seekStepSeconds * 1000L)
                        true
                    }
                    KeyEvent.KEYCODE_MEDIA_REWIND, KeyEvent.KEYCODE_DPAD_LEFT -> {
                        controller.seekBy(-currentSettings.seekStepSeconds * 1000L)
                        controlsVisible = true
                        true
                    }
                    else -> false
                }
            }
    ) {
        VideoPlayer(controller = controller, modifier = Modifier.fillMaxSize())

        AnimatedVisibility (
            visible = controlsVisible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn(),   // 默认入场：淡入
            exit = fadeOut()    // 默认出场：淡出
        ) {
            PlayerControls(
                visible=controlsVisible,
                title = title,
                fileName = fileName,
                position = position,
                duration = duration,
                isPlaying = isPlaying,
                speed = currentSettings.speed,
                scaleMode = ScaleMode.from(currentSettings.scaleMode),
                brightness = if (brightness < 0f) 0.5f else brightness,
                volume = volume,
                toast = toast,
                onBack = {
                    // 返回动画一开始就停声，画面继续滑出，避免退出时还有残留声音。
                    controller.pause()
                    onNavigateBack()
                },
                onTogglePlay = { controller.playPause() },
                onPrevious = {
                    if (previousId != null) onSwitchEpisode(previousId!!) else toast = "没有上一集了"
                },
                onNext = {
                    if (nextId != null) onSwitchEpisode(nextId!!) else toast = "没有下一集了"
                },
                onOpenPanel = {
                    openPanel()
                    notePanelInteraction()
                },
                onCycleSpeed = { cycleSpeed() },
                onCycleScale = { cycleScale() },
                episodes = episodes,
                onOpenEpisodes = {
                    panelOpen = false
                    episodePickerOpen = true
                    notePanelInteraction()
                },
                onSeekFraction = { fraction -> controller.seekTo((duration * fraction).toLong()) },
                onKeySeek = { delta -> controller.seekBy(delta) }
            )
        }

        // 遥控器菜单键由 PlayerKeyHandler 负责关闭面板；
        // 方向键在控制条显示时交给控制条按钮内部导航，面板打开时交给面板内部导航。
//        PlayerKeyHandler(
//            enabled = !panelOpen && !episodePickerOpen,
//            controlsVisible = controlsVisible,
//            onMenu = { Log.i("APP", "onMenu"); togglePanel(); },
//            menuEnabled = true,
//            modifier = Modifier.matchParentSize()
//        )
        AnimatedVisibility(
            visible = panelOpen,
            modifier = Modifier.align(Alignment.CenterEnd),
            enter = slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth }  ),
            exit = slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth }  ),
        ) {
            SettingsPanel(
                settings = currentSettings,
                onUpdateSettings= onUpdateSettings,
                textTracks = textTracks,
                audioTracks = audioTracks,
                brightness = if (brightness < 0f) 0.5f else brightness,
                volume = volume,
                onClose = { panelOpen = false },
                onInteract = { notePanelInteraction() },
                onSpeed = {
                    notePanelInteraction()
                    setSpeed(it)
                },
                onSeekStep = { seconds ->
                    notePanelInteraction()
                    onUpdateSettings { it.copy(seekStepSeconds = seconds) }
                    toast = "快进步长 ${seconds}s"
                },
                onScaleMode = {
                    notePanelInteraction()
                    setScale(it)
                },
                onTextTrack = { option ->
                    notePanelInteraction()
                    controller.selectTrack(C.TRACK_TYPE_TEXT, option)
                    tracksVersion++
                    toast = "字幕：${option.label}"
                },
                onAudioTrack = { option ->
                    notePanelInteraction()
                    controller.selectTrack(C.TRACK_TYPE_AUDIO, option)
                    tracksVersion++
                    toast = "音轨：${option.label}"
                },
                onBrightnessDelta = {
                    notePanelInteraction()
                    changeBrightness(it)
                },
                onVolumeDelta = {
                    notePanelInteraction()
                    changeVolume(it)
                },
                onToggleAutoSubtitle = {
                    notePanelInteraction()
                    onUpdateSettings { it.copy(autoSubtitle = !it.autoSubtitle) }
                    toast = "同名字幕设置已更新（下次播放生效）"
                },
                onToggleAutoNext = {
                    notePanelInteraction()
                    onUpdateSettings { it.copy(autoNext = !it.autoNext) }
                },
                onToggleRememberSpeed = {
                    notePanelInteraction()
                    onUpdateSettings { it.copy(rememberSpeed = !it.rememberSpeed) }
                },
                onLongPressSpeed = { speed ->
                    notePanelInteraction()
                    onUpdateSettings { it.copy(longPressSpeed = speed) }
                    toast = "长按倍速 ${speed}x"
                },
                onToggleShowCurrentTime = {
                    notePanelInteraction()
                    onUpdateSettings { it.copy(showCurrentTime = !it.showCurrentTime) }
                },
                onToggleShowProgressInfo = {
                    notePanelInteraction()
                    onUpdateSettings { it.copy(showProgressInfo = !it.showProgressInfo) }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 触摸手势放在最顶层：控制条显示或隐藏都能响应，只避开顶部/底部按钮区。
        PlayerGestures(
            enabled = !panelOpen && !episodePickerOpen,
            seekRangeMs = duration,
            onTap = {
                if (panelOpen) {
                    panelOpen = false
                } else {
                    controlsVisible = !controlsVisible
                }
            },
            onDoubleTap = { handleDoubleTap(it) },
            onLongPressStart = {
                if (longPressOriginalSpeed.value == null) {
                    // 保存“长按前的速度”，不是固定的 1.0x。
                    longPressOriginalSpeed.value = currentSettings.speed
                    val temp = currentSettings.longPressSpeed
                    controller.setSpeed(temp)
                    toast = "临时 ${if (temp == 1.0f) "1x" else "${temp}x"}"
                }
            },
            onLongPressEnd = {
                longPressOriginalSpeed.value?.let { original ->
                    controller.setSpeed(original)
                    longPressOriginalSpeed.value = null
                    toast = "已恢复 ${if (original == 1.0f) "1x" else "${original}x"}"
                }
            },
            onBrightnessDelta = { changeBrightness(it) },
            onVolumeDelta = { changeVolume(it) },
            onSeekStart = { startSeek() },
            onSeekPreview = { previewSeek(it) },
            onSeekEnd = { endSeek() },
            modifier = Modifier.matchParentSize()
        )

        SeekHud(seekHud, modifier = Modifier.fillMaxSize())

        AdjustmentHud(adjustment, modifier = Modifier.fillMaxSize())

        PlaybackStatusOverlay(
            currentTime = currentTimeText,
            position = position,
            duration = duration,
            showCurrentTime = currentSettings.showCurrentTime,
            showProgress = currentSettings.showProgressInfo,
            modifier = Modifier.fillMaxSize()
        )

        if (episodePickerOpen) {
            EpisodePickerDialog(
                episodes = episodes,
                onSelect = { id ->
                    episodePickerOpen = false
                    controlsVisible = true
                    if (episodes.firstOrNull { it.id == id }?.current != true) {
                        onSwitchEpisode(id)
                    }
                },
                onDismiss = { episodePickerOpen = false },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
