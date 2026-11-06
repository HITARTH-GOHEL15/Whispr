package com.app.whispr.data.remote.websocket

import com.app.whispr.data.remote.dto.Message
import com.google.gson.JsonObject


sealed class ChatEvent {
    data class Connected(val payload: JsonObject) : ChatEvent()
    data class Joined(val payload: JsonObject) : ChatEvent()
    data class MessageReceived(val message: Message) : ChatEvent()
    data class MessageAck(val payload: JsonObject) : ChatEvent()
    data class Presence(val payload: JsonObject) : ChatEvent()
    data class Typing(val payload: JsonObject) : ChatEvent()
    data class History(val payload: JsonObject) : ChatEvent()
    data class Left(val payload: JsonObject) : ChatEvent()
    data class ServerError(val payload: JsonObject) : ChatEvent()
    data class Unknown(val type: String, val payload: JsonObject) : ChatEvent()
    data class ParseError(val error: String) : ChatEvent()
}

enum class ChatConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    JOINED,
    RECONNECTING,
    LEAVING
}
