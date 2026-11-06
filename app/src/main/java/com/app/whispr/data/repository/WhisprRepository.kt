package com.app.whispr.data.repository

import com.app.movieexplorer2.utils.resource.Status
import com.app.whispr.api.ChatApi
import com.app.whispr.data.remote.dto.GroupDetailResponseDto
import com.app.whispr.data.remote.dto.GroupMessagesDto
import com.app.whispr.data.remote.dto.GroupRequest
import com.app.whispr.data.remote.dto.GroupResponseDto
import com.app.whispr.data.remote.websocket.ChatConnectionState
import com.app.whispr.data.remote.websocket.ChatEvent
import com.app.whispr.data.remote.websocket.ChatWebSocketClient
import com.app.whispr.utils.resource.Resource
import com.app.whispr.utils.resource.ResponseHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow


class WhisprRepository(
    private val api: ChatApi,
    private val webSocketClient: ChatWebSocketClient,
    private val responseHandler: ResponseHandler
) {

    //REST API calls Implementation
    fun getGroups(): Flow<Resource<GroupResponseDto>> = flow {
        emit(Resource.loading())

        try {
            val response = responseHandler.handleResponse(
                api.getGroups()
            )

            if(response.status == Status.SUCCESS) {

                val groups = response.data?.results
                    .orEmpty()
            }

            emit(response)
        } catch (e: Exception) {
            emit(responseHandler.handleException(e))
        }
    }.catch {
        emit(responseHandler.handleException(it))
    }

    fun getGroupMessages(slug: String): Flow<Resource<GroupMessagesDto>> = flow {
        emit(Resource.loading())

        try {
            val response = responseHandler.handleResponse(
                api.getGroupMessages(slug)
            )

            emit(response)
        } catch (e: Exception) {
            emit(responseHandler.handleException(e))
        }
    }.catch {
        emit(responseHandler.handleException(it))
    }

    fun getGroupDetails(slug: String): Flow<Resource<GroupDetailResponseDto>> = flow {
        emit(Resource.loading())

        try {
            val response = responseHandler.handleResponse(
                api.getGroupDetails(slug)
            )

            emit(response)
        } catch (e: Exception) {
            emit(responseHandler.handleException(e))
        }
    }.catch {
        emit(responseHandler.handleException(it))
    }

    fun createGroup(
        name: String,
        slug: String,
        description: String? = "",
        email: String
    ): Flow<Resource<GroupResponseDto>> = flow {
        emit(Resource.loading())
        try {
            val response = responseHandler.handleResponse(
                api.createGroups(
                    groupRequest = GroupRequest(
                        name = name,
                        slug = slug,
                        description = description,
                        email = email
                    )
                )
            )
            emit(response)
        } catch (e: Exception) {
            e.printStackTrace()
            emit(responseHandler.handleException(e))
        }
    }.catch {
        it.printStackTrace()
        emit(responseHandler.handleException(it))
    }

    //WebSocket Implementation
    val connectionState: StateFlow<ChatConnectionState>
        get() = webSocketClient.connectionState

    val socketEvents: SharedFlow<ChatEvent>
        get() = webSocketClient.events

    // Connect and join a chat room
    fun connectToRoom(
        room: String,
        email: String,
        name: String
    ) {
        webSocketClient.connect(
            room = room,
            email = email,
            name = name
        )
    }

    // Send chat message
    fun sendMessage(content: String): String? {
        return webSocketClient.sendMessage(content)
    }

    // Send typing status
    fun sendTyping(isTyping: Boolean) {
        webSocketClient.sendTyping(isTyping)
    }

    // Request older messages
    fun loadOlderMessages(
        beforeId: Long,
        limit: Int = 50
    ) {
        webSocketClient.loadHistory(
            beforeId = beforeId,
            limit = limit
        )
    }

    // Leave current room
    fun leaveRoom() {
        webSocketClient.leaveRoom()
    }

    // Disconnect without sending leave
    fun disconnectWebSocket() {
        webSocketClient.disconnect()
    }
}