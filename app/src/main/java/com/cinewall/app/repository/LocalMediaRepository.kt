package com.cinewall.app.repository

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.cinewall.app.BuildConfig
import com.cinewall.app.db.AppDatabase
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val PREF_NAME = "cinewall_prefs"
const val KEY_SCAN_FOLDERS = "scan_folders"
const val KEY_TMDB_API_KEY = "tmdb_api_key"
const val KEY_TMDB_LANGUAGE = "tmdb_language"
const val KEY_THEME_MODE = "theme_mode" // 0 system,1 light,2 dark
const val KEY_THEME_PALETTE = "theme_palette"
const val KEY_PLAYBACK_SETTINGS = "playback_settings"
const val KEY_PLAYBACK_POSITION = "playback_position"

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
        prefs.edit().putInt(KEY_THEME_MODE, mode).apply()
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
        showCurrentTime = prefs.getBoolean("${KEY_PLAYBACK_SETTINGS}_show_time", false),
        showProgressInfo = prefs.getBoolean("${KEY_PLAYBACK_SETTINGS}_show_progress", false)
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

    override suspend fun scanFolders(): ScanResult {
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

        withContext(Dispatchers.IO) {
            db.mediaDao().replaceAll(distinct)
        }

        val movies = distinct.count { it.mediaType == MediaType.MOVIE }
        val shows = distinct.count { it.mediaType == MediaType.TV_SHOW }
        return ScanResult(totalFound = distinct.size, movies = movies, shows = shows)
    }

    override suspend fun scrapeItem(item: MediaItem): MediaItem {
        val scraper = currentScraper()
        return withContext(Dispatchers.IO) {
            val scraped = scraper.scrape(item)
            db.mediaDao().insert(listOf(scraped))
            scraped
        }
    }

    override suspend fun scrapeAllUnscraped(): Int {
        val scraper = currentScraper()
        if (!scraper.isAvailable) return 0
        return withContext(Dispatchers.IO) {
            val unscraped = db.mediaDao().getUnscraped().first()
            val scraped = scraper.scrapeAll(unscraped)
            db.mediaDao().insert(scraped)
            scraped.size
        }
    }

    override suspend fun scrapeAll(force: Boolean): Int {
        val scraper = currentScraper()
        if (!scraper.isAvailable) return 0
        return withContext(Dispatchers.IO) {
            val items = if (force) {
                db.mediaDao().getAll().first()
            } else {
                db.mediaDao().getUnscraped().first()
            }
            val scraped = scraper.scrapeAll(items)
            db.mediaDao().insert(scraped)
            scraped.size
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
}
