package com.cinewall.app.ui.components

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.cinewall.app.scanner.LocalFolderBrowser
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 应用内自绘的目录选择器：先选磁盘，再逐层进入子文件夹。
 *
 * 不调用 ACTION_OPEN_DOCUMENT_TREE，因为很多 Android TV 没有系统文件管理器。
 */
@Composable
fun FolderPickerDialog(
    allFilesGranted: Boolean,
    onDismiss: () -> Unit,
    onRequestAccess: () -> Unit,
    onPick: (folderPath: String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var currentPath by remember { mutableStateOf<String?>(null) }
    var rootPath by remember { mutableStateOf<String?>(null) }
    var volumes by remember { mutableStateOf(emptyList<com.cinewall.app.scanner.StorageVolumeInfo>()) }
    var entries by remember { mutableStateOf(emptyList<com.cinewall.app.scanner.DirectoryEntry>()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var refreshKey by remember { mutableStateOf(0) }

    fun requestAccess() {
        onRequestAccess()
        refreshKey++
    }

    fun goBack() {
        val path = currentPath
        if (path == null) {
            onDismiss()
            return
        }
        if (path == rootPath) {
            currentPath = null
            rootPath = null
            entries = emptyList()
            error = null
            return
        }
        val parent = File(path).parentFile?.absolutePath
        if (parent == null || parent == path) {
            currentPath = null
            rootPath = null
        } else {
            currentPath = parent
        }
    }

    BackHandler { goBack() }

    LaunchedEffect(currentPath, allFilesGranted, refreshKey) {
        loading = true
        error = null
        val path = currentPath
        if (path == null) {
            volumes = withContext(Dispatchers.IO) { LocalFolderBrowser.volumes(context) }
        } else {
            runCatching {
                withContext(Dispatchers.IO) { LocalFolderBrowser.listDirectories(path) }
            }.onSuccess { loaded ->
                entries = loaded
            }.onFailure { throwable ->
                entries = emptyList()
                error = when (throwable) {
                    is SecurityException, is IllegalAccessException ->
                        "无法读取该目录，可能没有存储访问权限"
                    else -> "无法读取该目录：${throwable.message ?: "未知错误"}"
                }
            }
        }
        loading = false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.68f).height(620.dp),
            shape = RoundedCornerShape(22.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    "选择媒体目录",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    when {
                        currentPath == null -> "先选择一块磁盘，再逐层进入包含视频的文件夹。"
                        currentPath == rootPath -> "当前位于磁盘根目录，可直接选择整块磁盘，也可以继续进入子文件夹。"
                        else -> "进入包含视频的目录后，点击“使用此文件夹”。"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!allFilesGranted) {
                    Spacer(Modifier.height(12.dp))
                    Surface(shape = RoundedCornerShape(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "未开启“所有文件访问”，外接磁盘可能无法浏览。",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(12.dp))
                            SecondaryTvButton(
                                label = "去授权",
                                onClick = { requestAccess() },
                                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentPath != null) {
                        Surface(
                            onClick = { goBack() },
                            shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(10.dp)),
                            modifier = Modifier.tvTouchClick { goBack() }
                        ) {
                            Text(
                                "← 上一级",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                    }
                    Text(
                        text = currentPath ?: "存储设备",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(8.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        when {
                            loading -> Text(
                                "正在读取目录…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            error != null -> {
                                Text(
                                    error!!,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    SecondaryTvButton(
                                        label = "重新加载",
                                        onClick = { refreshKey++ },
                                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
                                    )
                                    SecondaryTvButton(
                                        label = "授予所有文件访问权限",
                                        onClick = { requestAccess() },
                                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
                                    )
                                }
                            }

                            currentPath == null -> {
                                if (volumes.isEmpty()) {
                                    Text(
                                        "没有检测到可用的存储设备",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    volumes.forEach { volume ->
                                        val open = { currentPath = volume.path; rootPath = volume.path }
                                        Surface(
                                            onClick = open,
                                            shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(12.dp)),
                                            modifier = Modifier.fillMaxWidth().tvTouchClick(open)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth()
                                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        volume.label,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        "${volume.path}${if (volume.removable) " · U盘" else " · 本地磁盘"}",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                Spacer(Modifier.width(12.dp))
                                                Text(
                                                    if (volume.freeBytes > 0) {
                                                        "${LocalFolderBrowser.formatSize(volume.freeBytes)} 可用"
                                                    } else {
                                                        ""
                                                    },
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            else -> {
                                if (entries.isEmpty()) {
                                    Text(
                                        "该目录没有子文件夹，可直接选择当前目录。",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    entries.forEach { entry ->
                                        val enter = { currentPath = entry.path }
                                        Surface(
                                            onClick = enter,
                                            shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(12.dp)),
                                            modifier = Modifier.fillMaxWidth().tvTouchClick(enter)
                                        ) {
                                            Text(
                                                entry.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                modifier = Modifier.fillMaxWidth()
                                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = currentPath?.let { "当前选择：$it" } ?: "未选择文件夹",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(16.dp))
                    SecondaryTvButton(
                        label = "取消",
                        onClick = onDismiss,
                        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 10.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    PrimaryTvButton(
                        label = if (currentPath == rootPath && currentPath != null) "使用整块磁盘" else "使用此文件夹",
                        onClick = {
                            currentPath?.let { path -> onPick(Uri.fromFile(File(path)).toString()) }
                        },
                        enabled = currentPath != null,
                        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}
