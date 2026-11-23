package com.cinewall.app.ui.player

import android.content.Context
import android.media.AudioManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

@Composable
fun PlayerScreen(
    mediaId: String,
    repository: MediaRepository,
    onNavigateBack: () -> Unit,
    onBrightnessChanged: (Float) -> Unit = {},
    modifier: Modifier = Modifier
) {
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
    Column(modifier = modifier.fillMaxSize().background(Color.Black)) {
        val filePath = item?.filePath.orEmpty()
        if (filePath.isNotEmpty() && item != null) {
            // filePath 变化（换集）时重建播放器；改速度/字幕不会打断播放
            key(filePath) {
                PlaybackContent(
                    title = item.displayTitle,
                    fileName = item.fileName,
                    filePath = filePath,
                    settings = settings,
                    previousId = previousId,
                    nextId = nextId,
                    onSwitchEpisode = { currentId = it },
                    onNavigateBack = onNavigateBack,
                    onBrightnessChanged = onBrightnessChanged,
                    onUpdateSettings = { transform -> updateSettings(transform) }
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
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
}

@Composable
private fun PlaybackContent(
    title: String,
    fileName: String,
    filePath: String,
    settings: PlaybackSettings,
    previousId: String?,
    nextId: String?,
    onSwitchEpisode: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onBrightnessChanged: (Float) -> Unit,
    onUpdateSettings: ((PlaybackSettings) -> PlaybackSettings) -> Unit
) {
    val context = LocalContext.current
    val controller = remember { PlayerController(context) }
    val currentSettings by rememberUpdatedState(settings)
    val currentNextId by rememberUpdatedState(nextId)
    val currentSwitch by rememberUpdatedState(onSwitchEpisode)
    val currentUpdate by rememberUpdatedState(onUpdateSettings)

    var controlsVisible by remember { mutableStateOf(true) }
    var panelOpen by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf("") }
    // 横向拖动预览：松手前不真正跳转，松手才 seek
    var seekBase by remember { mutableStateOf<Long?>(null) }
    var seekTarget by remember { mutableStateOf(0L) }
    var seekDelta by remember { mutableStateOf(0L) }
    var seekHud by remember { mutableStateOf("") }
    var tracksVersion by remember { mutableStateOf(0) }
    var position by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var isPlaying by remember { mutableStateOf(false) }

    var brightness by remember { mutableStateOf(-1f) } // -1=跟随系统
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    val maxVolume = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 0
    var volume by remember {
        val current = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
        mutableStateOf(if (maxVolume > 0) current.toFloat() / maxVolume else 0.5f)
    }

    DisposableEffect(Unit) {
        onDispose { controller.release() }
    }

    LaunchedEffect(Unit) {
        controller.autoNext = currentSettings.autoNext
        controller.rememberSpeed = currentSettings.rememberSpeed
        controller.onEnded = { currentNextId?.let { currentSwitch(it) } }
        controller.onSpeedChanged = { speed ->
            currentUpdate { it.copy(speed = speed) }
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
            ScaleMode.from(currentSettings.scaleMode)
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
            delay(250)
        }
    }

    LaunchedEffect(controlsVisible, panelOpen) {
        if (controlsVisible && !panelOpen) {
            delay(4000)
            controlsVisible = false
        }
    }

    LaunchedEffect(toast) {
        if (toast.isNotBlank()) {
            delay(900)
            toast = ""
        }
    }

    fun changeBrightness(delta: Float) {
        val next = if (brightness < 0f) 0.5f + delta else (brightness + delta).coerceIn(0.05f, 1f)
        brightness = next
        onBrightnessChanged(next)
        toast = "亮度 ${(next * 100).roundToInt()}%"
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
        toast = "音量 ${(next * 100).roundToInt()}%"
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

    fun cycleScale() {
        val modes = ScaleMode.entries
        val next = modes[(modes.indexOf(ScaleMode.from(currentSettings.scaleMode)) + 1) % modes.size]
        setScale(next)
    }

    val textTracks = remember(panelOpen, tracksVersion) {
        if (panelOpen) controller.trackOptions(C.TRACK_TYPE_TEXT) else emptyList()
    }
    val audioTracks = remember(panelOpen, tracksVersion) {
        if (panelOpen) controller.trackOptions(C.TRACK_TYPE_AUDIO) else emptyList()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        VideoPlayer(controller = controller, modifier = Modifier.fillMaxSize())

        PlayerGestures(
            enabled = !panelOpen,
            seekRangeMs = duration,
            onBrightnessDelta = { changeBrightness(it) },
            onVolumeDelta = { changeVolume(it) },
            onSeekStart = { startSeek() },
            onSeekPreview = { previewSeek(it) },
            onSeekEnd = { endSeek() },
            onTap = { controlsVisible = !controlsVisible },
            onDoubleTap = { fraction ->
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
            },
            onMenu = {
                panelOpen = true
                controlsVisible = true
            },
            modifier = Modifier.matchParentSize()
        )

        if (controlsVisible) {
            PlayerControls(
                title = title,
                position = position,
                duration = duration,
                isPlaying = isPlaying,
                scaleMode = ScaleMode.from(currentSettings.scaleMode),
                brightness = if (brightness < 0f) 0.5f else brightness,
                volume = volume,
                toast = toast,
                onBack = onNavigateBack,
                onTogglePlay = { controller.playPause() },
                onPrevious = {
                    if (previousId != null) onSwitchEpisode(previousId!!) else toast = "没有上一集了"
                },
                onNext = {
                    if (nextId != null) onSwitchEpisode(nextId!!) else toast = "没有下一集了"
                },
                onOpenPanel = {
                    panelOpen = true
                    controlsVisible = true
                },
                onCycleScale = { cycleScale() },
                onSeekFraction = { fraction -> controller.seekTo((duration * fraction).toLong()) },
                onKeySeek = { delta -> controller.seekBy(delta) }
            )
        }

        SeekHud(seekHud, modifier = Modifier.fillMaxSize())

        if (panelOpen) {
            SettingsPanel(
                settings = currentSettings,
                textTracks = textTracks,
                audioTracks = audioTracks,
                brightness = if (brightness < 0f) 0.5f else brightness,
                volume = volume,
                onClose = { panelOpen = false },
                onSpeed = { setSpeed(it) },
                onSeekStep = { seconds ->
                    onUpdateSettings { it.copy(seekStepSeconds = seconds) }
                    toast = "快进步长 ${seconds}s"
                },
                onScaleMode = { setScale(it) },
                onTextTrack = { option ->
                    controller.selectTrack(C.TRACK_TYPE_TEXT, option)
                    tracksVersion++
                    toast = "字幕：${option.label}"
                },
                onAudioTrack = { option ->
                    controller.selectTrack(C.TRACK_TYPE_AUDIO, option)
                    tracksVersion++
                    toast = "音轨：${option.label}"
                },
                onBrightnessDelta = { changeBrightness(it) },
                onVolumeDelta = { changeVolume(it) },
                onToggleAutoSubtitle = {
                    onUpdateSettings { it.copy(autoSubtitle = !it.autoSubtitle) }
                    toast = "同名字幕设置已更新（下次播放生效）"
                },
                onToggleAutoNext = {
                    onUpdateSettings { it.copy(autoNext = !it.autoNext) }
                },
                onToggleRememberSpeed = {
                    onUpdateSettings { it.copy(rememberSpeed = !it.rememberSpeed) }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
