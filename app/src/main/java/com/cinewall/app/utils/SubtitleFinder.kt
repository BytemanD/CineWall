package com.cinewall.app.utils

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import java.io.File

/**
 * 查找与视频同名字幕（不区分扩展名大小写）。
 * 同时支持普通文件路径和 SAF 授权树里的 content Uri。
 */
object SubtitleFinder {
    data class Found(val uri: Uri, val mimeType: String)

    private val EXTENSIONS = setOf("srt", "ass", "ssa", "vtt", "sub")

    fun find(context: Context, videoPath: String, videoBaseName: String): Found? {
        val base = videoBaseName.substringBeforeLast('.')
        val name = if (base.isNotBlank()) base else File(videoPath).nameWithoutExtension

        val found = if (videoPath.startsWith("content://")) {
            findInDocumentTree(context, Uri.parse(videoPath), name)
        } else {
            findInDirectory(File(videoPath).parentFile, name)
        }
        return found
    }

    private fun findInDirectory(dir: File?, base: String): Found? {
        val file = dir?.listFiles { candidate ->
            candidate.isFile && matches(candidate.name, base)
        }?.firstOrNull() ?: return null
        return Found(Uri.fromFile(file), mimeType(file.name))
    }

    private fun findInDocumentTree(context: Context, videoUri: Uri, base: String): Found? {
        val docId = runCatching { DocumentsContract.getDocumentId(videoUri) }.getOrNull() ?: return null
        val separator = docId.indexOf(':')
        val volume = if (separator > 0) docId.substring(0, separator) else "primary"
        val path = if (separator > 0) docId.substring(separator + 1) else docId
        val parent = path.substringBeforeLast('/', "")
        val treeUri = DocumentsContract.buildTreeDocumentUri(
            videoUri.authority ?: return null,
            if (parent.isBlank()) "$volume:" else "$volume:$parent"
        )
        val dir = DocumentFile.fromTreeUri(context, treeUri) ?: return null
        val subtitle = dir.listFiles().firstOrNull { file ->
            file.isFile && file.name?.let { matches(it, base) } == true
        } ?: return null
        return Found(subtitle.uri, mimeType(subtitle.name.orEmpty()))
    }

    private fun matches(fileName: String, base: String): Boolean {
        val dot = fileName.lastIndexOf('.')
        if (dot <= 0) return false
        val extension = fileName.substring(dot + 1).lowercase()
        return extension in EXTENSIONS && fileName.substring(0, dot).equals(base, ignoreCase = true)
    }

    private fun mimeType(fileName: String): String = when (fileName.substringAfterLast('.').lowercase()) {
        "vtt" -> "text/vtt"
        "ass", "ssa" -> "application/x-ass"
        "sub" -> "text/x-subrip"
        else -> "application/x-subrip"
    }
}
