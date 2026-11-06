package com.app.whispr.ui.uiState

import com.app.whispr.data.remote.dto.Message
import com.app.whispr.data.remote.websocket.ChatConnectionState

data class WebSocketUiState(
    val messages: List<Message> = emptyList(),
    val connectionState: ChatConnectionState =
        ChatConnectionState.DISCONNECTED,
    val onlineMembers: Set<String> = emptySet(),
    val typingUsers: Set<String> = emptySet(),
    val isLoadingHistory: Boolean = false,
    val hasMoreHistory: Boolean = true,
    val error: String? = null
)