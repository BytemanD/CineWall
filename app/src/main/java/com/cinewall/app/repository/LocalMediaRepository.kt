package com.cinewall.app.repository

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import com.cinewall.app.BuildConfig
import com.cinewall.app.db.AppDatabase
import com.cinewall.app.model.GlobalSnackBar
import com.cinewall.app.model.MediaItem
import com.cinewall.app.model.MediaType
import com.cinewall.app.model.PlaybackSettings
import com.cinewall.app.model.ScanResult
import com.cinewall.app.scanner.MediaScanner
import com.cinewall.app.scraper.DoubanScraper
import com.cinewall.app.scraper.FallbackScraper
import com.cinewall.app.scraper.LocalScraper
import com.cinewall.app.scraper.MetadataScraper
import com.cinewall.app.scraper.TmdbScraper
import com.cinewall.app.utils.DocumentsPathUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import androidx.core.content.edit

const val PREF_NAME = "cinewall_prefs"
const val KEY_SCAN_FOLDERS = "scan_folders"
const val KEY_TMDB_API_KEY = "tmdb_api_key"
const val KEY_TMDB_LANGUAGE = "tmdb_language"
const val KEY_THEME_MODE = "theme_mode" // 0 system,1 light,2 dark
const val KEY_THEME_PALETTE = "theme_palette"
const val KEY_PLAYBACK_SETTINGS = "playback_settings"
const val KEY_PLAYBACK_POSITION = "playback_position"

const val KEY_autoScanAfterStarted = "autoScanAfterStarted"
const val KEY_autoScrapAfterScan = "autoScrapAfterScan"
const val KEY_POSTER_AUTO_SCROLL_DELAY = "autoScrollDelay"

/** 本地实现：Room + SharedPreferences */
class LocalMediaRepository(private val context: Context, private val scope: CoroutineScope) : MediaRepository {

    private val db = AppDatabase.getDatabase(context)
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }
    private val scanner = MediaScanner(context)

    override fun getMediaByType(type: MediaType): Flow<List<MediaItem>> =
        db.mediaDao().getByType(type.name)

    override fun getAllMedia(): Flow<List<MediaItem>> =
        db.mediaDao().getAll()

    override fun getScrapedMedia(): Flow<List<MediaItem>> =
        db.mediaDao().getScraped()

    override suspend fun count(): Int = db.mediaDao().count()

    override suspend fun getItemById(id: String): MediaItem? = db.mediaDao().getById(id)

    override fun saveScanFolders(uris: List<String>) {
        prefs.edit().putStringSet(KEY_SCAN_FOLDERS, uris.toMutableSet()).apply()
    }

    override fun loadScanFolders(): List<String> =
        (prefs.getStringSet(KEY_SCAN_FOLDERS, emptySet()) ?: emptySet()).toList()

    override fun removeScanFolder(uri: String) {
        prefs.edit().apply {
            val current = loadScanFolders().toMutableSet()
            current.remove(uri)
            putStringSet(KEY_SCAN_FOLDERS, current)
        }.apply()
        runCatching {
            val parsed = Uri.parse(uri)
            // 自绘目录浏览器保存的是 file:// 路径，没有可释放的 SAF 授权
            if (parsed.scheme != "content") return@runCatching
            val read = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            val write = android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            try {
                context.contentResolver.releasePersistableUriPermission(parsed, read or write)
            } catch (_: SecurityException) {
                context.contentResolver.releasePersistableUriPermission(parsed, read)
            }
        }
    }

    override fun saveTmdbApiKey(key: String?) {
        prefs.edit().putString(KEY_TMDB_API_KEY, key ?: "").apply()
    }

    /** 手动覆盖值优先；没有时用构建期写入 BuildConfig 的密钥（安装即可用） */
    override fun loadTmdbApiKey(): String? =
        prefs.getString(KEY_TMDB_API_KEY, null)?.takeIf { it.isNotBlank() }
            ?: BuildConfig.TMDB_API_KEY.takeIf { it.isNotBlank() }

    override fun loadDoubanApiKey(): String? =
        BuildConfig.DOUBAN_API_KEY.takeIf { it.isNotBlank() }

    override fun saveTmdbLanguage(code: String) {
        prefs.edit().putString(KEY_TMDB_LANGUAGE, code).apply()
    }

    override fun loadTmdbLanguage(): String =
        prefs.getString(KEY_TMDB_LANGUAGE, "zh-CN") ?: "zh-CN"

    override fun isUsingBuiltInApiKey(): Boolean =
        prefs.getString(KEY_TMDB_API_KEY, null).isNullOrBlank()

    override fun saveThemeMode(mode: Int) {
        prefs.edit { putInt(KEY_THEME_MODE, mode) }
    }

    override fun loadThemeMode(): Int = prefs.getInt(KEY_THEME_MODE, 0)

    override fun saveThemePalette(id: String) {
        prefs.edit().putString(KEY_THEME_PALETTE, id).apply()
    }

    override fun loadThemePalette(): String = prefs.getString(KEY_THEME_PALETTE, "classic") ?: "classic"

    override fun loadPlaybackSettings(): PlaybackSettings = PlaybackSettings(
        autoSubtitle = prefs.getBoolean("${KEY_PLAYBACK_SETTINGS}_subtitle", true),
        autoNext = prefs.getBoolean("${KEY_PLAYBACK_SETTINGS}_next", true),
        rememberSpeed = prefs.getBoolean("${KEY_PLAYBACK_SETTINGS}_remember_speed", true),
        speed = prefs.getFloat("${KEY_PLAYBACK_SETTINGS}_speed", 1.0f),
        longPressSpeed = prefs.getFloat("${KEY_PLAYBACK_SETTINGS}_long_press_speed", 2.0f),
        seekStepSeconds = prefs.getInt("${KEY_PLAYBACK_SETTINGS}_seek_step", 10),
        scaleMode = prefs.getString("${KEY_PLAYBACK_SETTINGS}_scale_mode", "FIT") ?: "FIT",
        showCurrentTime = prefs.getBoolean("${KEY_PLAYBACK_SETTINGS}_show_time", true),
        showProgressInfo = prefs.getBoolean("${KEY_PLAYBACK_SETTINGS}_show_progress", true) ,
   )

    override fun savePlaybackSettings(settings: PlaybackSettings) {
        prefs.edit()
            .putBoolean("${KEY_PLAYBACK_SETTINGS}_subtitle", settings.autoSubtitle)
            .putBoolean("${KEY_PLAYBACK_SETTINGS}_next", settings.autoNext)
            .putBoolean("${KEY_PLAYBACK_SETTINGS}_remember_speed", settings.rememberSpeed)
            .putFloat("${KEY_PLAYBACK_SETTINGS}_speed", settings.speed)
            .putFloat("${KEY_PLAYBACK_SETTINGS}_long_press_speed", settings.longPressSpeed)
            .putInt("${KEY_PLAYBACK_SETTINGS}_seek_step", settings.seekStepSeconds)
            .putString("${KEY_PLAYBACK_SETTINGS}_scale_mode", settings.scaleMode)
            .putBoolean("${KEY_PLAYBACK_SETTINGS}_show_time", settings.showCurrentTime)
            .putBoolean("${KEY_PLAYBACK_SETTINGS}_show_progress", settings.showProgressInfo)
            .apply()
    }

    override fun loadPlaybackPosition(mediaId: String): Long =
        prefs.getLong("${KEY_PLAYBACK_POSITION}_$mediaId", 0L)

    override fun savePlaybackPosition(mediaId: String, positionMs: Long) {
        prefs.edit().putLong("${KEY_PLAYBACK_POSITION}_$mediaId", positionMs.coerceAtLeast(0L)).apply()
    }

    private fun currentScraper(): MetadataScraper = buildScraper()

    /** TMDb 优先，找不到 / 失败时自动用豆瓣，两者都没配置则用本地信息 */
    private fun buildScraper(): MetadataScraper {
        val tmdbKey = loadTmdbApiKey()
        val tmdbLanguage = loadTmdbLanguage()
        val douban = DoubanScraper(loadDoubanApiKey().orEmpty())
        return when {
            !tmdbKey.isNullOrBlank() -> FallbackScraper(TmdbScraper(tmdbKey, tmdbLanguage), douban)
            douban.isAvailable -> douban
            else -> LocalScraper
        }
    }
    private var _scanning = false
    private var _scraping = false
    override val scanning: Boolean get() = _scanning
    override val scraping: Boolean get() = _scraping

    override suspend fun scanFolders(): ScanResult {
        _scanning = true

        val uris = loadScanFolders()

        if (uris.isEmpty()) return ScanResult()

        val allItems = mutableListOf<MediaItem>()
        for (uriStr in uris) {
            val uri = Uri.parse(uriStr)
            val scanned = when (uri.scheme) {
                // SAF 目录树：用 DocumentFile 遍历授权目录（Android 11+ 最可靠）
                "content" -> scanner.scanTreeUri(uri).ifEmpty { scanner.scanViaMediaStore(uri) }
                else -> realFileFromUri(uri)?.let { scanner.scanDirectory(it) } ?: emptyList()
            }
            allItems.addAll(scanned)
        }

        // 去重（按文件路径）
        val distinct = allItems.distinctBy { it.filePath }

        var newCount = 0
        var removedCount = 0

        withContext(Dispatchers.IO) {
            // 读取存量数据（含不可用记录），按 filePath 建立索引
            val existing = db.mediaDao().getAllIncludeUnavailable().associateBy { it.filePath }
            val scannedPaths = distinct.map { it.filePath }.toSet()

            // 1. 数据库中存在但本次扫描未命中的文件，标记为不可用（不物理删除，保留刮削数据）
            val missingPaths = existing.keys.filter { it !in scannedPaths }
            removedCount = missingPaths.size
            if (missingPaths.isNotEmpty()) {
                Log.d("CineWallScan", "mark unavailable: ${missingPaths.size} items")
                db.mediaDao().updateAvailability(missingPaths, false)
            }

            // 2. 本次扫描到但数据库中已存在且标记为不可用：恢复为可用，保留原有刮削数据
            val toRestore = distinct.filter {
                it.filePath in existing && !existing[it.filePath]!!.isAvailable
            }
            if (toRestore.isNotEmpty()) {
                Log.d("CineWallScan", "restore available: ${toRestore.size} items")
                val restoreItems = toRestore.map { item ->
                    val old = existing[item.filePath]!!
                    item.copy(
                        isAvailable = true,
                        // 保留旧的刮削数据，避免重新刮削
                        scraped = old.scraped,
                        title = old.title,
                        overview = old.overview,
                        posterPath = old.posterPath,
                        backdropPath = old.backdropPath,
                        voteAverage = old.voteAverage,
                        year = old.year,
                        genres = old.genres,
                        director = old.director,
                        cast = old.cast,
                        externalId = old.externalId,
                        source = old.source,
                        // 保留旧的扫描时间
                        scannedAt = old.scannedAt
                    )
                }
                db.mediaDao().insert(restoreItems)
            }

            // 3. 本次扫描到且数据库中完全不存在：插入新记录
            val toInsert = distinct.filter { it.filePath !in existing }
            newCount = toInsert.size
            if (toInsert.isNotEmpty()) {
                Log.d("CineWallScan", "insert new: ${toInsert.size} items")
                db.mediaDao().insert(toInsert)
            }
        }

        GlobalSnackBar.show("扫描完成：新增 $newCount 部，移除 $removedCount 部")

        val movies = distinct.count { it.mediaType == MediaType.MOVIE }
        val shows = distinct.count { it.mediaType == MediaType.TV_SHOW }
        _scanning = false
        return ScanResult(totalFound = distinct.size, movies = movies, shows = shows)
    }

    override suspend fun scrapeItem(item: MediaItem): MediaItem {
        Log.d("CineWallScrape", "scrapeItem start: id=${item.id} fileName=${item.fileName} title=${item.title} source=${item.source} scraped=${item.scraped}")
        GlobalSnackBar.show("开始刮削 「${item.displayTitle}」")
        val scraper = currentScraper()
        return withContext(Dispatchers.IO) {
            val scraped = scraper.scrape(item)
            Log.d("CineWallScrape", "scrapeItem result: id=${scraped.id} scraped=${scraped.scraped} source=${scraped.source} title=${scraped.title}")
            if (scraped.scraped)
                GlobalSnackBar.show("「${item.displayTitle}」 刮削完成")
            else
                GlobalSnackBar.show("未找到匹配数据")
            db.mediaDao().insert(listOf(scraped))
            scraped
        }
    }

    override suspend fun scrapeAllUnscraped(): Int {
        Log.d("CineWallScrape", "scrapeAllUnscraped start")
        val scraper = currentScraper()

        if (!scraper.isAvailable) return 0
        _scraping = true
        val result = withContext(Dispatchers.IO) {
            val unscraped = db.mediaDao().getUnscraped().first()
            Log.d("CineWallScrape", "scrapeAllUnscraped: found ${unscraped.size} unscraped items")
            unscraped.forEach { it ->
                Log.d("CineWallScrape", "  unscraped: id=${it.id} fileName=${it.fileName} title=${it.title}")
            }
            GlobalSnackBar.show("开始刮削 ...")
            Log.i("APP", "start scrape all unscraped")
            val scraped = scraper.scrapeAll(unscraped)
            Log.d("CineWallScrape", "scrapeAllUnscraped: scraped ${scraped.size} items")
            scraped.forEach { it ->
                Log.d("CineWallScrape", "  scraped result: id=${it.id} scraped=${it.scraped} source=${it.source} title=${it.title}")
            }
            db.mediaDao().insert(scraped)
            GlobalSnackBar.show("刮削完成：${scraped.size} 部")
            scraped.size
        }
        _scraping = false
        return result
    }

    override suspend fun scrapeAll(force: Boolean): Int {
        Log.d("CineWallScrape", "scrapeAll start: force=$force")
        val scraper = currentScraper()
        if (!scraper.isAvailable) return 0
        return withContext(Dispatchers.IO) {
            val items = if (force) {
                db.mediaDao().getAll().first()
            } else {
                db.mediaDao().getUnscraped().first()
            }
            Log.d("CineWallScrape", "scrapeAll: processing ${items.size} items (force=$force)")
            items.forEach { it ->
                Log.d("CineWallScrape", "  item: id=${it.id} fileName=${it.fileName} title=${it.title} scraped=${it.scraped} source=${it.source}")
            }
            val scraped = scraper.scrapeAll(items)
            Log.d("CineWallScrape", "scrapeAll: scraped ${scraped.size} items")
            scraped.forEach { it ->
                Log.d("CineWallScrape", "  scraped result: id=${it.id} scraped=${it.scraped} source=${it.source} title=${it.title}")
            }
            db.mediaDao().insert(scraped)
            scraped.size
        }
    }

    override suspend fun getTmdbSeasonCounts(tmdbId: Int): Map<Int, Int> {
        val tmdbKey = loadTmdbApiKey()
        if (tmdbKey.isNullOrBlank()) return emptyMap()
        return withContext(Dispatchers.IO) {
            TmdbScraper(tmdbKey, loadTmdbLanguage()).getSeasonEpisodeCounts(tmdbId)
        }
    }

    /** 尝试将 SAF Uri 转为实际文件（仅部分可访问路径） */
    private fun realFileFromUri(uri: Uri): java.io.File? {
        return try {
            val path = DocumentsPathUtils.getPath(context, uri)
            if (path.isNullOrBlank()) null else java.io.File(path)
        } catch (_: Exception) {
            null
        }
    }

    override fun getAutoScanAfterStarted(): Boolean =
        prefs.getBoolean(KEY_autoScanAfterStarted, false)
    override fun setAutoScanAfterStarted(value: Boolean) =
        prefs.edit().putBoolean(KEY_autoScanAfterStarted, value).apply()

    override fun getAutoScrollDelay(): Int =
        prefs.getInt(KEY_POSTER_AUTO_SCROLL_DELAY, 5000) ?: 5000
    override fun setAutoScrollDelay(value: Int) =
        prefs.edit().putInt(KEY_POSTER_AUTO_SCROLL_DELAY, value).apply()
}
