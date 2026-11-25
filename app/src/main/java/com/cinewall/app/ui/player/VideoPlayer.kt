package com.cinewall.app.ui.player

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LifecycleStartEffect
import com.google.android.exoplayer2.ui.PlayerView

/** 只负责把 ExoPlayer 的PlayerView挂到 Compose 树上，控制逻辑全部在 PlayerController。 */
@Composable
fun VideoPlayer(
    controller: PlayerController,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            PlayerView(context).apply {
                controller.bind(this)
            }
        },
        modifier = modifier.fillMaxSize()
    )
}
