package com.cinewall.app.ui.components

import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.IconButton
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

@Composable
fun IconBtn(
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    IconButton(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = Modifier.tvTouchClick(onClick).indication(
            interactionSource = interactionSource,
            ripple(bounded = true, color = Color.White),
        ),
        glow = ButtonDefaults.glow(
            focusedGlow = Glow(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), 16.dp)
        ),
    ) {
        Icon(imageVector = icon, contentDescription=icon.name)
    }
}

@Composable
fun DefaultBtn(
    onClick: () -> Unit,
    label: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    if (icon != null && label == null) {
        return IconBtn(icon, onClick, enabled=enabled);
    }
    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = Modifier.tvTouchClick(onClick).indication(
            interactionSource = interactionSource,
            ripple(bounded = true, color = Color.White),
        ),
        glow = ButtonDefaults.glow(
            focusedGlow = Glow(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), 16.dp)
        ),
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription=icon.name)
            Spacer(Modifier.width(8.dp))
        }
        if (label != null) {
            Text(label, style = MaterialTheme.typography.titleMedium, maxLines = 1)
        }
    }
}