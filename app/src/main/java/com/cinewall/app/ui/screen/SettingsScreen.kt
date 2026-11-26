package com.cinewall.app.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Add
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.IconButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Switch
import androidx.tv.material3.Text
import com.cinewall.app.BuildConfig
import com.cinewall.app.model.PlaybackSettings
import com.cinewall.app.model.TmdbLanguage
import com.cinewall.app.repository.MediaRepository
import com.cinewall.app.ui.components.Alert
import com.cinewall.app.ui.components.AlertType
import com.cinewall.app.ui.components.AppHeader
import com.cinewall.app.ui.components.ConfirmDialog
import com.cinewall.app.ui.components.DefaultBtn
import com.cinewall.app.ui.components.FolderPickerDialog
import com.cinewall.app.ui.components.SettingCardListRow
import com.cinewall.app.ui.components.MainTabs
import com.cinewall.app.ui.components.PrimaryTvButton
import com.cinewall.app.ui.components.SecondaryTvButton
import com.cinewall.app.ui.components.SettingRadioGroupRow
import com.cinewall.app.ui.components.SettingSwitchRow
import com.cinewall.app.ui.components.tvTouchClick
import com.cinewall.app.ui.theme.LocalThemePalette
import com.cinewall.app.ui.theme.ThemeMode
import com.cinewall.app.ui.theme.ThemePalette
import com.cinewall.app.utils.FriendlyPath
import com.cinewall.app.utils.StoragePermissions
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    repository: MediaRepository,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var folders by remember { mutableStateOf(repository.loadScanFolders()) }
    var themeMode by remember { mutableStateOf(repository.loadThemeMode()) }
    var palette by remember { mutableStateOf(ThemePalette.fromId(repository.loadThemePalette())) }
    var tmdbLanguage by remember { mutableStateOf(repository.loadTmdbLanguage()) }
    var playback by remember { mutableStateOf(repository.loadPlaybackSettings()) }
    var status by remember { mutableStateOf("") }
    var scanning by remember { mutableStateOf(false) }
    var mediaGranted by remember { mutableStateOf(StoragePermissions.hasMediaPermissions(context)) }
    var allFilesGranted by remember { mutableStateOf(StoragePermissions.hasAllFilesAccess(context)) }

    var showFolderPicker by remember { mutableStateOf(false) }
    var pendingRemoveFolder by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        mediaGranted = result.values.all { it }
        allFilesGranted = StoragePermissions.hasAllFilesAccess(context)
        status = if (mediaGranted) "媒体文件读取权限已授予" else "未授予媒体文件读取权限"
    }

    // 从系统设置页返回后自动刷新权限状态，不需要用户手动重进设置页
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val newMedia = StoragePermissions.hasMediaPermissions(context)
        val newAllFiles = StoragePermissions.hasAllFilesAccess(context)
        if (newMedia != mediaGranted || newAllFiles != allFilesGranted) {
            mediaGranted = newMedia
            allFilesGranted = newAllFiles
            if (newAllFiles) status = "所有文件访问已授予"
        }
    }

    fun updatePlayback(transform: (PlaybackSettings) -> PlaybackSettings) {
        val updated = transform(playback)
        playback = updated
        repository.savePlaybackSettings(updated)
        status = "播放设置已更新"
    }

    fun applyThemeMode(value: Int) {
        themeMode = value
        repository.saveThemeMode(value)
        val label = when (value) {
            ThemeMode.SYSTEM.value -> "跟随系统"
            ThemeMode.LIGHT.value -> "明亮"
            else -> "夜间"
        }
        status = "主题已设为：$label"
        (context as? android.app.Activity)?.recreate()
    }

    fun applyPalette(option: ThemePalette) {
        palette = option
        repository.saveThemePalette(option.id)
        status = "主题配色已设为：${option.label}"
        (context as? android.app.Activity)?.recreate()
    }

    Box(modifier = modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 56.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // —— 外观：主题模式 + 主题配色置于同一张卡片 ——
            SettingsCard(title = "主题", subtitle = "明暗 & 配色，立即生效") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SettingRadioGroupRow(
                        label = "模式",
                        subtitle = "明亮 / 夜间 / 跟随系统",
                        items = listOf(
                            "跟随系统" to ThemeMode.SYSTEM.value,
                            "明亮" to ThemeMode.LIGHT.value,
                            "夜间" to ThemeMode.DARK.value
                        ),
                        selected = themeMode,
                        onSelect = { applyThemeMode(it) }
                    )

                    SettingCardListRow(
                        label = "配色",
                        subtitle = "内置 TV 配色",
                        items = ThemePalette.entries,
                        selected = palette,
                        onSelect = { applyPalette(it) }
                    ) { option, selected, onClick ->
                        Card(
                            onClick = onClick,
                            modifier = Modifier
                                .weight(1f)
                                .tvTouchClick(onClick),
                            scale = CardDefaults.scale(focusedScale = 1.02f),
                        ) {
                            Row(Modifier.padding(16.dp)) {
                                Column {
                                    Text(
                                        option.label,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        option.hint,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(Modifier.weight(1f))
                                Box(
                                    Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(Color(option.primary)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selected) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = "当前使用",
                                            tint = Color.White
                                        )
                                    }
                                }
                            }
//                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
//                                Row(
//                                    verticalAlignment = Alignment.CenterVertically,
//                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
//                                ) {
//                                    Box(
//                                        Modifier
//                                            .size(16.dp)
//                                            .clip(CircleShape)
//                                            .background(Color(option.primary)),
//                                        contentAlignment = Alignment.Center
//                                    ) {
//                                        if (selected) {
//                                            Icon(
//                                                imageVector = Icons.Filled.Check,
//                                                contentDescription = "当前使用",
//                                                modifier = Modifier.size(12.dp),
//                                                tint = Color.White
//                                            )
//                                        }
//                                    }
//                                    Text(
//                                        option.label,
//                                        style = MaterialTheme.typography.titleMedium,
//                                        maxLines = 1,
//                                        overflow = TextOverflow.Ellipsis
//                                    )
//                                }
//                                Spacer(Modifier.height(4.dp))
//                                Text(
//                                    option.hint,
//                                    style = MaterialTheme.typography.labelSmall,
//                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
//                                    maxLines = 1,
//                                    overflow = TextOverflow.Ellipsis
//                                )
//                            }
                        }
//                        Button(
//                            onClick = onClick,
//                            modifier = Modifier.weight(1f).tvTouchClick(onClick),
//                            shape = ButtonDefaults.shape(shape = RoundedCornerShape(16.dp)),
//                            colors = ButtonDefaults.colors(
//                                containerColor = if (selected) {
//                                    MaterialTheme.colorScheme.primaryContainer
//                                } else {
//                                    MaterialTheme.colorScheme.surfaceVariant
//                                },
//                                contentColor = if (selected) {
//                                    MaterialTheme.colorScheme.onPrimaryContainer
//                                } else {
//                                    MaterialTheme.colorScheme.onSurface
//                                },
//                                focusedContainerColor = if (selected) {
//                                    MaterialTheme.colorScheme.primaryContainer
//                                } else {
//                                    MaterialTheme.colorScheme.secondaryContainer
//                                },
//                                focusedContentColor = if (selected) {
//                                    MaterialTheme.colorScheme.onPrimaryContainer
//                                } else {
//                                    MaterialTheme.colorScheme.onSurface
//                                },
//                                pressedContainerColor = MaterialTheme.colorScheme.primaryContainer,
//                                pressedContentColor = MaterialTheme.colorScheme.onPrimaryContainer
//                            ),
//                            border = if (selected) ButtonDefaults.border(
//                                border = Border(
//                                    BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
//                                    shape = RoundedCornerShape(16.dp)
//                                )
//                            ) else ButtonDefaults.border(),
//                            glow = ButtonDefaults.glow(
//                                focusedGlow = Glow(
//                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
//                                    12.dp
//                                )
//                            ),
//                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 14.dp)
//                        ) {
//
//                        }
                    }
                }
            }

            // —— 播放偏好 ——
            SettingsCard(title = "播放设置", subtitle = "字幕、连续播放与播放速度会立即用于下一次播放") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SettingSwitchRow(
                        label = "自动加载同名字幕",
                        subtitle = "播放视频时自动挂载同目录、同名字幕文件",
                        checked = playback.autoSubtitle,
                        onCheckedChange = { checked ->
                            updatePlayback { it.copy(autoSubtitle = checked) }
                        }
                    )
                    SettingSwitchRow(
                        label = "自动播放下一集",
                        subtitle = "剧集播完后自动切换到下一集",
                        checked = playback.autoNext,
                        onCheckedChange = { checked ->
                            updatePlayback { it.copy(autoNext = checked) }
                        }
                    )
                    SettingSwitchRow(
                        label = "记住播放速度",
                        subtitle = "把播放器里调整过的倍速保存下来",
                        checked = playback.rememberSpeed,
                        onCheckedChange = { checked ->
                            updatePlayback { it.copy(rememberSpeed = checked) }
                        }
                    )
                    SettingRadioGroupRow(
                        label = "默认播放速度",
                        subtitle = "当前 ${playback.speed}x",
                        items = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 3.0f).map { speed ->
                            (if (speed == 1.0f) "1x" else "${speed}x") to speed
                        },
                        selected = playback.speed,
                        onSelect = { speed -> updatePlayback { it.copy(speed = speed) } }
                    )
                }
            }

            // —— 媒体库：目录管理 + 扫描/刮削 ——
            SettingsCard(title = "媒体库", subtitle = "添加本地磁盘 / U 盘目录，扫描视频并刮削元数据") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (folders.isEmpty()) {
                        Text(
                            "还没有添加任何文件夹",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        folders.forEach { uri ->
                            val requestRemove: () -> Unit = { pendingRemoveFolder = uri }
                            val danger = MaterialTheme.colorScheme.error
                            Card(
                                requestRemove,
//                                onLongClick = { requestRemove() },
                                scale = CardDefaults.scale(focusedScale = 1.02f),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .tvTouchClick(requestRemove),
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = FriendlyPath.folderLabel(uri),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(vertical = 4.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Icon(
                                        painter = painterResource(android.R.drawable.ic_menu_delete),
                                        contentDescription = "移除文件夹",
                                        tint = danger
                                    )
                                }
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Spacer(Modifier.weight(1f))
                        DefaultBtn(onClick = { showFolderPicker = true },
                            label = "文件夹",
                            icon= Icons.Filled.Add
                        )
                        PrimaryTvButton(label = "清空", onClick = {
                            repository.saveScanFolders(emptyList())
                            folders = emptyList()
                            status = "已清空文件夹列表"
                        }, containerColor = MaterialTheme.colorScheme.error)
                    }
                }
            }

            // 权限状态：浏览本地磁盘/U盘必须授予

            fun requestAllFilesGranted() {
                val result = StoragePermissions.openAllFilesAccessSettings(context)
                status = StoragePermissions.allFilesAccessMessage(result)
            }

            SettingsCard(
                title = "存储权限",
                subtitle = "扫描本地磁盘和 U 盘中的视频需要以下权限"
            ) {
                Alert(
                    "${ if(mediaGranted) "已" else "未" }获取媒体文件读取权限",
                    type = if(mediaGranted) AlertType.SUCCESS else AlertType.ERROR,
                    onClick = if (!mediaGranted) {{
                        permissionLauncher.launch(StoragePermissions.mediaPermissions())
                    }} else { null },
                )
                Spacer(Modifier.height(14.dp))
                Alert(
                    "${ if(allFilesGranted) "已" else "未" }获取所有文件访问",
                    type = if(allFilesGranted) AlertType.SUCCESS else AlertType.ERROR,
                    onClick = if (!allFilesGranted) {{ requestAllFilesGranted() }} else { null },
                )
            }

            SettingsCard(
                title = "刮削",
                subtitle = "元数据来源：TMDb 优先，失败时自动改用豆瓣 · 语言：${TmdbLanguage.fromCode(tmdbLanguage).label}"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ConfigStatusRow("TMDb API Key", BuildConfig.TMDB_API_KEY.isNotBlank())
                    ConfigStatusRow("豆瓣 API Key", repository.loadDoubanApiKey() != null)
                    SettingRadioGroupRow(
                        label = "TMDb 元数据语言",
                        subtitle = "下一次刮削生效",
                        items = TmdbLanguage.entries.map { it.label to it.code },
                        selected = TmdbLanguage.fromCode(tmdbLanguage).code,
                        onSelect = { code ->
                            repository.saveTmdbLanguage(code)
                            tmdbLanguage = code
                        }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(Modifier.weight(1f))
                        TvButton(label = "重新刮削全部（覆盖现有元数据）", onClick = {
                            scope.launch {
                                status = "正在重新刮削…"
                                val count = repository.scrapeAll(force = true)
                                status = "重新刮削完成：$count 部"
                            }
                        })
                    }
                }
            }

            if (status.isNotBlank()) {
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = 32.dp)
                )
            }
        }
    }

    if (pendingRemoveFolder != null) {
        val folder = pendingRemoveFolder!!
        ConfirmDialog(
            title = "移除媒体目录？",
            message = "将从 CineWall 中移除「${FriendlyPath.folderLabel(folder)}」，不会删除硬盘上的文件。",
            confirmLabel = "移除",
            onConfirm = {
                repository.removeScanFolder(folder)
                folders = repository.loadScanFolders()
                status = "已移除：${FriendlyPath.folderLabel(folder)}"
                pendingRemoveFolder = null
            },
            onDismiss = { pendingRemoveFolder = null }
        )
    }

    if (showFolderPicker) {
        FolderPickerDialog(
            allFilesGranted = allFilesGranted,
            onDismiss = { showFolderPicker = false },
            onRequestAccess = {
                val result = StoragePermissions.openAllFilesAccessSettings(context)
                status = StoragePermissions.allFilesAccessMessage(result)
            },
            onPick = { path ->
                val current = repository.loadScanFolders().toMutableSet()
                if (current.add(path)) {
                    repository.saveScanFolders(current.toList())
                    folders = repository.loadScanFolders()
                }
                status = "已添加：${FriendlyPath.folderLabel(path)}"
                showFolderPicker = false
            }
        )
    }

}

@Composable
private fun SettingsCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    content: (@Composable () -> Unit)? = null
) {
    Surface(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(Modifier.height(2.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (content != null) {
                Spacer(Modifier.height(18.dp))
                Column(modifier = Modifier.padding(horizontal = 8.dp)) {
                    content()
                }
            }
        }
    }
}

/** 刮削配置项：左侧名称，右侧 icon + 可用状态。 */
@Composable
private fun ConfigStatusRow(label: String, available: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        ConfigStatus(available)
    }
}

/** 配置状态：右侧一行 icon + 文案，用 success/danger 区分可用与否。 */
@Composable
private fun ConfigStatus(available: Boolean) {
    val semantic = LocalThemePalette.current
    val color = if (available) Color(semantic.success) else Color(semantic.danger)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = if (available) Icons.Filled.Check else Icons.Filled.Close,
            contentDescription = if (available) "可用" else "不可用",
            modifier = Modifier.size(20.dp),
            tint = color
        )
        Text(
            text = if (available) "可用" else "不可用",
            style = MaterialTheme.typography.labelLarge,
            color = color
        )
    }
}

@Composable
private fun PermissionChip(label: String, granted: Boolean) {
    val semantic = LocalThemePalette.current
    Surface(
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Text(
            text = if (granted) "✓ $label" else "✗ $label",
            style = MaterialTheme.typography.labelLarge,
            color = if (granted) Color(semantic.success) else Color(semantic.danger),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun TvButton(
    label: String,
    onClick: () -> Unit,
    primary: Boolean = false,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
) {
    if (primary) {
        PrimaryTvButton(label = label, onClick = onClick, modifier = modifier, icon=icon)
    } else {
        SecondaryTvButton(label = label, onClick = onClick, modifier = modifier)
    }
}
