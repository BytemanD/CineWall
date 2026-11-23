package com.cinewall.app.ui.player

import android.content.Context
import android.graphics.Color
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Format
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Tracks
import com.google.android.exoplayer2.source.TrackGroup
import com.google.android.exoplayer2.trackselection.TrackSelectionOverride
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.CaptionStyleCompat
import com.google.android.exoplayer2.ui.PlayerView

/** 画面缩放模式 */
enum class ScaleMode(val label: String, val value: Int) {
    FIT("适应画面", AspectRatioFrameLayout.RESIZE_MODE_FIT),
    FILL("铺满屏幕", AspectRatioFrameLayout.RESIZE_MODE_FILL),
    ZOOM("裁剪填充", AspectRatioFrameLayout.RESIZE_MODE_ZOOM);

    companion object {
        fun from(value: String?): ScaleMode = entries.find { it.name == value } ?: FIT
    }
}

/** 一条可选轨道（字幕/音轨） */
data class TrackOption(
    val key: String,
    val label: String,
    val groupIndex: Int = -1,
    val trackIndex: Int = -1,
    val selected: Boolean = false
)

/**
 * 播放器控制器：把 ExoPlayer 的命令封装成一组稳定的方法，
 * 供 Compose 覆盖层（手势 / 面板 / 快捷按钮）调用。
 */
class PlayerController(context: Context) {

    val player: ExoPlayer = ExoPlayer.Builder(context).build()
    var playerView: PlayerView? = null
        private set

    var autoNext: Boolean = true
    var rememberSpeed: Boolean = true
    var onEnded: () -> Unit = {}
    var onSpeedChanged: (Float) -> Unit = {}

    var scaleMode: ScaleMode = ScaleMode.FIT
        private set

    // 用户一旦在面板里手动选过字幕，就不再自动覆盖他的选择。
    private var textTrackManuallySelected = false
    private var autoSelectingTextTrack = false

    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED && autoNext) onEnded()
        }

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
            if (rememberSpeed) onSpeedChanged(playbackParameters.speed)
        }

        override fun onTracksChanged(tracks: Tracks) {
            if (!textTrackManuallySelected) autoSelectPreferredTextTrack(tracks)
        }
    }

    init {
        player.addListener(listener)
    }

    fun prepare(mediaItem: MediaItem, speed: Float, mode: ScaleMode, startPositionMs: Long = 0L) {
        scaleMode = mode
        textTrackManuallySelected = false
        playerView?.setResizeMode(mode.value)
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_TEXT)
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            .build()
        player.setMediaItem(mediaItem, startPositionMs.coerceAtLeast(0L))
        player.playbackParameters = PlaybackParameters(speed.coerceIn(0.5f, 3.0f))
        player.prepare()
        player.playWhenReady = true
    }

    fun bind(view: PlayerView) {
        playerView = view
        view.player = player
        view.useController = false // 使用自定义覆盖层
        view.setResizeMode(scaleMode.value)
        // PlayerView 默认会把 CaptionStyleCompat.DEFAULT 套到字幕上，
        // 而这个默认样式的背景是黑色；这里改成透明背景 + 黑色描边，
        // 字幕既没有黑框，在亮/暗画面上也都看得清。
        view.subtitleView?.setStyle(
            CaptionStyleCompat(
                Color.WHITE,
                Color.TRANSPARENT,
                Color.TRANSPARENT,
                CaptionStyleCompat.EDGE_TYPE_OUTLINE,
                Color.BLACK,
                null
            )
        )
    }

    fun release() {
        player.removeListener(listener)
        playerView?.player = null
        playerView = null
        player.release()
    }

    fun playPause() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun play() {
        player.playWhenReady = true
    }

    fun pause() {
        player.playWhenReady = false
    }

    val isPlaying: Boolean get() = player.isPlaying

    val position: Long get() = player.currentPosition

    val duration: Long get() = player.duration.takeIf { it != C.TIME_UNSET } ?: 0L

    fun seekBy(deltaMs: Long) {
        val total = duration
        val target = (position + deltaMs).coerceIn(0L, total.takeIf { it > 0 } ?: Long.MAX_VALUE)
        player.seekTo(target)
    }

    fun seekTo(ms: Long) {
        val total = duration
        player.seekTo(ms.coerceIn(0L, total.takeIf { it > 0 } ?: Long.MAX_VALUE))
    }

    fun setSpeed(speed: Float) {
        val value = speed.coerceIn(0.5f, 3.0f)
        player.playbackParameters = PlaybackParameters(value)
    }

    fun setScaleMode(mode: ScaleMode) {
        scaleMode = mode
        playerView?.setResizeMode(mode.value)
    }

    /** 列出指定类型（字幕/音轨）的可选项，第一项固定为“关闭/默认”。 */
    fun trackOptions(type: Int): List<TrackOption> {
        val options = mutableListOf<TrackOption>()
        options += if (type == C.TRACK_TYPE_TEXT) {
            TrackOption(key = "off", label = "关闭字幕", selected = !player.currentTracks.isTypeSelected(type))
        } else {
            TrackOption(key = "default", label = "默认音轨", selected = !hasOverride(type))
        }
        player.currentTracks.groups.forEachIndexed { groupIndex, group ->
            if (group.type != type) return@forEachIndexed
            for (i in 0 until group.length) {
                if (!group.isTrackSupported(i)) continue
                val format = group.getTrackFormat(i)
                val base = format.label?.takeIf { it.isNotBlank() }
                    ?: format.language?.takeIf { it.isNotBlank() && it != "und" }
                    ?: "轨道 ${i + 1}"
                val detail = when (type) {
                    C.TRACK_TYPE_AUDIO -> {
                        val parts = mutableListOf<String>()
                        if (format.channelCount > 0) parts += "${format.channelCount}ch"
                        if (format.bitrate > 0) parts += "${format.bitrate / 1000}kbps"
                        if (parts.isEmpty()) "" else " (${parts.joinToString(" ")})"
                    }
                    C.TRACK_TYPE_TEXT -> format.language?.takeIf { it.isNotBlank() && it != "und" }?.let { " · $it" } ?: ""
                    else -> ""
                }
                options += TrackOption(
                    key = "$groupIndex:$i",
                    label = base + detail,
                    groupIndex = groupIndex,
                    trackIndex = i,
                    selected = group.isTrackSelected(i)
                )
            }
        }
        return options
    }

    private fun hasOverride(type: Int): Boolean =
        player.trackSelectionParameters.overrides.values.any { it.type == type }

    fun selectTrack(type: Int, option: TrackOption) {
        if (type == C.TRACK_TYPE_TEXT) textTrackManuallySelected = true
        val builder = player.trackSelectionParameters.buildUpon()
        when (option.key) {
            "off" -> {
                builder.clearOverridesOfType(type).setTrackTypeDisabled(type, true)
            }
            "default" -> {
                builder.clearOverridesOfType(type).setTrackTypeDisabled(type, false)
            }
            else -> {
                val group: TrackGroup? = player.currentTracks.groups.getOrNull(option.groupIndex)?.mediaTrackGroup
                if (group != null) {
                    builder.setTrackTypeDisabled(type, false)
                        .setOverrideForType(TrackSelectionOverride(group, option.trackIndex))
                }
            }
        }
        player.trackSelectionParameters = builder.build()
    }

    /** 让 ExoPlayer 重新计算轨道（例如外挂字幕加载完成后） */
    fun refreshTracks() {
        player.trackSelectionParameters = player.trackSelectionParameters
    }

    /**
     * 自动选字幕轨：简体中文 > 繁体中文 > English > 其他。
     * 用户在面板里手动选过字幕后，不再自动覆盖。
     */
    private fun autoSelectPreferredTextTrack(tracks: Tracks) {
        if (autoSelectingTextTrack) return
        autoSelectingTextTrack = true
        try {
            var bestGroup: TrackGroup? = null
            var bestIndex = -1
            var bestRank = Int.MAX_VALUE
            tracks.groups.forEach { group ->
                if (group.type != C.TRACK_TYPE_TEXT) return@forEach
                for (i in 0 until group.length) {
                    if (!group.isTrackSupported(i)) continue
                    val rank = textTrackLanguageRank(group.getTrackFormat(i))
                    if (rank < bestRank) {
                        bestRank = rank
                        bestGroup = group.mediaTrackGroup
                        bestIndex = i
                    }
                }
            }
            val group = bestGroup ?: return
            player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                .setOverrideForType(TrackSelectionOverride(group, bestIndex))
                .build()
        } finally {
            autoSelectingTextTrack = false
        }
    }

    private fun textTrackLanguageRank(format: Format): Int {
        val raw = listOfNotNull(
            format.language?.takeIf { it.isNotBlank() },
            format.label?.takeIf { it.isNotBlank() }
        ).joinToString(" ") { it.lowercase() }

        return when {
            TRADITIONAL_CHINESE.containsMatchIn(raw) -> 1
            SIMPLIFIED_CHINESE.containsMatchIn(raw) -> 0
            GENERIC_CHINESE.containsMatchIn(raw) -> 0
            ENGLISH.containsMatchIn(raw) -> 2
            else -> 3
        }
    }

    companion object {
        private val TRADITIONAL_CHINESE = Regex(
            "zh[-_]?(hant|tw|hk|mo)|chi[-_]?t|繁體|繁体|繁中|正體|traditional"
        )
        private val SIMPLIFIED_CHINESE = Regex(
            "zh[-_]?(hans|cn|sg)|chi[-_]?s|简体|简中|simplified"
        )
        private val GENERIC_CHINESE = Regex("(^|[^a-z])(zh|zho|chi|chinese)([^a-z]|$)")
        private val ENGLISH = Regex("(^|[^a-z])(en|eng|english)([^a-z]|$)")
    }
}
