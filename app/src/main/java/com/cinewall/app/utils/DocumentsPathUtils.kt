package com.cinewall.app.utils

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.webkit.MimeTypeMap

/** SAF Uri 路径辅助：尽力将文档树 Uri 转为文件路径（Android 10+ 通常返回 null，此时走 MediaStore 兜底） */
object DocumentsPathUtils {
    fun getPath(context: Context, uri: Uri): String? {
        return when {
            DocumentsContract.isDocumentUri(context, uri) -> getDocumentPath(context, uri)
            uri.scheme == "content" -> getContentPath(context, uri)
            uri.scheme == "file" -> uri.path
            else -> null
        }
    }

    private fun getDocumentPath(context: Context, uri: Uri): String? {
        val docId = DocumentsContract.getDocumentId(uri)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val authority = uri.authority ?: return null
            return when {
                "com.android.externalstorage.documents" == authority -> {
                    val split = docId.split(":")
                    if (split.size >= 2 && "primary".equals(split[0], ignoreCase = true)) {
                        val path = split[1]
                        java.io.File(
                            context.getExternalFilesDir(null)?.parentFile?.parentFile,
                            path
                        )?.absolutePath
                    } else null
                }
                "com.android.providers.downloads.documents" == authority -> {
                    val id = docId.toLongOrNull() ?: return null
                    val contentUri = android.content.ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), id
                    )
                    getDataColumn(context, contentUri, null, null)
                }
                "com.android.providers.media.documents" == authority -> {
                    val split = docId.split(":")
                    if (split.size < 2) return null
                    val type = split[0]
                    val id = split[1]
                    val collection = when {
                        "image" == type -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        "video" == type -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                        "audio" == type -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                        else -> null
                    } ?: return null
                    getDataColumn(context, collection, "_id=?", arrayOf(id))
                }
                else -> null
            }
        }
        return null
    }

    private fun getContentPath(context: Context, uri: Uri): String? {
        val projection = arrayOf(MediaStore.MediaColumns.DATA)
        var cursor: Cursor? = null
        return try {
            cursor = context.contentResolver.query(uri, projection, null, null, null)
            val columnIndex = cursor?.getColumnIndex(MediaStore.MediaColumns.DATA) ?: -1
            if (cursor != null && columnIndex >= 0 && cursor.moveToFirst()) {
                cursor.getString(columnIndex)
            } else null
        } finally {
            cursor?.close()
        }
    }

    private fun getDataColumn(
        context: Context,
        uri: Uri,
        selection: String?,
        selectionArgs: Array<String>?
    ): String? {
        val projection = arrayOf(MediaStore.MediaColumns.DATA)
        var cursor: Cursor? = null
        return try {
            cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, null)
            val columnIndex = cursor?.getColumnIndex(MediaStore.MediaColumns.DATA) ?: -1
            if (cursor != null && columnIndex >= 0 && cursor.moveToFirst()) {
                cursor.getString(columnIndex)
            } else null
        } finally {
            cursor?.close()
        }
    }

    fun isVideoMimeType(context: Context, uri: Uri): Boolean {
        val type = context.contentResolver.getType(uri) ?: return false
        return type.startsWith("video/")
    }

    fun getExtension(fileName: String): String =
        MimeTypeMap.getFileExtensionFromUrl(fileName)
}
