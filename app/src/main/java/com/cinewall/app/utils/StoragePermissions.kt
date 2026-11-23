package com.cinewall.app.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat

/** 本地视频库所需的存储权限集中管理 */
object StoragePermissions {

    private const val TAG = "StoragePermissions"

    /** 打开“所有文件访问”设置页的结果，用于给用户明确提示 */
    enum class AllFilesAccessResult {
        ALREADY_GRANTED,
        OPENED_APP_PAGE,
        OPENED_ALL_FILES_LIST,
        OPENED_APP_DETAILS,
        UNSUPPORTED
    }

    /** Android 11+ 的“所有文件访问权限”，浏览任意目录/U盘需要它 */
    fun hasAllFilesAccess(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager()

    /**
     * 按 TV 上可用的页面依次尝试打开授权入口：
     * 1. 当前应用的“所有文件访问”开关页
     * 2. 系统“所有文件访问”应用列表
     * 3. 应用详情页（用户可从 权限 中手动开启）
     *
     * 参考 my-tv-player：不能直接在 TV 上 startActivity 第一个 intent，
     * 因为很多 TV 系统没有实现它，会直接抛 ActivityNotFoundException。
     */
    fun openAllFilesAccessSettings(context: Context): AllFilesAccessResult {
        if (hasAllFilesAccess(context)) return AllFilesAccessResult.ALREADY_GRANTED

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val candidates = listOf(
                AllFilesAccessResult.OPENED_APP_PAGE to Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                ),
                AllFilesAccessResult.OPENED_ALL_FILES_LIST to Intent(
                    Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                ),
                AllFilesAccessResult.OPENED_APP_DETAILS to appDetailsIntent(context)
            )

            for ((result, intent) in candidates) {
                if (startIfResolvable(context, intent)) return result
            }
        } else {
            if (startIfResolvable(context, appDetailsIntent(context))) {
                return AllFilesAccessResult.OPENED_APP_DETAILS
            }
        }

        Log.w(TAG, "No usable all-files-access settings page on this device")
        return AllFilesAccessResult.UNSUPPORTED
    }

    fun allFilesAccessMessage(result: AllFilesAccessResult): String = when (result) {
        AllFilesAccessResult.ALREADY_GRANTED -> "所有文件访问已开启"
        AllFilesAccessResult.OPENED_APP_PAGE -> "已打开“所有文件访问”页面，请开启后返回，状态会自动刷新"
        AllFilesAccessResult.OPENED_ALL_FILES_LIST -> "已打开“所有文件访问”列表，找到 CineWall 并开启后返回"
        AllFilesAccessResult.OPENED_APP_DETAILS -> "已打开应用详情，请进入 权限 → 所有文件访问 开启后返回"
        AllFilesAccessResult.UNSUPPORTED -> "此设备没有可用的授权页面，请在系统设置 → 应用 → CineWall → 权限中手动开启"
    }

    private fun appDetailsIntent(context: Context): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))

    private fun startIfResolvable(context: Context, intent: Intent): Boolean = try {
        if (intent.resolveActivity(context.packageManager) == null) {
            Log.i(TAG, "No activity for ${intent.action}, trying next")
            false
        } else {
            if (context !is Activity) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        }
    } catch (error: Exception) {
        Log.w(TAG, "Failed to start ${intent.action}: ${error.message}", error)
        false
    }

    /** Android 13+ 用 READ_MEDIA_VIDEO/IMAGES，低版本用 READ_EXTERNAL_STORAGE */
    fun mediaPermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

    fun hasMediaPermissions(context: Context): Boolean =
        mediaPermissions().all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

    fun isFullyGranted(context: Context): Boolean =
        hasMediaPermissions(context) && hasAllFilesAccess(context)
}
