package com.cinewall.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun EmptyLibraryState(
    hasFolders: Boolean,
    onAddFolder: () -> Unit,
    onScan: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    description: String? = null
) {
    Column(
        modifier = modifier.fillMaxWidth().widthIn(max = 760.dp).padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CineWallMark(size = 72.dp)
        Spacer(Modifier.height(28.dp))
        Text(
            text = title ?: if (hasFolders) "媒体库还是空的" else "把你的电影，留在墙上",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = description ?: if (hasFolders) {
                "目录已经添加，但还没有发现视频。检查权限或重新扫描一次。"
            } else {
                "在设置中添加本地文件夹或 U 盘目录，CineWall 会自动扫描视频并整理海报墙。"
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(30.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = if (hasFolders) onScan else onAddFolder,
                modifier = Modifier.tvTouchClick(if (hasFolders) onScan else onAddFolder)
            ) {
                Text(if (hasFolders) "重新扫描" else "添加媒体目录")
            }
            if (!hasFolders) {
                OutlinedButton(
                    onClick = onScan,
                    modifier = Modifier.tvTouchClick(onScan)
                ) { Text("扫描媒体库") }
            }
        }
    }
}
