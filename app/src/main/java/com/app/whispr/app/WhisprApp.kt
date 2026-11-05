package com.app.whispr.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class WhisprApp : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}