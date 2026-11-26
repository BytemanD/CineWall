package com.cinewall.app.ui.components

import android.graphics.Color
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.cinewall.app.ui.theme.COLOR_INFO
import com.cinewall.app.ui.theme.COLOR_SUCCESS
import com.cinewall.app.ui.theme.COLOR_WARNING

enum class AlertType {
    ERROR,
    WARNING,
    INFO,
    SUCCESS,
}

@Composable
fun Alert(
    message: String,
    type: AlertType = AlertType.INFO,
    onClick: (() -> Unit)? = null,
) {
    val icon = when (type) {
        AlertType.ERROR -> Icons.Filled.Dangerous
        AlertType.WARNING -> Icons.Filled.Warning
        AlertType.SUCCESS -> Icons.Filled.CheckCircleOutline
        else -> Icons.Filled.Info
    }
    val color = when (type) {
        AlertType.ERROR -> MaterialTheme.colorScheme.error
        AlertType.WARNING -> COLOR_WARNING
        AlertType.SUCCESS -> COLOR_SUCCESS
        else -> COLOR_INFO
    }
    @Composable
    fun content() {
        Icon(imageVector=icon, contentDescription=icon.name, tint=color);
        Spacer(Modifier.width(8.dp))
        Text(message)
    }


    if (onClick != null)
        Card(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().tvTouchClick(onClick),
            scale = CardDefaults.scale(focusedScale = 1.02f),
        ) {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).padding(8.dp)) {
                content()
            }
        }

    else
        Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(
            MaterialTheme.colorScheme.surfaceVariant
        ).padding(8.dp)
    ) { content() }
}