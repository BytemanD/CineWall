package com.cinewall.app.scraper

import android.util.Log
import com.cinewall.app.model.MediaItem
import com.cinewall.app.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

private const val DOUBAN_HOST = "https://api.douban.com/v2"

/**
 * 豆瓣电影 API v2 刮削器，逻辑来自 Flutter 参考项目 lib/data/scraper/douban_client.dart。
 * TMDb 找不到（或不可用）时作为兜底数据源。
 */
class DoubanScraper(private val apiKey: String) : MetadataScraper {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(18, TimeUnit.SECONDS)
        .build()

    override val isAvailable: Boolean = apiKey.isNotBlank()

    private data class Candidate(
        val id: String,
        val title: String,
        val originalTitle: String,
        val subtype: String,
        val year: Int?,
        val rating: Double?,
        val poster: String?,
        val backdrop: String?,
        val overview: String?,
        val genres: List<String>,
        val aka: List<String>,
        val episodeCount: Int?,
        val durationMs: Long = 0L
    ) {
        fun mergedWith(other: Candidate) = copy(
            year = year ?: other.year,
            rating = rating ?: other.rating,
            poster = poster ?: other.poster,
            backdrop = backdrop ?: other.backdrop,
            overview = overview ?: other.overview,
            genres = genres.ifEmpty { other.genres },
            aka = aka.ifEmpty { other.aka },
            episodeCount = episodeCount ?: other.episodeCount
        )
    }

    override suspend fun scrape(item: MediaItem): MediaItem = withContext(Dispatchers.Default) {
        Log.d(
            "DoubanScraper",
            "=== scrape start: id=${item.id} fileName=${item.fileName} mediaType=${item.mediaType} " +
                "title=${item.title} season=${item.seasonNumber} episode=${item.episodeNumber}"
        )
        try {
            val title = TitleMatcher.cleanSearchTitle(
                item.title.ifBlank { item.fileName }
            ).ifBlank { item.fileName }
            Log.d("DoubanScraper", "scrape: cleaned title=$title year=${item.year}")
            val candidates = search(title, item.year)
                .filter { if (item.mediaType == MediaType.MOVIE) it.subtype != "tv" else it.subtype == "tv" }
            Log.d("DoubanScraper", "scrape: search candidates count=${candidates.size}")
            val match = best(candidates, title, item.year) ?: return@withContext item.copy(
                scraped = false,
                source = "douban"
            )
            val details = try {
                subject(match.id)?.mergedWith(match) ?: match
            } catch (e: Exception) {
                Log.w("DoubanScraper", "subject(${match.id}) failed", e)
                match
            }
            Log.d("DoubanScraper", "scrape: matched id=${details.id} title=${details.title} rating=${details.rating}")
            item.copy(
                title = nonEmpty(details.title) ?: title,
                year = details.year ?: item.year,
                overview = details.overview.orEmpty(),
                voteAverage = details.rating ?: 0.0,
                posterPath = details.poster.orEmpty(),
                backdropPath = details.backdrop.orEmpty(),
                genres = details.genres.joinToString(","),
                durationMs = if (details.durationMs > 0) details.durationMs else item.durationMs,
                scraped = true,
                externalId = details.id.toIntOrNull() ?: 0,
                source = "douban"
            )
        } catch (e: Exception) {
            Log.w("DoubanScraper", "Scrape failed for ${item.fileName}", e)
            item.copy(scraped = false, source = "douban")
        }
    }

    private suspend fun search(title: String, year: Int): List<Candidate> {
        val params = mutableMapOf("q" to title)
        if (year > 0) params["year"] = year.toString()
        val json = post("/movie/search", params)
        val subjects = json.optJSONArray("subjects") ?: return emptyList()
        return (0 until subjects.length()).mapNotNull { subjects.optJSONObject(it)?.let(::parse) }
            .filter { it.id.isNotBlank() && it.title.isNotBlank() }
    }

    private suspend fun subject(id: String): Candidate? {
        val json = post("/movie/subject/$id")
        return parse(json)?.takeIf { it.id.isNotBlank() && it.title.isNotBlank() }
    }

    private suspend fun post(path: String, params: Map<String, String> = emptyMap()): JSONObject =
        withContext(Dispatchers.IO) {
            val urlBuilder = "$DOUBAN_HOST$path".toHttpUrl().newBuilder().apply {
                addQueryParameter("apikey", apiKey)
                params.forEach { (k, v) -> addQueryParameter(k, v) }
            }
            val url = urlBuilder.build()
            Log.d("DoubanScraper", "Douban API request: POST ${url.toString()} params=$params")
            val body = FormBody.Builder().add("apikey", apiKey).build()
            val request = Request.Builder().url(url).post(body)
                .header("Accept", "application/json")
                .header("apikey", apiKey)
                .build()
            var lastError: Exception? = null
            repeat(3) { attempt ->
                try {
                    client.newCall(request).execute().use { response ->
                        val text = response.body?.string().orEmpty()
                        Log.d("DoubanScraper", "Douban API response: code=${response.code} body=${text.take(500)}")
                        if (response.code == 401 || response.code == 403) {
                            throw IOException("豆瓣 API Key 无效")
                        }
                        if (!response.isSuccessful) {
                            throw IOException("豆瓣 HTTP ${response.code}: ${text.take(120)}")
                        }
                        return@withContext JSONObject(text)
                    }
                } catch (e: Exception) {
                    lastError = e
                    if (attempt < 2) kotlinx.coroutines.delay(500)
                }
            }
            throw lastError ?: IOException("豆瓣请求失败")
        }

    private fun parse(json: JSONObject): Candidate {
        val rating = json.optJSONObject("rating")
        val images = json.optJSONObject("images")
        val durations = json.optJSONArray("durations")
        val durationMs = if (durations != null && durations.length() > 0) {
            durations.optInt(0, 0) * 60_000L
        } else 0L
        Log.d("DoubanScraper", "parse: id=${json.opt("id")} durationMs=$durationMs")
        return Candidate(
            id = text(json.opt("id")),
            title = text(json.opt("title")),
            originalTitle = text(json.opt("original_title")),
            subtype = text(json.opt("subtype")),
            year = json.opt("year")?.toString()?.substringBefore('-')?.toIntOrNull(),
            rating = rating?.opt("average")?.toString()?.toDoubleOrNull(),
            poster = imageUrl(images?.opt("medium")?.toString()),
            backdrop = imageUrl(images?.opt("large")?.toString()),
            overview = text(json.opt("summary")).takeIf { it.isNotBlank() },
            genres = names(json.optJSONArray("genres")),
            aka = names(json.optJSONArray("aka")),
            episodeCount = json.opt("episodes_count")?.toString()?.toIntOrNull(),
            durationMs = durationMs
        )
    }

    private fun best(candidates: List<Candidate>, title: String, year: Int): Candidate? {
        if (candidates.isEmpty()) return null
        var best: Candidate? = null
        var bestScore = 0.0
        val scored = candidates.mapIndexed { index, candidate ->
            val extra = (candidate.rating ?: 0.0).coerceIn(0.0, 10.0) / 200.0 +
                (candidates.size - index).toDouble() / (candidates.size * 8)
            val score = TitleMatcher.score(
                candidateTitles = listOf(candidate.title, candidate.originalTitle) + candidate.aka,
                candidateYear = candidate.year,
                wantedTitle = title,
                wantedYear = year.takeIf { it > 0 },
                extra = extra
            )
            if (score > bestScore) {
                bestScore = score
                best = candidate
            }
            candidate to score
        }
        if (best != null && bestScore >= TitleMatcher.THRESHOLD) return best

        // 弱兜底也必须有一定标题相似度，绝不能因为「拉丁标题」就无脑取第一条，
        // 否则中文剧的英文名会被刮成任意一部豆瓣结果。
        val minLooseScore = 0.30
        if (year > 0) {
            scored.firstOrNull { (candidate, score) ->
                score >= minLooseScore && candidate.year != null &&
                    kotlin.math.abs(candidate.year!! - year) <= 1
            }?.let { return it.first }
        }
        if (scored.size == 1 && bestScore >= minLooseScore) return scored.first().first
        Log.d(
            "DoubanScraper",
            "Rejected low-score match for $title (best=$bestScore, results=${candidates.size})"
        )
        return null
    }

    /** 豆瓣图片：升级 http→https，并把低清 s_ratio_poster 换成 l 大图。 */
    private fun imageUrl(value: String?): String? {
        var url = value?.trim()?.takeIf { it.isNotBlank() } ?: return null
        if (url.startsWith("http://") && url.contains("doubanio.com")) {
            url = url.replaceFirst("http://", "https://")
        }
        if (url.contains("doubanio.com") && url.contains("/view/photo/")) {
            url = url.replace("/s_ratio_poster/", "/l/")
        }
        return url
    }

    private fun text(value: Any?): String = value?.toString()?.trim() ?: ""

    private fun names(array: org.json.JSONArray?): List<String> =
        array?.let { (0 until it.length()).map { i -> it.optString(i) }.filter { it.isNotBlank() } }
            ?: emptyList()

    private fun nonEmpty(value: String?): String? = value?.trim()?.takeIf { it.isNotEmpty() }
}
