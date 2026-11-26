package com.cinewall.app.repository

import android.content.Context
import android.net.Uri
import com.cinewall.app.model.MediaItem
import com.cinewall.app.model.MediaType
import com.cinewall.app.model.PlaybackSettings
import com.cinewall.app.model.ScanResult
import kotlinx.coroutines.flow.Flow

/** 媒体仓库接口 */
interface MediaRepository {
    val scanning: Boolean get() = false
    val scraping: Boolean get() = false

    /** 所有媒体项（按类型） */
    fun getMediaByType(type: MediaType): Flow<List<MediaItem>>
    /** 所有媒体 */
    fun getAllMedia(): Flow<List<MediaItem>>
    /** 已刮削的媒体 */
    fun getScrapedMedia(): Flow<List<MediaItem>>
    /** 媒体总数 */
    suspend fun count(): Int

    /** 按 ID 获取单个媒体项 */
    suspend fun getItemById(id: String): MediaItem?

    /** 保存选中的媒体文件夹 Uri 列表（字符串） */
    fun saveScanFolders(uris: List<String>)
    /** 读取已保存的媒体文件夹 Uri */
    fun loadScanFolders(): List<String>
    /** 移除单个媒体文件夹（同时释放已持久化的 URI 权限） */
    fun removeScanFolder(uri: String)

    /** 覆盖 TMDb API Key（可选；默认使用构建期注入的密钥） */
    fun saveTmdbApiKey(key: String?)
    /** 读取 TMDb API Key（优先手动覆盖值，否则取构建配置） */
    fun loadTmdbApiKey(): String?
    /** 读取豆瓣 API Key（TMDb 找不到时的备用源，来自构建配置） */
    fun loadDoubanApiKey(): String?
    /** TMDb 刮削使用的元数据语言（zh-CN / en-US 等） */
    fun saveTmdbLanguage(code: String)
    fun loadTmdbLanguage(): String
    /** 是否使用构建配置中的密钥（而非手动覆盖） */
    fun isUsingBuiltInApiKey(): Boolean

    /** 主题模式：0=跟随系统, 1=明亮, 2=夜间 */
    fun saveThemeMode(mode: Int)
    fun loadThemeMode(): Int
    /** 主题配色 id（对应 ThemePalette，默认 classic） */
    fun saveThemePalette(id: String)
    fun loadThemePalette(): String

    /** 播放偏好：字幕 / 自动下一集 / 播放速度 */
    fun loadPlaybackSettings(): PlaybackSettings
    fun savePlaybackSettings(settings: PlaybackSettings)

    /** 单个媒体的播放进度（毫秒），0 表示从头播放 */
    fun loadPlaybackPosition(mediaId: String): Long
    fun savePlaybackPosition(mediaId: String, positionMs: Long)

    /** 扫描所有已选文件夹并入库 */
    suspend fun scanFolders(): ScanResult
    /** 刮削指定项的元数据 */
    suspend fun scrapeItem(item: MediaItem): MediaItem
    /** 刮削所有未刮削项 */
    suspend fun scrapeAllUnscraped(): Int
    /** 刮削全部条目；force=true 时会覆盖已有刮削结果（用于修正确认刮错的数据） */
    suspend fun scrapeAll(force: Boolean = false): Int
}
