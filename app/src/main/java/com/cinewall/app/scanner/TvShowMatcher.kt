package com.cinewall.app.scanner

import java.util.regex.Pattern

/** 剧集文件名匹配器：识别 SxxExx / 1x01 / 第xx集 等常见命名模式 */
object TvShowMatcher {
    private val PATTERNS = listOf(
        // S01E01 / s01e01
        Pattern.compile("(?i)[._\\s-]s(\\d{1,3})[e](\\d{1,3})"),
        // S01E01E02
        Pattern.compile("(?i)[._\\s-]s(\\d{1,3})[e](\\d{1,3})[e](\\d{1,3})"),
        // 1x01
        Pattern.compile("(?i)[._\\s-](\\d{1,2})x(\\d{1,3})"),
        // 第01季第02集
        Pattern.compile("(?i)第\\s*(\\d{1,3})\\s*季.*?第\\s*(\\d{1,3})\\s*集"),
        // 第02集
        Pattern.compile("(?i)第\\s*(\\d{1,3})\\s*集"),
        // EP01
        Pattern.compile("(?i)[._\\s-]ep(\\d{1,3})")
    )

    data class MatchResult(
        val isTvShow: Boolean = false,
        val season: Int = 0,
        val episode: Int = 0
    )

    fun match(fileName: String): MatchResult {
        val name = fileName.substringBeforeLast('.')
        for (pattern in PATTERNS) {
            val matcher = pattern.matcher(name)
            if (matcher.find()) {
                val season = if (matcher.groupCount() >= 1) matcher.group(1).toIntOrNull() ?: 0 else 0
                val episode = if (matcher.groupCount() >= 2) matcher.group(2).toIntOrNull() ?: 0 else 0
                return MatchResult(true, season, episode)
            }
        }
        return MatchResult(false)
    }

    /** 去掉季/集标记，得到同一部剧的公共标题，用于把分散的剧集文件聚合到一起 */
    fun showTitle(fileName: String): String {
        val name = fileName.substringBeforeLast('.')
        val stripped = listOf(
            Regex("(?i)[._\\s-]*s\\d{1,3}e\\d{1,3}.*"),
            Regex("(?i)[._\\s-]*\\d{1,2}x\\d{1,3}.*"),
            Regex("(?i)[._\\s-]*ep\\d{1,3}.*"),
            Regex("第\\s*\\d{1,3}\\s*集.*")
        ).fold(name) { acc, regex -> regex.replace(acc, "") }
        return stripped.trim(' ', '.', '_', '-').ifBlank { name }
    }
}
