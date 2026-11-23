package com.cinewall.app.scanner

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract

/** 文件夹选择助手：基于 SAF (Storage Access Framework) 选择本地/U盘目录 */
object FolderPicker {
    const val REQUEST_PICK_FOLDER = 1001

    /** 启动目录选择器（ActivityResultContracts.OpenDocumentTree 替代方案） */
    fun pickFolder(activity: Activity) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
        }
        activity.startActivityForResult(intent, REQUEST_PICK_FOLDER)
    }

    /**
     * 将目录 Uri 持久化，以便应用重启后仍可访问（U盘/本地磁盘）。
     *
     * 注意：takePersistableUriPermission 只接受 READ/WRITE 标志（0x1 | 0x2）。
     * FLAG_GRANT_PERSISTABLE_URI_PERMISSION 是签发 Intent 时使用的（0x40），
     * 传给这里会抛 SecurityException: request flags 0x43, but only 0x3 are allowed。
     */
    fun persistUriPermission(context: Context, uri: Uri) {
        val resolver: ContentResolver = context.contentResolver
        val read = Intent.FLAG_GRANT_READ_URI_PERMISSION
        val write = Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                resolver.takePersistableUriPermission(uri, read or write)
            } catch (e: SecurityException) {
                // 部分提供程序（某些 U 盘/云盘）不支持持久化 WRITE，降级为只读
                try {
                    resolver.takePersistableUriPermission(uri, read)
                } catch (ignored: SecurityException) {
                    // 该 Uri 无法持久化；本次会话仍可读取，扫描不受影响
                }
            }
        }
    }

    /** 从返回的 data 中提取目录 Uri */
    fun folderUriFromResult(data: Intent?): Uri? = data?.data
}
