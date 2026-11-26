package com.cinewall.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SpacedRow(modifier: Modifier = Modifier, space: Int = 8,
              verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
              content: @Composable RowScope.() -> Unit,) {
    Row (
        modifier=modifier,
        horizontalArrangement = Arrangement.spacedBy(space.dp),
        verticalAlignment = verticalAlignment,
        content=content,
    )
}