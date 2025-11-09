@file:Suppress("MatchingDeclarationName") // expect/actual pattern requires .ios.kt suffix

package com.worldwidewaves.shared.utils

import com.worldwidewaves.crashlytics.CrashlyticsBridge
import com.worldwidewaves.shared.BuildKonfig
import kotlinx.cinterop.ExperimentalForeignApi

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

/**
 * iOS implementation of Crashlytics integration.
 *
 * ## Current Status: FULLY INTEGRATED ✅
 *
 * This implementation sends all exceptions, breadcrumbs, and custom keys to Firebase Crashlytics
 * via the Swift bridge (CrashlyticsBridge.swift).
 *
 * ## Implementation Details
 * - Reports in production (BuildKonfig.DEBUG = false) or when forceEnableCrashReporting = true
 * - Uses cinterop to call Swift bridge which forwards to Firebase iOS SDK
 * - Also logs locally for debugging purposes
 *
 * ## Testing Support
 * Use [enableTestReporting] to force crash reporting in DEBUG builds for testing purposes.
 * This matches the Android implementation for consistent cross-platform behavior.
 *
 * ## Architecture
 * ```
 * Kotlin Shared Code → CrashlyticsLogger.ios.kt
 *                   ↓ (via cinterop)
 *         CrashlyticsBridge.swift → Firebase iOS SDK
 * ```
 *
 * ## What Works Now
 * - Fatal crashes: ✅ Automatically captured by Firebase
 * - Non-fatal exceptions from Kotlin: ✅ Sent to Firebase via bridge
 * - Breadcrumbs: ✅ Sent to Firebase via bridge
 * - Custom keys: ✅ Sent to Firebase via bridge
 * - DEBUG mode control: ✅ Matches Android behavior
 * - dSYM upload: ✅ Enabled for all configurations
 * - Crashlytics initialization: ✅ Explicit init in AppDelegate
 *
 * ## Production Safety
 * - No force unwraps or unsafe operations
 * - Logs locally in addition to Firebase reporting for debugging
 * - No crashes from Crashlytics integration itself
 */
@OptIn(ExperimentalForeignApi::class)
actual object CrashlyticsLogger {
    private const val TAG = "CrashlyticsLogger.iOS"

    /**
     * Force enable crash reporting even in DEBUG builds.
     * Useful for testing Crashlytics integration without building release build.
     * Matches Android implementation for cross-platform consistency.
     */
    private var forceEnableCrashReporting = false

    /**
     * Enable crash reporting in DEBUG builds for testing purposes.
     * This allows verification of Crashlytics integration without building release build.
     * Matches Android implementation.
     */
    fun enableTestReporting() {
        forceEnableCrashReporting = true
        Log.i(TAG, "Test reporting enabled - crashes will be reported in DEBUG builds")
    }

    /**
     * Disable test reporting to return to normal behavior (production-only reporting).
     * Matches Android implementation.
     */
    fun disableTestReporting() {
        forceEnableCrashReporting = false
        Log.i(TAG, "Test reporting disabled - returning to production-only reporting")
    }

    /**
     * Check if crash reporting is currently enabled.
     * Matches Android implementation logic.
     */
    private fun isReportingEnabled(): Boolean = !BuildKonfig.DEBUG || forceEnableCrashReporting

    /**
     * Record a non-fatal exception from Kotlin code.
     *
     * Sends exception to Firebase Crashlytics via Swift bridge and logs locally for debugging.
     * Respects DEBUG mode - only reports in production unless test reporting is enabled.
     *
     * @param throwable The exception that occurred
     * @param tag The component/module where the exception occurred
     * @param message Additional context message
     */
    actual fun recordException(
        throwable: Throwable,
        tag: String,
        message: String,
    ) {
        if (isReportingEnabled()) {
            // Get stack trace as string
            val stackTrace = throwable.stackTraceToString()

            // Build full error message
            val fullMessage = "$message: ${throwable.message ?: "Unknown error"}"

            // Send exception to Firebase via Swift bridge
            CrashlyticsBridge.recordExceptionWithMessage(fullMessage, tag, stackTrace)

            // Also log locally for debugging
            Log.e(TAG, "[$tag] $fullMessage\n$stackTrace")
        }
    }

    /**
     * Log a message that will appear as breadcrumb in crash reports.
     *
     * Sends breadcrumb to Firebase Crashlytics via Swift bridge and logs locally for debugging.
     * Respects DEBUG mode - only reports in production unless test reporting is enabled.
     *
     * @param message The breadcrumb message
     */
    actual fun log(message: String) {
        if (isReportingEnabled()) {
            // Extract tag from message if present (format: "[Tag] Message")
            val (tag, cleanMessage) =
                if (message.startsWith("[") && message.contains("]")) {
                    val endIndex = message.indexOf("]")
                    message.substring(1, endIndex) to message.substring(endIndex + 2)
                } else {
                    "App" to message
                }

            // Send breadcrumb to Firebase via Swift bridge
            CrashlyticsBridge.logWithMessage(cleanMessage, tag)

            // Also log locally for debugging
            Log.d(TAG, "Breadcrumb: [$tag] $cleanMessage")
        }
    }

    /**
     * Set a custom key-value pair that will appear in crash reports.
     *
     * Sends custom key to Firebase Crashlytics via Swift bridge and logs locally for debugging.
     * Respects DEBUG mode - only reports in production unless test reporting is enabled.
     *
     * @param key The key name
     * @param value The value (will be converted to string)
     */
    actual fun setCustomKey(
        key: String,
        value: String,
    ) {
        if (isReportingEnabled()) {
            // Send custom key to Firebase via Swift bridge
            CrashlyticsBridge.setCustomKeyWithKey(key, value)

            // Also log locally for debugging
            Log.d(TAG, "Custom key: $key = $value")
        }
    }
}
