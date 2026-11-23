package com.cinewall.app.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.cinewall.app.db.DateConverter
import com.cinewall.app.db.MediaTypeConverter
import java.util.Date
import java.util.UUID

/** 媒体项：直接作为 Room 实体 */
@Entity(tableName = "media_items")
@TypeConverters(DateConverter::class, MediaTypeConverter::class)
data class MediaItem(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    /** 原始文件绝对路径 */
    val filePath: String = "",
    /** 文件名（不带扩展名） */
    val fileName: String = "",
    /** 识别出的类型：电影 / 剧集 */
    val mediaType: MediaType = MediaType.MOVIE,
    /** 刮削后的标题（优先使用刮削结果，否则回退到文件名） */
    val title: String = "",
    /** 副标题 / 年份 */
    val subtitle: String = "",
    /** 概述 */
    val overview: String = "",
    /** 评分 */
    val voteAverage: Double = 0.0,
    /** 上映/首播年份 */
    val year: Int = 0,
    /** 剧集季号（仅 TV_SHOW） */
    val seasonNumber: Int = 0,
    /** 剧集集号（仅 TV_SHOW） */
    val episodeNumber: Int = 0,
    /** 剧集名（仅 TV_SHOW） */
    val episodeTitle: String = "",
    /** 海报路径（TMDb 路径或本地文件路径） */
    val posterPath: String = "",
    /** 背景图/扇区图路径 */
    val backdropPath: String = "",
    /** 导演 */
    val director: String = "",
    /** 演员 */
    val cast: String = "",
    /** 类型 */
    val genres: String = "",
    /** 时长（秒） */
    val durationMs: Long = 0,
    /** 文件大小（字节） */
    val fileSize: Long = 0,
    /** 最后扫描时间 */
    val scannedAt: Date = Date(),
    /** 是否已刮削元数据 */
    val scraped: Boolean = false,
    /** 外部 ID（如 TMDb ID） */
    val externalId: Int = 0,
    /** 外部数据源（tmdb / local / none） */
    val source: String = "local"
) {
    companion object {
        val VIDEO_EXTENSIONS = setOf(
            "mp4", "mkv", "avi", "mov", "wmv", "flv", "ts", "m2ts", "mpg", "mpeg",
            "m4v", "3gp", "webm", "vob", "rmvb", "ogv"
        )
    }

    /** 判断文件是否为视频 */
    fun isVideo(): Boolean = VIDEO_EXTENSIONS.contains(fileName.substringAfterLast('.').lowercase())

    /** 便捷显示名称 */
    val displayTitle: String
        get() = if (title.isNotBlank()) title else fileName

    /** 便捷副标题 */
    val displaySubtitle: String
        get() = buildString {
            if (year > 0) append(year.toString())
            if (mediaType == MediaType.TV_SHOW && seasonNumber > 0) {
                if (isNotEmpty()) append(" · ")
                append("S${String.format("%02d", seasonNumber)}")
            }
            if (mediaType == MediaType.TV_SHOW && episodeNumber > 0) {
                if (isNotEmpty()) append(" · ")
                append("E${String.format("%02d", episodeNumber)}")
            }
            if (isEmpty() && subtitle.isNotBlank()) append(subtitle)
        }
}
