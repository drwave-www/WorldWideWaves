@file:Suppress("MatchingDeclarationName") // expect/actual pattern requires .ios.kt suffix

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

import com.worldwidewaves.shared.WWWGlobals

/**
 * iOS implementation of RuntimeLogConfig.
 *
 * Currently uses build-time configuration only (fallback mode).
 * Firebase Remote Config integration requires FirebaseRemoteConfig CocoaPod
 * to be added to the iOS project first.
 *
 * To enable Firebase Remote Config on iOS:
 * 1. Add FirebaseRemoteConfig pod to iosApp/Podfile
 * 2. Run pod install
 * 3. Replace this stub with Firebase implementation
 *
 * See: docs/setup/firebase-ios-setup.md for configuration instructions
 */
actual object RuntimeLogConfig {
    private const val TAG = "WWW.Log.Config"

    /**
     * Initialize remote config (stub - uses build config fallback).
     *
     * NOTE: Firebase Remote Config integration deferred - using build config fallback.
     * Implementation requires Firebase/RemoteConfig CocoaPod configuration.
     */
    actual suspend fun initialize() {
        Log.i(TAG, "RuntimeLogConfig initialized (using build config fallback on iOS)")
    }

    /**
     * Check if logging should be enabled for a given tag and level.
     * Falls back to build configuration (BuildKonfig flags).
     */
    actual fun shouldLog(
        tag: String,
        level: LogLevel,
    ): Boolean {
        // Get build-time configuration
        val buildConfigAllows =
            when (level) {
                LogLevel.VERBOSE -> WWWGlobals.LogConfig.ENABLE_VERBOSE_LOGGING
                LogLevel.DEBUG -> WWWGlobals.LogConfig.ENABLE_DEBUG_LOGGING
                LogLevel.INFO -> true // Always allow INFO in production
                LogLevel.WARNING -> true // Always allow WARN in production
                LogLevel.ERROR -> true // Always allow ERROR in production
            }

        return buildConfigAllows
    }

    /**
     * Get minimum log level for a tag (stub - returns build config minimum).
     */
    actual fun getMinLogLevel(tag: String): LogLevel =
        when {
            WWWGlobals.LogConfig.ENABLE_VERBOSE_LOGGING -> LogLevel.VERBOSE
            WWWGlobals.LogConfig.ENABLE_DEBUG_LOGGING -> LogLevel.DEBUG
            else -> LogLevel.INFO
        }
}
