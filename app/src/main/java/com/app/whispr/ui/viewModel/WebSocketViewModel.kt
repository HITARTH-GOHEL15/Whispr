package com.app.whispr.ui.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.whispr.data.remote.dto.Message
import com.app.whispr.data.remote.websocket.ChatConnectionState
import com.app.whispr.data.remote.websocket.ChatEvent
import com.app.whispr.data.repository.WhisprRepository
import com.app.whispr.ui.uiState.WebSocketUiState
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WebSocketViewModel @Inject constructor(
    private val repository: WhisprRepository,
    private val gson: Gson
) : ViewModel() {

    private val _uiState = MutableStateFlow(WebSocketUiState())
    val uiState: StateFlow<WebSocketUiState> = _uiState.asStateFlow()

    private var currentRoom: String? = null

    init {
        observeConnectionState()
        observeSocketEvents()
    }

    // --------------------------------------------------
    // CONNECTION STATE
    // --------------------------------------------------

    private fun observeConnectionState() {
        viewModelScope.launch {
            repository.connectionState.collect { state ->
                _uiState.update {
                    it.copy(connectionState = state)
                }
            }
        }
    }

    // --------------------------------------------------
    // SOCKET EVENTS
    // --------------------------------------------------

    private fun observeSocketEvents() {
        viewModelScope.launch {
            repository.socketEvents.collect { event ->

                when (event) {

                    is ChatEvent.Connected -> {
                        // WebSocket connection established.
                    }

                    is ChatEvent.Joined -> {
                        handleJoined(event.payload)
                    }

                    is ChatEvent.MessageReceived -> {
                        addMessage(event.message)
                    }

                    is ChatEvent.MessageAck -> {
                        // ChatWebSocketClient already removes
                        // acknowledged messages from pendingMessages.
                    }

                    is ChatEvent.Presence -> {
                        handlePresence(event.payload)
                    }

                    is ChatEvent.Typing -> {
                        handleTyping(event.payload)
                    }

                    is ChatEvent.History -> {
                        handleHistory(event.payload)
                    }

                    is ChatEvent.Left -> {
                        _uiState.update {
                            it.copy(
                                connectionState =
                                    ChatConnectionState.DISCONNECTED,
                                onlineMembers = emptySet(),
                                typingUsers = emptySet(),
                                isLoadingHistory = false
                            )
                        }
                    }

                    is ChatEvent.ServerError -> {
                        handleServerError(event.payload)
                    }

                    is ChatEvent.ParseError -> {
                        _uiState.update {
                            it.copy(
                                error = event.error,
                                isLoadingHistory = false
                            )
                        }
                    }

                    is ChatEvent.Unknown -> {
                        // Ignore unsupported events.
                    }
                }
            }
        }
    }

    // --------------------------------------------------
    // JOINED EVENT
    // --------------------------------------------------

    private fun handleJoined(payload: JsonObject) {

        val history = parseMessages(
            payload.get("history")
        )

        val onlineMembers = parseOnlineMembers(
            payload.get("online_members")
        )

        val hasMore = payload.get("has_more")
            ?.takeUnless { it.isJsonNull }
            ?.asBoolean ?: false

        _uiState.update { state ->

            val mergedMessages = mergeMessages(
                existing = state.messages,
                incoming = history
            )

            state.copy(
                messages = mergedMessages,
                onlineMembers = onlineMembers,
                hasMoreHistory = hasMore,
                isLoadingHistory = false,
                error = null
            )
        }
    }

    // --------------------------------------------------
    // HISTORY EVENT
    // --------------------------------------------------

    private fun handleHistory(payload: JsonObject) {

        val messages = parseMessages(
            payload.get("messages")
        )

        val hasMore = payload.get("has_more")
            ?.takeUnless { it.isJsonNull }
            ?.asBoolean ?: false

        _uiState.update { state ->

            state.copy(
                messages = mergeMessages(
                    existing = state.messages,
                    incoming = messages
                ),
                hasMoreHistory = hasMore,
                isLoadingHistory = false
            )
        }
    }

    // --------------------------------------------------
    // PRESENCE EVENT
    // --------------------------------------------------

    private fun handlePresence(payload: JsonObject) {

        val event = payload.get("event")
            ?.takeUnless { it.isJsonNull }
            ?.asString ?: return

        val member = payload.getAsJsonObject("member")
            ?: return

        val email = member.get("email")
            ?.takeUnless { it.isJsonNull }
            ?.asString ?: return

        _uiState.update { state ->

            val updatedMembers = state.onlineMembers.toMutableSet()

            when (event) {
                "joined" -> updatedMembers.add(email)
                "left" -> updatedMembers.remove(email)
            }

            state.copy(
                onlineMembers = updatedMembers
            )
        }
    }

    // --------------------------------------------------
    // TYPING EVENT
    // --------------------------------------------------

    private fun handleTyping(payload: JsonObject) {

        val member = payload.getAsJsonObject("member")
            ?: return

        val email = member.get("email")
            ?.takeUnless { it.isJsonNull }
            ?.asString ?: return

        val isTyping = payload.get("is_typing")
            ?.takeUnless { it.isJsonNull }
            ?.asBoolean ?: false

        _uiState.update { state ->

            val updatedTypingUsers =
                state.typingUsers.toMutableSet()

            if (isTyping) {
                updatedTypingUsers.add(email)
            } else {
                updatedTypingUsers.remove(email)
            }

            state.copy(
                typingUsers = updatedTypingUsers
            )
        }
    }

    // --------------------------------------------------
    // SERVER ERROR
    // --------------------------------------------------

    private fun handleServerError(payload: JsonObject) {

        val message = payload.get("message")
            ?.takeUnless { it.isJsonNull }
            ?.asString
            ?: payload.get("detail")
                ?.takeUnless { it.isJsonNull }
                ?.asString
            ?: payload.get("code")
                ?.takeUnless { it.isJsonNull }
                ?.asString
            ?: "Something went wrong."

        _uiState.update {
            it.copy(
                error = message,
                isLoadingHistory = false
            )
        }
    }

    // --------------------------------------------------
    // PARSE MESSAGE LIST
    // --------------------------------------------------

    private fun parseMessages(element: JsonElement?)
            : List<Message> {

        if (element == null || !element.isJsonArray) {
            return emptyList()
        }

        return runCatching {
            element.asJsonArray.mapNotNull { item ->

                if (!item.isJsonObject) {
                    null
                } else {
                    gson.fromJson(item, Message::class.java)
                }
            }
        }.getOrDefault(emptyList())
    }

    // --------------------------------------------------
    // PARSE ONLINE MEMBERS
    // --------------------------------------------------

    private fun parseOnlineMembers(
        element: JsonElement?
    ): Set<String> {

        if (element == null || !element.isJsonArray) {
            return emptySet()
        }

        return element.asJsonArray.mapNotNull { item ->

            if (!item.isJsonObject) {
                return@mapNotNull null
            }

            item.asJsonObject.get("email")
                ?.takeUnless { it.isJsonNull }
                ?.asString

        }.toSet()
    }

    // --------------------------------------------------
    // ADD SINGLE MESSAGE
    // --------------------------------------------------

    private fun addMessage(message: Message) {

        _uiState.update { state ->

            val updatedMessages = mergeMessages(
                existing = state.messages,
                incoming = listOf(message)
            )

            state.copy(
                messages = updatedMessages
            )
        }
    }

    // --------------------------------------------------
    // MERGE AND DEDUPLICATE MESSAGES
    // --------------------------------------------------

    private fun mergeMessages(
        existing: List<Message>,
        incoming: List<Message>
    ): List<Message> {

        return (existing + incoming)
            .distinctBy { message ->
                message.id?.let { "id_$it" }
                    ?: gson.toJson(message)
            }
            .sortedBy { message ->
                message.id ?: Long.MAX_VALUE
            }
    }

    // --------------------------------------------------
    // CONNECT TO ROOM
    // --------------------------------------------------

    fun connectToRoom(
        room: String,
        email: String,
        name: String
    ) {

        if (room.isBlank() || email.isBlank()) {
            _uiState.update {
                it.copy(error = "Room and email are required.")
            }
            return
        }

        if (currentRoom == room &&
            _uiState.value.connectionState !=
            ChatConnectionState.DISCONNECTED
        ) {
            return
        }

        currentRoom = room

        _uiState.update {
            it.copy(
                error = null,
                isLoadingHistory = true
            )
        }

        repository.connectToRoom(
            room = room,
            email = email,
            name = name
        )
    }

    // --------------------------------------------------
    // SEND MESSAGE
    // --------------------------------------------------

    fun sendMessage(content: String) {

        val message = content.trim()

        if (message.isBlank()) return

        if (message.length > 2000) {
            _uiState.update {
                it.copy(
                    error = "Message cannot exceed 2000 characters."
                )
            }
            return
        }

        if (_uiState.value.connectionState !=
            ChatConnectionState.JOINED
        ) {
            _uiState.update {
                it.copy(error = "You are not connected to the room.")
            }
            return
        }

        val clientId = repository.sendMessage(message)

        if (clientId == null) {
            _uiState.update {
                it.copy(error = "Unable to send message.")
            }
        }
    }

    // --------------------------------------------------
    // TYPING STATUS
    // --------------------------------------------------

    fun sendTypingStatus(isTyping: Boolean) {

        if (_uiState.value.connectionState !=
            ChatConnectionState.JOINED
        ) {
            return
        }

        repository.sendTyping(isTyping)
    }

    // --------------------------------------------------
    // LOAD OLDER MESSAGES
    // --------------------------------------------------

    fun loadOlderMessages() {

        val state = _uiState.value

        if (state.isLoadingHistory ||
            !state.hasMoreHistory ||
            state.connectionState != ChatConnectionState.JOINED
        ) {
            return
        }

        val oldestMessageId = state.messages
            .mapNotNull { it.id }
            .minOrNull()
            ?: return

        if (oldestMessageId <= 0L) return

        _uiState.update {
            it.copy(
                isLoadingHistory = true,
                error = null
            )
        }

        repository.loadOlderMessages(
            beforeId = oldestMessageId,
            limit = 50
        )
    }

    // --------------------------------------------------
    // CLEAR ERROR
    // --------------------------------------------------

    fun clearError() {
        _uiState.update {
            it.copy(error = null)
        }
    }

    // --------------------------------------------------
    // LEAVE ROOM
    // --------------------------------------------------

    fun leaveRoom() {

        repository.leaveRoom()

        currentRoom = null

        _uiState.value = WebSocketUiState()
    }

    // --------------------------------------------------
    // DISCONNECT
    // --------------------------------------------------

    fun disconnect() {

        repository.disconnectWebSocket()

        currentRoom = null

        _uiState.update {
            it.copy(
                connectionState =
                    ChatConnectionState.DISCONNECTED,
                onlineMembers = emptySet(),
                typingUsers = emptySet(),
                isLoadingHistory = false
            )
        }
    }
}
