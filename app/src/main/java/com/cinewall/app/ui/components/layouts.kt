package com.cinewall.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

@Composable
fun SpacedRow(space: Int = 8,  content: @Composable RowScope.() -> Unit,) {
    Row (
        horizontalArrangement = Arrangement.spacedBy(space.dp),
        content=content,
    )
}