package com.worldwidewaves.shared.utils

/*
 * Copyright 2025 DrWave
 *
 * WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and
 * countries. The project aims to transcend physical and cultural
 * boundaries, fostering unity, community, and shared human experience by leveraging real-time
 * coordination and location-based services.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.worldwidewaves.shared.WWWGlobals
import kotlinx.coroutines.tasks.await
import org.koin.mp.KoinPlatform
import kotlin.time.Duration.Companion.hours

/**
 * Android implementation of RuntimeLogConfig using Firebase Remote Config.
 */
actual object RuntimeLogConfig {
    private const val TAG = "WWW.Log.Config"

    // Remote Config keys
    private const val KEY_LOG_LEVEL_GLOBAL = "log_level_global"
    private const val KEY_PREFIX_LOG_LEVEL_TAG = "log_level_"
    private const val KEY_ENABLE_VERBOSE_LOGGING = "enable_verbose_logging"
    private const val KEY_ENABLE_DEBUG_LOGGING = "enable_debug_logging"
    private const val KEY_ENABLE_PERFORMANCE_LOGGING = "enable_performance_logging"
    private const val KEY_ENABLE_POSITION_TRACKING_LOGGING = "enable_position_tracking_logging"

    // Fetch interval for remote config (12 hours)
    private val FETCH_INTERVAL = 12.hours.inWholeSeconds

    private var remoteConfig: FirebaseRemoteConfig? = null
    private var initialized = false

    /**
     * Initialize Firebase Remote Config and fetch latest values.
     */
    actual suspend fun initialize() {
        if (initialized) {
            Log.v(TAG, "RuntimeLogConfig already initialized")
            return
        }

        try {
            // Get context from Koin
            val context = KoinPlatform.getKoin().get<Context>()

            // Ensure FirebaseApp is initialized
            if (FirebaseApp.getApps(context).isEmpty()) {
                Log.i(TAG, "Initializing FirebaseApp for RuntimeLogConfig")
                FirebaseApp.initializeApp(context)
            }

            // Get Remote Config instance
            remoteConfig = FirebaseRemoteConfig.getInstance()

            // Configure Remote Config settings
            val configSettings =
                FirebaseRemoteConfigSettings
                    .Builder()
                    .setMinimumFetchIntervalInSeconds(FETCH_INTERVAL)
                    .build()

            remoteConfig?.setConfigSettingsAsync(configSettings)

            // Set default values based on build configuration
            val defaults =
                mapOf(
                    KEY_LOG_LEVEL_GLOBAL to "INFO",
                    KEY_ENABLE_VERBOSE_LOGGING to WWWGlobals.LogConfig.ENABLE_VERBOSE_LOGGING,
                    KEY_ENABLE_DEBUG_LOGGING to WWWGlobals.LogConfig.ENABLE_DEBUG_LOGGING,
                    KEY_ENABLE_PERFORMANCE_LOGGING to WWWGlobals.LogConfig.ENABLE_PERFORMANCE_LOGGING,
                    KEY_ENABLE_POSITION_TRACKING_LOGGING to WWWGlobals.LogConfig.ENABLE_POSITION_TRACKING_LOGGING,
                )

            remoteConfig?.setDefaultsAsync(defaults)

            // Fetch and activate remote config
            try {
                remoteConfig?.fetchAndActivate()?.await()
                Log.i(TAG, "RuntimeLogConfig initialized successfully")
            } catch (e: Exception) {
                // Non-fatal: fetch failed but defaults are set
                Log.w(TAG, "Failed to fetch remote config, using defaults", e)
            }

            initialized = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize RuntimeLogConfig", e)
            // Don't throw - fall back to build configuration
            initialized = false
        }
    }

    /**
     * Check if a specific log level should be enabled for a tag.
     *
     * Checks in order:
     * 1. Tag-specific remote config (e.g., "log_level_PositionManager")
     * 2. Global remote config ("log_level_global")
     * 3. Feature-specific remote config (e.g., "enable_verbose_logging")
     * 4. Build configuration (fallback)
     */
    actual fun shouldLog(
        tag: String,
        level: LogLevel,
    ): Boolean {
        if (!initialized || remoteConfig == null) {
            // Fall back to build configuration
            return shouldLogBuildConfig(level)
        }

        val minLevel = getMinLogLevel(tag)
        return level.priority >= minLevel.priority
    }

    /**
     * Get minimum log level for a specific tag from remote config.
     */
    actual fun getMinLogLevel(tag: String): LogLevel {
        if (!initialized || remoteConfig == null) {
            // Fall back to build configuration
            return getMinLogLevelBuildConfig()
        }

        try {
            // Check tag-specific config first
            val tagKey = KEY_PREFIX_LOG_LEVEL_TAG + tag.replace(".", "_")
            val tagLevelStr = remoteConfig?.getString(tagKey)
            if (!tagLevelStr.isNullOrEmpty()) {
                val tagLevel = LogLevel.fromString(tagLevelStr)
                if (tagLevel != null) {
                    return tagLevel
                }
            }

            // Fall back to global config
            val globalLevelStr = remoteConfig?.getString(KEY_LOG_LEVEL_GLOBAL)
            if (!globalLevelStr.isNullOrEmpty()) {
                val globalLevel = LogLevel.fromString(globalLevelStr)
                if (globalLevel != null) {
                    return globalLevel
                }
            }

            // Fall back to feature-specific flags
            val enableVerbose = remoteConfig?.getBoolean(KEY_ENABLE_VERBOSE_LOGGING) ?: false
            val enableDebug = remoteConfig?.getBoolean(KEY_ENABLE_DEBUG_LOGGING) ?: false

            return when {
                enableVerbose -> LogLevel.VERBOSE
                enableDebug -> LogLevel.DEBUG
                else -> LogLevel.INFO
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading remote config, using build config", e)
            return getMinLogLevelBuildConfig()
        }
    }

    // ============================================================
    // FALLBACK TO BUILD CONFIGURATION
    // ============================================================

    /**
     * Check if log should be enabled based on build configuration (fallback).
     */
    private fun shouldLogBuildConfig(level: LogLevel): Boolean =
        when (level) {
            LogLevel.VERBOSE -> WWWGlobals.LogConfig.ENABLE_VERBOSE_LOGGING
            LogLevel.DEBUG -> WWWGlobals.LogConfig.ENABLE_DEBUG_LOGGING
            LogLevel.INFO, LogLevel.WARNING, LogLevel.ERROR -> true
        }

    /**
     * Get minimum log level from build configuration (fallback).
     */
    private fun getMinLogLevelBuildConfig(): LogLevel =
        when {
            WWWGlobals.LogConfig.ENABLE_VERBOSE_LOGGING -> LogLevel.VERBOSE
            WWWGlobals.LogConfig.ENABLE_DEBUG_LOGGING -> LogLevel.DEBUG
            else -> LogLevel.INFO
        }

    // ============================================================
    // TEST SUPPORT
    // ============================================================

    /**
     * Reset initialization state for testing.
     * INTERNAL USE ONLY.
     */
    internal fun resetForTesting() {
        initialized = false
        remoteConfig = null
    }
}
