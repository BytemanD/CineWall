package com.cinewall.app.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults


@Composable
fun TvCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    scale: Float = 1.04f,
    content: @Composable (ColumnScope.() -> Unit)
) {

    Card(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier.tvTouchClick(onClick),
        scale = CardDefaults.scale(focusedScale = scale),
        content = content,
    )
}