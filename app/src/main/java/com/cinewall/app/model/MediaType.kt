package com.cinewall.app.model

/** 媒体类型：电影或剧集 */
enum class MediaType {
    MOVIE,
    TV_SHOW,
}

/** 扫描结果 */
data class ScanResult(
    val totalFound: Int = 0,
    val movies: Int = 0,
    val shows: Int = 0,
    val errors: List<String> = emptyList()
)
