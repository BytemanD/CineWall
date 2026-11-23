package com.cinewall.app.scraper

import java.util.Locale

/**
 * 从 Flutter 项目 lib/data/scraper 移植的标题匹配逻辑。
 * TMDb 和豆瓣共用，避免把搜索结果第一条直接当成匹配结果。
 */
internal object TitleMatcher {
    /** 命中阈值，低于此分数认为「没找到」，交给下一数据源兜底。 */
    const val THRESHOLD = 0.62

    fun normalize(value: String): String {
        var lower = value.trim().lowercase(Locale.ROOT)
        lower = lower.replace(Regex("^(the|a|an)\\s+"), "")
        return lower.replace(Regex("[^0-9a-z\\u4e00-\\u9fff]+"), "")
    }

    /** 候选与文件名的加权相似度；extra 是各数据源自己的加分项（热度 / 评分 / 排序）。 */
    fun score(
        candidateTitles: Collection<String>,
        candidateYear: Int?,
        wantedTitle: String,
        wantedYear: Int?,
        extra: Double = 0.0
    ): Double {
        val wanted = normalize(wantedTitle)
        if (wanted.isEmpty()) return 0.0
        var best = 0.0
        for (raw in candidateTitles) {
            val name = normalize(raw)
            if (name.isEmpty()) continue
            var current = when {
                name == wanted -> 1.0
                name.contains(wanted) || wanted.contains(name) -> 0.72
                else -> tokenScore(wanted, name)
            }
            if (candidateYear != null && wantedYear != null && wantedYear > 0) {
                val diff = kotlin.math.abs(wantedYear - candidateYear)
                current += when {
                    diff == 0 -> 0.15
                    diff == 1 -> 0.06
                    diff > 3 -> -0.2
                    else -> 0.0
                }
            }
            if (current > best) best = current
        }
        return best + extra
    }

    /** 豆瓣专用兜底：拉丁片名常对应中文标题，相似度天然为 0，此时相信豆瓣相关度排序。 */
    fun hasCjk(value: String): Boolean =
        Regex("[\\u4e00-\\u9fff\\u3040-\\u30ff\\uac00-\\ud7af]").containsMatchIn(value)

    /**
     * 把文件名式标题清洗成可搜索的片名：
     * `Bloody.Smart.S01E05.Episode.5` → `Bloody Smart`
     */
    fun cleanSearchTitle(value: String): String {
        var result = value.trim()
            .replace(Regex("[._]+"), " ")
            .replace(Regex("\\[[^\\]]*\\]"), " ")
            .replace(Regex("\\([^)]*\\)"), " ")
            .replace(Regex("(?i)\\bS\\d{1,2}\\s*E\\d{1,3}\\b.*$"), "")
            .replace(Regex("(?i)\\bSeason\\s*\\d+.*$"), "")
            .replace(Regex("(?i)\\bEpisode\\s*\\d+.*$"), "")
            .replace(Regex("(?i)\\b\\d{3,4}[pi]\\b.*$"), "")
        result = result.replace(Regex("\\s+"), " ").trim(' ', '-')
        return result
    }

    private fun tokenScore(a: String, b: String): Double {
        val aTokens = tokens(a)
        val bTokens = tokens(b)
        if (aTokens.isEmpty() || bTokens.isEmpty()) return 0.0
        val union = aTokens.union(bTokens).size
        return if (union == 0) 0.0 else aTokens.intersect(bTokens).size.toDouble() / union * 0.75
    }

    private fun tokens(value: String): Set<String> =
        value.split(Regex("[^0-9a-z\\u4e00-\\u9fff]+")).filter { it.isNotEmpty() }.toSet()
}
