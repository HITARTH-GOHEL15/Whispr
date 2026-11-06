
package com.app.whispr.data.remote.websocket

import com.app.whispr.data.remote.dto.Message
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.jvm.java
import kotlin.math.min
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class ChatWebSocketClient @Inject constructor(
    @Named("websocket_base_url")
    private val baseUrl: String,

    private val okHttpClient: OkHttpClient,

    private val gson: Gson
){

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Any()

    private var webSocket: WebSocket? = null

    private var room: String = ""
    private var email: String = ""
    private var name: String = ""

    @Volatile
    private var shouldReconnect = false

    @Volatile
    private var isJoined = false

    private var reconnectAttempt = 0



    private var reconnectJob: Job? = null
    private var heartbeatJob: Job? = null

    private var lastMessageId = 0L

    // Retain unacknowledged messages for retry.
    private val pendingMessages = linkedMapOf<String, JsonObject>()

    private val _connectionState =
        MutableStateFlow(ChatConnectionState.DISCONNECTED)

    val connectionState = _connectionState.asStateFlow()

    private val _events = MutableSharedFlow<ChatEvent>(
        extraBufferCapacity = 64
    )

    val events = _events.asSharedFlow()

    // --------------------------------------------------
    // CONNECT
    // --------------------------------------------------

    fun connect(
        room: String,
        email: String,
        name: String
    ) {
        synchronized(lock) {
            if (shouldReconnect) return

            this.room = room
            this.email = email
            this.name = name

            shouldReconnect = true
            isJoined = false
            reconnectAttempt = 0
        }

        openSocket()
    }

    private fun openSocket() {
        synchronized(lock) {
            if (!shouldReconnect) return
        }

        _connectionState.value = ChatConnectionState.CONNECTING

        val url = "${baseUrl.trimEnd('/')}/$room/"

        val request = Request.Builder()
            .url(url)
            .build()

        val socket = okHttpClient.newWebSocket(
            request,
            createListener()
        )

        synchronized(lock) {
            if (shouldReconnect) {
                webSocket = socket
            } else {
                socket.cancel()
            }
        }
    }

    // --------------------------------------------------
    // WEBSOCKET LISTENER
    // --------------------------------------------------

    private fun createListener() = object : WebSocketListener() {

        override fun onOpen(
            webSocket: WebSocket,
            response: Response
        ) {
            synchronized(lock) {
                if (!shouldReconnect) {
                    webSocket.close(1000, "No longer needed")
                    return
                }

                this@ChatWebSocketClient.webSocket = webSocket
            }

            _connectionState.value = ChatConnectionState.CONNECTED

            // Join only after the socket opens.
            sendJoin()

            startHeartbeat()
        }

        override fun onMessage(
            webSocket: WebSocket,
            text: String
        ) {
            handleIncomingMessage(text)
        }

        override fun onClosing(
            webSocket: WebSocket,
            code: Int,
            reason: String
        ) {
            webSocket.close(code, reason)
        }

        override fun onClosed(
            webSocket: WebSocket,
            code: Int,
            reason: String
        ) {
            stopHeartbeat()

            synchronized(lock) {
                if (this@ChatWebSocketClient.webSocket === webSocket) {
                    this@ChatWebSocketClient.webSocket = null
                }

                if (code == 4400) {
                    shouldReconnect = false
                }
            }

            isJoined = false

            if (shouldReconnect) {
                scheduleReconnect()
            } else {
                _connectionState.value =
                    ChatConnectionState.DISCONNECTED
            }
        }

        override fun onFailure(
            webSocket: WebSocket,
            t: Throwable,
            response: Response?
        ) {
            stopHeartbeat()

            synchronized(lock) {
                if (this@ChatWebSocketClient.webSocket === webSocket) {
                    this@ChatWebSocketClient.webSocket = null
                }
            }

            isJoined = false

            _events.tryEmit(
                ChatEvent.ParseError(
                    "WebSocket failure: ${t.message}"
                )
            )

            if (shouldReconnect) {
                scheduleReconnect()
            } else {
                _connectionState.value =
                    ChatConnectionState.DISCONNECTED
            }
        }
    }

    // --------------------------------------------------
    // JOIN
    // --------------------------------------------------

    private fun sendJoin() {
        val json = JsonObject().apply {
            addProperty("type", "join")
            addProperty("email", email)
            addProperty("name", name)
            addProperty("last_message_id", lastMessageId)
        }

        sendJson(json)
    }

    // --------------------------------------------------
    // INCOMING EVENTS
    // --------------------------------------------------

    private fun handleIncomingMessage(text: String) {
        try {
            val json = JsonParser.parseString(text).asJsonObject
            val type = json.get("type")?.asString ?: return

            when (type) {

                "connected" -> {
                    _events.tryEmit(ChatEvent.Connected(json))
                }

                "joined" -> {
                    isJoined = true
                    reconnectAttempt = 0

                    _connectionState.value =
                        ChatConnectionState.JOINED

                    updateLastMessageId(json)

                    _events.tryEmit(ChatEvent.Joined(json))

                    // Retry unacknowledged messages.
                    resendPendingMessages()
                }

                "message" -> {
                    val messageJson = json.getAsJsonObject("message")

                    if (messageJson != null) {
                        val message = gson.fromJson(
                            messageJson,
                            Message::class.java
                        )

                        message.id?.let {
                            lastMessageId = maxOf(
                                lastMessageId,
                                it
                            )
                        }

                        _events.tryEmit(
                            ChatEvent.MessageReceived(message)
                        )
                    }
                }

                "message_ack" -> {
                    val clientId = json.get("client_id")
                        ?.takeUnless { it.isJsonNull }
                        ?.asString

                    if (clientId != null) {
                        synchronized(lock) {
                            pendingMessages.remove(clientId)
                        }
                    }

                    _events.tryEmit(ChatEvent.MessageAck(json))
                }

                "presence" -> {
                    _events.tryEmit(ChatEvent.Presence(json))
                }

                "typing" -> {
                    _events.tryEmit(ChatEvent.Typing(json))
                }

                "history" -> {
                    updateLastMessageId(json)
                    _events.tryEmit(ChatEvent.History(json))
                }

                "left" -> {
                        synchronized(lock) {
                            shouldReconnect = false
                            isJoined = false
                        }

                        stopHeartbeat()
                        reconnectJob?.cancel()
                        reconnectJob = null

                        _connectionState.value =
                            ChatConnectionState.DISCONNECTED

                        _events.tryEmit(ChatEvent.Left(json))
                }

                "error" -> {
                    _events.tryEmit(ChatEvent.ServerError(json))
                }

                "pong" -> {
                    // Heartbeat response received.
                }

                else -> {
                    _events.tryEmit(
                        ChatEvent.Unknown(type, json)
                    )
                }
            }

        } catch (e: Exception) {
            _events.tryEmit(
                ChatEvent.ParseError(
                    "Invalid server JSON: ${e.message}"
                )
            )
        }
    }

    private fun updateLastMessageId(json: JsonObject) {
        val history = json.getAsJsonArray("history")

        history?.forEach { element ->
            if (element.isJsonObject) {
                val id = element.asJsonObject
                    .get("id")
                    ?.takeUnless { it.isJsonNull }
                    ?.asLong

                if (id != null) {
                    lastMessageId = maxOf(lastMessageId, id)
                }
            }
        }

        val messages = json.getAsJsonArray("messages")

        messages?.forEach { element ->
            if (element.isJsonObject) {
                val id = element.asJsonObject
                    .get("id")
                    ?.takeUnless { it.isJsonNull }
                    ?.asLong

                if (id != null) {
                    lastMessageId = maxOf(lastMessageId, id)
                }
            }
        }
    }

    // --------------------------------------------------
    // SEND MESSAGE
    // --------------------------------------------------

    fun sendMessage(content: String): String? {
        val trimmedContent = content.trim()

        if (trimmedContent.isEmpty() || trimmedContent.length > 2000) {
            return null
        }

        val clientId = UUID.randomUUID().toString()

        val json = JsonObject().apply {
            addProperty("type", "message")
            addProperty("content", trimmedContent)
            addProperty("client_id", clientId)
        }

        synchronized(lock) {
            pendingMessages[clientId] = json
        }

        if (isJoined) {
            sendJson(json)
        }

        return clientId
    }

    private fun resendPendingMessages() {
        val messages = synchronized(lock) {
            pendingMessages.values.toList()
        }

        messages.forEach { sendJson(it) }
    }

    // --------------------------------------------------
    // TYPING
    // --------------------------------------------------

    fun sendTyping(isTyping: Boolean) {
        val json = JsonObject().apply {
            addProperty("type", "typing")
            addProperty("is_typing", isTyping)
        }

        if (isJoined) {
            sendJson(json)
        }
    }

    // --------------------------------------------------
    // HEARTBEAT
    // --------------------------------------------------

    private fun startHeartbeat() {
        heartbeatJob?.cancel()

        heartbeatJob = scope.launch {
            while (isActive && shouldReconnect) {
                delay(25_000.milliseconds)

                val json = JsonObject().apply {
                    addProperty("type", "ping")
                }

                sendJson(json)
            }
        }
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    // --------------------------------------------------
    // SEND JSON
    // --------------------------------------------------

    private fun sendJson(json: JsonObject): Boolean {
        val socket = synchronized(lock) {
            webSocket
        } ?: return false

        return socket.send(gson.toJson(json))
    }

    // --------------------------------------------------
    // RECONNECTION
    // --------------------------------------------------

    private fun scheduleReconnect() {
        synchronized(lock) {
            if (!shouldReconnect) return
            if (reconnectJob?.isActive == true) return

            _connectionState.value =
                ChatConnectionState.RECONNECTING

            val attempt = reconnectAttempt++

            reconnectJob = scope.launch {
                val baseDelay = min(
                    1000L * (1L shl min(attempt, 4)),
                    15_000L
                )

                val jitter = Random.nextLong(0, 500)

                delay((baseDelay + jitter).milliseconds)

                synchronized(lock) {
                    if (!shouldReconnect) return@launch
                }

                openSocket()
            }
        }
    }

    fun loadHistory(
        beforeId: Long,
        limit: Int = 50
    ) {
        if (!isJoined) return

        if (beforeId <= 0L || limit !in 1..100) return

        val json = JsonObject().apply {
            addProperty("type", "history")
            addProperty("before_id", beforeId)
            addProperty("limit", limit)
        }

        sendJson(json)
    }

    // --------------------------------------------------
    // LEAVE ROOM
    // --------------------------------------------------

    fun leaveRoom() {
        synchronized(lock) {
            shouldReconnect = false
        }

        _connectionState.value = ChatConnectionState.LEAVING

        stopHeartbeat()
        reconnectJob?.cancel()
        reconnectJob = null

        val leaveJson = JsonObject().apply {
            addProperty("type", "leave")
        }

        sendJson(leaveJson)

        // The leave frame is queued before the close frame.
        synchronized(lock) {
            webSocket?.close(1000, "Leaving room")
            webSocket = null
            isJoined = false
            pendingMessages.clear()
        }

        _connectionState.value =
            ChatConnectionState.DISCONNECTED
    }

    // --------------------------------------------------
    // DISCONNECT WITHOUT LEAVING
    // --------------------------------------------------

    fun disconnect() {
        synchronized(lock) {
            shouldReconnect = false
            isJoined = false
        }

        stopHeartbeat()
        reconnectJob?.cancel()
        reconnectJob = null

        synchronized(lock) {
            webSocket?.close(1000, "Client disconnected")
            webSocket = null
        }

        _connectionState.value =
            ChatConnectionState.DISCONNECTED
    }
}
