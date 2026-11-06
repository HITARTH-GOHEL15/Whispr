package com.app.whispr.di

import com.app.whispr.app.AppConstants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named

@Module
@InstallIn(SingletonComponent::class)
object WebSocketModule {

    @Provides
    @Named("websocket_base_url")
    fun provideWebSocketBaseUrl(): String {
        return AppConstants.WS_BASE_URL
    }
}
