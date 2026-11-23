package com.cinewall.app.scanner

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import java.io.File
import java.util.Locale

/** 应用内自绘目录浏览器用到的磁盘/目录信息 */
data class StorageVolumeInfo(
    val path: String,
    val label: String,
    val removable: Boolean,
    val totalBytes: Long,
    val freeBytes: Long
)

data class DirectoryEntry(
    val name: String,
    val path: String
)

/**
 * 不依赖系统文件管理器的本地目录浏览。
 *
 * 参考 my-tv-player：先列出已挂载磁盘，再逐层调用 File.listFiles() 浏览子目录。
 * 很多 Android TV 没有 ACTION_OPEN_DOCUMENT_TREE 的实现，必须自己实现。
 */
object LocalFolderBrowser {

    fun volumes(context: Context): List<StorageVolumeInfo> {
        val seen = linkedSetOf<String>()
        val result = mutableListOf<StorageVolumeInfo>()

        val manager = context.getSystemService(Context.STORAGE_SERVICE) as? StorageManager
        manager?.storageVolumes?.forEach { volume ->
            val state = volume.state
            if (state != Environment.MEDIA_MOUNTED && state != Environment.MEDIA_MOUNTED_READ_ONLY) {
                return@forEach
            }
            val directory = volumeDirectory(context, volume) ?: return@forEach
            if (!directory.exists()) return@forEach
            val label = runCatching { volume.getDescription(context)?.toString() }
                .getOrNull()
                ?.takeIf { it.isNotBlank() }
                ?: directory.name
            addVolume(result, seen, directory, label, volume.isRemovable)
        }

        // 部分 TV 固件的 StorageManager 不列出 U 盘，补扫常见挂载点。
        // 注意：不能用 canRead() 过滤；没有“所有文件访问”时外接盘 canRead() 可能为 false。
        listOf(File("/storage"), File("/mnt/media_rw")).forEach { root ->
            root.listFiles()?.forEach { dir ->
                if (!dir.isDirectory || !dir.exists()) return@forEach
                if (dir.name == "emulated" || dir.name == "self") return@forEach
                addVolume(result, seen, dir, dir.name, removable = true)
            }
        }

        if (result.isEmpty()) {
            val fallback = Environment.getExternalStorageDirectory()
            if (fallback.exists()) {
                addVolume(result, seen, fallback, "内部存储", removable = false)
            }
        }

        result.sortWith(
            compareByDescending<StorageVolumeInfo> { it.removable }
                .thenBy { it.label.lowercase(Locale.getDefault()) }
        )
        return result
    }

    fun listDirectories(path: String): List<DirectoryEntry> {
        val directory = File(path)
        if (!directory.isDirectory) {
            throw IllegalAccessException("$path 不是目录")
        }
        val children = directory.listFiles { file ->
            file.isDirectory && !file.name.startsWith(".") && !file.isHidden
        } ?: throw IllegalAccessException("无法读取 $path，可能没有存储访问权限")

        return children
            .sortedBy { it.name.lowercase(Locale.getDefault()) }
            .map { DirectoryEntry(name = it.name, path = it.absolutePath) }
    }

    fun formatSize(bytes: Long): String = when {
        bytes >= 1024L * 1024L * 1024L ->
            String.format(Locale.getDefault(), "%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        bytes >= 1024L * 1024L ->
            String.format(Locale.getDefault(), "%.0f MB", bytes / (1024.0 * 1024.0))
        else -> "$bytes B"
    }

    private fun addVolume(
        result: MutableList<StorageVolumeInfo>,
        seen: MutableSet<String>,
        directory: File,
        label: String,
        removable: Boolean
    ) {
        val path = directory.absolutePath
        if (!seen.add(path)) return
        val stats = runCatching { StatFs(path) }.getOrNull()
        result += StorageVolumeInfo(
            path = path,
            label = label,
            removable = removable,
            totalBytes = stats?.let { it.blockCountLong * it.blockSizeLong } ?: 0L,
            freeBytes = stats?.let { it.availableBlocksLong * it.blockSizeLong } ?: 0L
        )
    }

    @Suppress("DEPRECATION")
    private fun volumeDirectory(context: Context, volume: StorageVolume): File? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            volume.directory
        } else {
            val method = runCatching {
                StorageVolume::class.java.getMethod("getPathFile")
            }.getOrNull()
            val file = method?.invoke(volume) as? File
            file ?: runCatching {
                StorageVolume::class.java.getMethod("getPath").invoke(volume) as? String
            }.getOrNull()?.let(::File)
        }
    } catch (_: Exception) {
        null
    }
}
