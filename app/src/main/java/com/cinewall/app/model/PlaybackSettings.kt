package com.cinewall.app.model

/** 长按播放画面时临时切换到的倍速档位。 */
val LONG_PRESS_SPEED_OPTIONS = listOf(1.5f, 2.0f, 3.0f)

/** 播放器偏好设置，统一持久化，避免多个零散的键。 */
data class PlaybackSettings(
    /** 自动加载与视频同名的 .srt/.ass/.vtt 等字幕 */
    val autoSubtitle: Boolean = true,
    /** 剧集播完后自动播放下一集 */
    val autoNext: Boolean = true,
    /** 保存并恢复播放速度 */
    val rememberSpeed: Boolean = true,
    /** 播放速度（0.5 ~ 3.0） */
    val speed: Float = 1.0f,
    /** 长按播放画面时临时使用的倍速，松开后恢复原速 */
    val longPressSpeed: Float = 2.0f,
    /** 双击左右两侧快退/快进的秒数 */
    val seekStepSeconds: Int = 10,
    /** 画面缩放模式：FIT / FILL / ZOOM */
    val scaleMode: String = "FIT",
    /** 播放时在右上角显示当前时间 */
    val showCurrentTime: Boolean = true,
    /** 播放时在右上角显示视频进度 */
    val showProgressInfo: Boolean = true
)

