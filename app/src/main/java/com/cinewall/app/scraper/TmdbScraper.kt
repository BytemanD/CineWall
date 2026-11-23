package com.cinewall.app.scraper

import android.util.Log
import com.cinewall.app.model.MediaItem
import com.cinewall.app.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

const val TMDB_IMAGE_BASE = "https://image.tmdb.org/t/p/w500"

/**
 * TMDb 刮削器。
 *
 * 同时支持两种鉴权方式（与 Flutter 项目一致）：
 * - 传统 api_key：作为 query 参数发送；
 * - Read Access Token（eyJ 开头的 JWT）：用 Authorization: Bearer 发送。
 *
 * 两个官方域名互为镜像，网络失败 / 5xx 时自动切换。
 */
class TmdbScraper(
    private val apiKey: String,
    private val language: String = "zh-CN"
) : MetadataScraper {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(18, TimeUnit.SECONDS)
        .build()

    private val bearer = apiKey.startsWith("eyJ")
    private val hosts = listOf("api.tmdb.org", "api.themoviedb.org")

    override val isAvailable: Boolean = apiKey.isNotBlank()

    override suspend fun scrape(item: MediaItem): MediaItem = withContext(Dispatchers.Default) {
        try {
            when (item.mediaType) {
                MediaType.MOVIE -> scrapeMovie(item)
                MediaType.TV_SHOW -> scrapeTvShow(item)
            }
        } catch (e: Exception) {
            Log.w("TmdbScraper", "Scrape failed for ${item.fileName}", e)
            item.copy(scraped = false, source = "local")
        }
    }

    private suspend fun scrapeMovie(item: MediaItem): MediaItem {
        val wanted = TitleMatcher.cleanSearchTitle(
            item.title.ifBlank { item.fileName }
        ).ifBlank { item.fileName }
        val match = searchBest(
            "/search/movie",
            mapOf("query" to wanted, "include_adult" to "false")
                + yearParam(item.year, "year"),
            wanted,
            item.year
        ) { it.optString("title") to it.optString("original_title") }
            ?: return notMatched(item)
        val movieId = match.optInt("id")
        if (movieId <= 0) return notMatched(item)

        val detail = fetchJson(
            "/movie/$movieId",
            mapOf("append_to_response" to "external_ids"),
            language
        )
        return item.copy(
            title = nonEmpty(detail.optString("title")) ?: item.displayTitle,
            overview = detail.optString("overview", ""),
            voteAverage = detail.optDouble("vote_average", 0.0),
            year = parseYear(detail.optString("release_date")),
            posterPath = imageUrl(detail.optString("poster_path")),
            backdropPath = imageUrl(detail.optString("backdrop_path")),
            genres = genres(detail),
            scraped = true,
            externalId = movieId,
            source = "tmdb"
        )
    }

    private suspend fun scrapeTvShow(item: MediaItem): MediaItem {
        val wanted = TitleMatcher.cleanSearchTitle(
            item.title.ifBlank { item.fileName }
        ).ifBlank { item.fileName }
        val match = searchBest(
            "/search/tv",
            mapOf("query" to wanted, "include_adult" to "false")
                + yearParam(item.year, "first_air_date_year"),
            wanted,
            item.year
        ) {
            it.optString("name") to it.optString("original_name")
        } ?: return notMatched(item)
        val showId = match.optInt("id")
        if (showId <= 0) return notMatched(item)

        val detail = fetchJson(
            "/tv/$showId",
            mapOf("append_to_response" to "external_ids"),
            language
        )
        var epTitle = item.episodeTitle
        var epOverview = ""
        if (item.seasonNumber > 0 && item.episodeNumber > 0) {
            try {
                val ep = fetchJson(
                    "/tv/$showId/season/${item.seasonNumber}/episode/${item.episodeNumber}",
                    language = language
                )
                epTitle = nonEmpty(ep.optString("name")) ?: epTitle
                epOverview = ep.optString("overview", "")
            } catch (_: Exception) {
                // 单集详情失败不影响整部剧的信息
            }
        }
        return item.copy(
            title = nonEmpty(detail.optString("name")) ?: item.displayTitle,
            overview = if (epOverview.isNotBlank()) epOverview else detail.optString("overview", ""),
            voteAverage = detail.optDouble("vote_average", 0.0),
            year = parseYear(detail.optString("first_air_date")),
            seasonNumber = item.seasonNumber,
            episodeNumber = item.episodeNumber,
            episodeTitle = epTitle,
            posterPath = imageUrl(detail.optString("poster_path")),
            backdropPath = imageUrl(detail.optString("backdrop_path")),
            genres = genres(detail),
            scraped = true,
            externalId = showId,
            source = "tmdb"
        )
    }

    /**
     * 先用配置的语言搜索；匹配不到时再用 en-US 补一次，
     * 避免因为本地化标题不同直接放弃或错误兜底。
     */
    private suspend fun searchBest(
        path: String,
        params: Map<String, String>,
        wanted: String,
        year: Int,
        names: (JSONObject) -> Pair<String, String>
    ): JSONObject? {
        val primary = bestCandidate(
            fetchJson(path, params, language).optJSONArray("results"),
            wanted,
            year,
            names
        )
        if (primary != null || language.equals("en-US", ignoreCase = true)) return primary
        return try {
            bestCandidate(
                fetchJson(path, params, "en-US").optJSONArray("results"),
                wanted,
                year,
                names
            )
        } catch (e: Exception) {
            Log.d("TmdbScraper", "en-US retry failed for $wanted: ${e.message}")
            null
        }
    }

    private fun bestCandidate(
        results: org.json.JSONArray?,
        wanted: String,
        year: Int,
        names: (JSONObject) -> Pair<String, String>
    ): JSONObject? {
        if (results == null || results.length() == 0) return null
        var best: JSONObject? = null
        var bestScore = 0.0
        for (i in 0 until results.length()) {
            val candidate = results.optJSONObject(i) ?: continue
            val (title, original) = names(candidate)
            val candidateYear = parseYear(
                candidate.optString("release_date").ifBlank { candidate.optString("first_air_date") }
            ).takeIf { it > 0 }
            val score = TitleMatcher.score(
                candidateTitles = listOf(title, original),
                candidateYear = candidateYear,
                wantedTitle = wanted,
                wantedYear = year.takeIf { it > 0 },
                extra = (candidate.optDouble("popularity", 0.0) / 5000.0).coerceIn(0.0, 0.05)
            )
            if (score > bestScore) {
                bestScore = score
                best = candidate
            }
        }
        return best?.takeIf { bestScore >= TitleMatcher.THRESHOLD }
    }

    private suspend fun fetchJson(
        path: String,
        params: Map<String, String> = emptyMap(),
        language: String = "zh-CN"
    ): JSONObject = withContext(Dispatchers.IO) {
        var lastError: Exception? = null
        for (host in hosts) {
            val url = "https://$host/3$path".toHttpUrl().newBuilder().apply {
                addQueryParameter("language", language)
                params.forEach { (k, v) -> addQueryParameter(k, v) }
                if (!bearer) addQueryParameter("api_key", apiKey)
            }.build()
            val request = Request.Builder().url(url)
                .header("Accept", "application/json")
                .apply { if (bearer) header("Authorization", "Bearer $apiKey") }
                .build()
            try {
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (response.code == 401) throw IOException("TMDB API Key 无效")
                    if (!response.isSuccessful) throw IOException("TMDB HTTP ${response.code}")
                    if (body.isBlank()) throw IOException("TMDB 返回空数据")
                    return@withContext JSONObject(body)
                }
            } catch (e: Exception) {
                lastError = e
                // 401/404 等业务错误无需换域名
                if (e.message?.contains("API Key 无效") == true) throw e
            }
        }
        throw lastError ?: IOException("TMDB 请求失败")
    }

    private fun yearParam(year: Int, key: String) =
        if (year > 0) mapOf(key to year.toString()) else emptyMap()

    private fun notMatched(item: MediaItem) = item.copy(scraped = false, source = "tmdb")

    private fun imageUrl(path: String): String =
        path.takeIf { it.isNotBlank() && !it.startsWith("http") }?.let { "$TMDB_IMAGE_BASE$it" } ?: path

    private fun genres(detail: JSONObject): String {
        val arr = detail.optJSONArray("genres") ?: return ""
        return (0 until arr.length()).mapNotNull { arr.optJSONObject(it)?.optString("name") }
            .filter { it.isNotBlank() }
            .joinToString(",")
    }

    private fun nonEmpty(value: String?): String? = value?.trim()?.takeIf { it.isNotEmpty() }

    private fun parseYear(date: String): Int = date.substringBefore('-').toIntOrNull() ?: 0
}
