package com.cinewall.app.scanner

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import com.cinewall.app.model.MediaItem
import com.cinewall.app.model.MediaType
import java.io.File
import java.util.*

/** 本地视频扫描器：递归遍历目录，识别电影/剧集，构建 MediaItem */
class MediaScanner(private val context: Context) {

    private val matcher = TvShowMatcher

    /** 扫描一个目录（递归），返回该目录下的全部视频 MediaItem */
    fun scanDirectory(rootDir: File): List<MediaItem> {
        val items = mutableListOf<MediaItem>()
        scanRecursive(rootDir, items)
        return items
    }

    /** 扫描多个目录 */
    fun scanDirectories(dirs: List<File>): List<MediaItem> {
        val items = mutableListOf<MediaItem>()
        for (dir in dirs) {
            if (dir.exists() && dir.isDirectory) {
                scanRecursive(dir, items)
            }
        }
        return items
    }

    private fun scanRecursive(dir: File, items: MutableList<MediaItem>) {
        val files = dir.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory) {
                scanRecursive(file, items)
            } else if (isVideoFile(file)) {
                items.add(buildItem(file))
            }
        }
    }

    private fun isVideoFile(file: File): Boolean {
        val name = file.name.lowercase()
        return MediaItem.VIDEO_EXTENSIONS.any { name.endsWith(".${it.lowercase()}", ignoreCase = true) }
    }

    private fun buildItem(file: File): MediaItem {
        val name = file.name
        val baseName = name.substringBeforeLast('.')
        val matchResult = matcher.match(name)

        val mediaType = if (matchResult.isTvShow) MediaType.TV_SHOW else MediaType.MOVIE

        return MediaItem(
            filePath = file.absolutePath,
            fileName = baseName,
            mediaType = mediaType,
            title = baseName,
            subtitle = if (mediaType == MediaType.TV_SHOW) "剧集" else "电影",
            seasonNumber = matchResult.season,
            episodeNumber = matchResult.episode,
            durationMs = 0,
            fileSize = file.length(),
            source = "local",
            scraped = false
        )
    }

    /**
     * 通过 SAF 授权的目录树 Uri 递归扫描视频。
     * Android 11+ 无法把 tree Uri 稳定转换成真实文件路径，DocumentFile 是官方推荐做法，
     * 且持久化授权后可直接读取子文档，无需 MANAGE_EXTERNAL_STORAGE。
     */
    fun scanTreeUri(treeUri: Uri): List<MediaItem> {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return emptyList()
        val items = mutableListOf<MediaItem>()
        scanDocumentTree(root, items)
        return items
    }

    private fun scanDocumentTree(dir: DocumentFile, items: MutableList<MediaItem>) {
        for (child in dir.listFiles()) {
            if (child.isDirectory) {
                scanDocumentTree(child, items)
            } else if (child.isFile && isVideoName(child.name.orEmpty())) {
                items.add(buildItemFromDocument(child))
            }
        }
    }

    private fun isVideoName(name: String): Boolean {
        val lower = name.lowercase()
        return MediaItem.VIDEO_EXTENSIONS.any { lower.endsWith(".$it") }
    }

    private fun buildItemFromDocument(file: DocumentFile): MediaItem {
        val name = file.name ?: file.uri.lastPathSegment.orEmpty()
        val baseName = name.substringBeforeLast('.')
        val matchResult = matcher.match(name)
        val mediaType = if (matchResult.isTvShow) MediaType.TV_SHOW else MediaType.MOVIE
        return MediaItem(
            filePath = file.uri.toString(),
            fileName = baseName,
            mediaType = mediaType,
            title = baseName,
            subtitle = if (mediaType == MediaType.TV_SHOW) "剧集" else "电影",
            seasonNumber = matchResult.season,
            episodeNumber = matchResult.episode,
            durationMs = 0,
            fileSize = file.length(),
            source = "local",
            scraped = false
        )
    }

    /** 通过 MediaStore 查询已索引的视频（补充方案，适用于已获取权限的共享存储） */
    fun scanViaMediaStore(folderUri: Uri): List<MediaItem> {
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE
        )
        val items = mutableListOf<MediaItem>()
        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(folderUri, projection, null, null, null)
            cursor?.use { cur ->
                while (cur.moveToNext()) {
                    val path = cur.getString(cur.getColumnIndexOrThrow(MediaStore.Video.Media.DATA))
                    val displayName = cur.getString(cur.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME))
                    val duration = cur.getLong(cur.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION))
                    val size = cur.getLong(cur.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE))
                    val file = java.io.File(path)
                    if (!file.exists()) continue
                    val item = buildItem(file).copy(
                        durationMs = duration,
                        fileSize = size
                    )
                    items.add(item)
                }
            }
        } catch (e: Exception) {
            // 忽略单目录查询失败
        } finally {
            cursor?.close()
        }
        return items
    }
}
