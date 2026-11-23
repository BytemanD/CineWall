package com.cinewall.app.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/**
 * 为 androidx.tv.material3 组件补充触摸点击支持。
 *
 * tv-material 的 tvClickable 只实现了 handleDPadEnter（遥控器确认键），
 * 不处理手指触摸，所以在平板/手机上点按钮没有反应。
 * 这里用 pointerInput 单独监听轻触，不影响原有的 D-Pad 焦点导航。
 */
fun Modifier.tvTouchClick(onClick: () -> Unit): Modifier = this.then(
    Modifier.pointerInput(onClick) {
        detectTapGestures(onTap = { onClick() })
    }
)
