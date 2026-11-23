package com.cinewall.app.utils

import android.net.Uri
import android.provider.DocumentsContract
import java.net.URLDecoder

/**
 * 把 SAF 目录 Uri 转成人能读懂的短标签。
 *
 * 原始值形如：
 * content://com.android.externalstorage.documents/tree/primary%3ADownload%2F%E8%81%AA%E6%98%8E%E9%95%87...
 * 显示值形如：本地磁盘:/Download/聪明镇[全10集]
 */
object FriendlyPath {
    fun folderLabel(uriString: String): String {
        val uri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return uriString

        if (uri.scheme == "file") {
            return "本地磁盘:${uri.path ?: uriString}"
        }

        // getTreeDocumentId 会做 percent 解码，中文不再显示为 %E8%81%AA
        val docId = runCatching { DocumentsContract.getTreeDocumentId(uri) }.getOrNull()
        if (!docId.isNullOrBlank()) {
            val separator = docId.indexOf(':')
            val volume = if (separator > 0) docId.substring(0, separator) else "primary"
            val path = if (separator > 0) docId.substring(separator + 1) else docId
            val root = when {
                volume.equals("primary", ignoreCase = true) -> "本地磁盘"
                volume.startsWith("raw", ignoreCase = true) -> "本地磁盘"
                else -> "U盘"
            }
            val cleanPath = path.trimStart('/')
            return if (cleanPath.isBlank()) root else "$root:/$cleanPath"
        }

        // 兜底：手动解码最后一段
        val decoded = runCatching {
            URLDecoder.decode(uri.lastPathSegment ?: uriString, "UTF-8")
        }.getOrDefault(uriString)
        val root = when (uri.authority) {
            "com.android.externalstorage.documents" -> "本地磁盘"
            "com.android.providers.downloads.documents" -> "下载目录"
            null -> "本地磁盘"
            else -> "外部目录"
        }
        return if (decoded.isBlank()) root else "$root:/$decoded"
    }
}
