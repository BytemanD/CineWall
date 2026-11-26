package com.cinewall.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import com.cinewall.app.ui.components.CineWallLogo

@Preview(showBackground = true)
@Composable
fun LoadingPage() {
    CompositionLocalProvider(LocalInspectionMode provides false) {
        Box(
            modifier = Modifier.fillMaxSize().background(
                MaterialTheme.colorScheme.surfaceVariant
            ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CineWallLogo()
                LinearProgressIndicator(modifier = Modifier.height(4.dp),
                    color = MaterialTheme.colorScheme.primary)
            }
        }
    }

}