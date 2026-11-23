package com.cinewall.app.utils

import com.cinewall.app.model.MediaItem
import com.cinewall.app.model.MediaType
import com.cinewall.app.scanner.TvShowMatcher
import java.net.URLDecoder

/**
 * 首页 / 媒体库的展示层聚合。
 *
 * 数据库里每一集是一个独立的 MediaItem，但海报墙应该一整部剧只占一个卡片。
 * 这里按文件名去掉季集标记后的剧名分组，每组生成一个代表卡片；
 * 电影保持原样。代表卡片保留组内真实条目的 id，
 * 这样点进详情页后仍能按剧名取出全部集数。
 */
fun List<MediaItem>.aggregateForDisplay(): List<MediaItem> {
    val movies = mutableListOf<MediaItem>()
    val showGroups = LinkedHashMap<String, MutableList<MediaItem>>()

    for (item in this) {
        if (item.mediaType == MediaType.MOVIE) {
            movies.add(item)
        } else {
            val key = TvShowMatcher.showTitle(item.fileName)
            showGroups.getOrPut(key) { mutableListOf() }.add(item)
        }
    }

    val showCards = showGroups.values.map { buildShowCard(it) }
    return (movies + showCards).sortedByDescending { it.scannedAt }
}

private fun buildShowCard(group: List<MediaItem>): MediaItem {
    val ordered = group.sortedWith(
        compareBy<MediaItem> { it.seasonNumber }.thenBy { it.episodeNumber }
    )
    val firstEpisode = ordered.first()

    // 优先沿用已刮削条目的海报 / 简介 / 评分；没有则退回首集
    val scraped = ordered.firstOrNull { it.scraped }
    val base = scraped ?: firstEpisode

    // 详情页靠 fileName 反查全部集数，这里必须保留从文件名提取的公共剧名
    val fileShowTitle = TvShowMatcher.showTitle(firstEpisode.fileName)
    // 展示标题优先用剧集所在文件夹名（如「聪明镇[全10集]」），
    // 其次才用刮削结果，避免某一集刮错导致整部剧标题变成别的剧。
    val displayTitle = folderShowTitle(firstEpisode.filePath)
        ?: scraped?.title?.takeIf { it.isNotBlank() }?.let { TvShowMatcher.showTitle(it) }
        ?: fileShowTitle

    return base.copy(
        // 用真实 id，详情页才能查到条目并按剧名聚合出所有集
        id = firstEpisode.id,
        title = displayTitle,
        fileName = fileShowTitle,
        mediaType = MediaType.TV_SHOW,
        seasonNumber = 0,
        episodeNumber = 0,
        episodeTitle = "",
        subtitle = "共 ${group.size} 集"
    )
}

/** 太通用的上级目录名不能当剧名 */
private val GENERIC_FOLDER_NAMES = setOf(
    "Movies", "Movie", "Video", "Videos", "TV", "TV Shows", "Download", "Downloads",
    "电影", "剧集", "电视剧", "视频", "媒体库", "本地磁盘", "U盘"
)

/** 从文件路径 / SAF Uri 里取视频所在文件夹名作为剧名，并去掉「[全10集]」这类后缀 */
private fun folderShowTitle(filePath: String): String? {
    val decoded = runCatching {
        URLDecoder.decode(filePath.replace("+", "%2B"), "UTF-8")
    }.getOrDefault(filePath)
    val withoutTree = decoded.substringAfter("primary:", decoded)
    val parent = withoutTree.substringBeforeLast('/').substringAfterLast('/').trim()
    if (parent.isBlank() || parent.equals("primary", ignoreCase = true)) return null
    if (GENERIC_FOLDER_NAMES.any { it.equals(parent, ignoreCase = true) }) return null
    val cleaned = parent
        .replace(
            Regex("[\\[（(]\\s*(全\\s*\\d+\\s*集|第?\\s*[一二三四五六七八九十\\d]+\\s*季)\\s*[\\]）)]"),
            ""
        )
        .trim(' ', '-', '_', '.')
    return cleaned.takeIf { it.isNotBlank() }
}
