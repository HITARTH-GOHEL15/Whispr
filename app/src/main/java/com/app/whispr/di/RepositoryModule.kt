package com.app.whispr.di

import com.app.whispr.api.ChatApi
import com.app.whispr.data.remote.websocket.ChatWebSocketClient
import com.app.whispr.data.repository.WhisprRepository
import com.app.whispr.utils.resource.ResponseHandler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class RepositoryModule {

    @Singleton
    @Provides
    fun provideChatRepository(
        api: ChatApi,
        webSocketClient: ChatWebSocketClient,
        responseHandler: ResponseHandler
    ): WhisprRepository {
        return WhisprRepository(api, webSocketClient, responseHandler)
    }
}