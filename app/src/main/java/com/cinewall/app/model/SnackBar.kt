package com.cinewall.app.model;

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

data class SnackBarRequest(
    val message: String,
    val actionLabel: String? = null,
    val onAction: () -> Unit = {},
)

object GlobalSnackBar {
    // Channel 缓冲消息，ViewModel/仓库/UI 都能安全调用
    private val channel = Channel<SnackBarRequest>(Channel.BUFFERED)
    val requests = channel.receiveAsFlow()

    fun show(message: String, actionLabel: String? = null, onAction: () -> Unit = {}) {
        channel.trySend(SnackBarRequest(message, actionLabel, onAction))
    }
}