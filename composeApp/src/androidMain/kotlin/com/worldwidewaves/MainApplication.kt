package com.worldwidewaves

import android.app.Application
import androidx.work.Configuration

class MainApplication : Application(), Configuration.Provider {
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) android.util.Log.DEBUG else android.util.Log.ERROR)
            .build()

        override fun onCreate() {
            super.onCreate()

            // Initialize the WWW Compose platform
            AndroidPlatform.initialize(this)
        }
}