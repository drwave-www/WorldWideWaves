@file:Suppress("MatchingDeclarationName") // expect/actual pattern requires .ios.kt suffix

package com.worldwidewaves.shared.utils

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
 * ## Current Status: LOGGING ONLY (Bridge Ready for Integration)
 *
 * This implementation currently logs exceptions locally while the Swift bridge
 * (CrashlyticsBridge.swift) is integrated into the Xcode project.
 *
 * ## Implementation Details
 * - Reports in production (BuildKonfig.DEBUG = false) or when forceEnableCrashReporting = true
 * - Currently logs locally (bridge integration pending)
 * - Graceful fallback prevents crashes if bridge is not available
 *
 * ## Testing Support
 * Use [enableTestReporting] to force crash reporting in DEBUG builds for testing purposes.
 * This matches the Android implementation for consistent cross-platform behavior.
 *
 * ## Architecture
 * ```
 * Kotlin Shared Code → CrashlyticsLogger.ios.kt (logs locally)
 *                   ↓
 *            (Future: CrashlyticsBridge.swift → Firebase iOS SDK)
 * ```
 *
 * ## What Works Now
 * - Fatal crashes: ✅ Automatically captured by Firebase
 * - Non-fatal exceptions from Kotlin: ⚠️ Logged locally (not sent to Firebase)
 * - DEBUG mode control: ✅ Matches Android behavior
 * - dSYM upload: ✅ Enabled for all configurations
 * - Crashlytics initialization: ✅ Explicit init in AppDelegate
 *
 * ## What Needs Manual Integration
 * - Swift bridge needs cinterop definition file for Kotlin/Native access
 * - After integration, uncomment bridge calls in methods
 * - Then non-fatal exceptions will be sent to Firebase
 *
 * ## Production Safety
 * - No force unwraps or unsafe operations
 * - Graceful fallback (logs locally)
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
     * CURRENT: Logs exception locally for debugging (respects DEBUG mode)
     * FUTURE: Will send to Firebase once bridge is integrated
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

            // TODO: Once CrashlyticsBridge.swift is added to Xcode project, uncomment:
            // CrashlyticsBridge.recordException(message: fullMessage, tag: tag, stackTrace: stackTrace)

            // For now, log locally
            Log.e(TAG, "[$tag] $fullMessage\n$stackTrace")
            Log.i(TAG, "Note: Exception logged locally. Add CrashlyticsBridge.swift to Xcode to send to Firebase.")
        }
    }

    /**
     * Log a message that will appear as breadcrumb in crash reports.
     *
     * CURRENT: Logs locally for debugging (respects DEBUG mode)
     * FUTURE: Will appear in Firebase crash reports once bridge is integrated
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

            // TODO: Once CrashlyticsBridge.swift is added to Xcode project, uncomment:
            // CrashlyticsBridge.log(message: cleanMessage, tag: tag)

            // For now, log locally
            Log.d(TAG, "Breadcrumb: [$tag] $cleanMessage")
        }
    }

    /**
     * Set a custom key-value pair that will appear in crash reports.
     *
     * CURRENT: Logs locally for debugging (respects DEBUG mode)
     * FUTURE: Will appear in Firebase crash reports once bridge is integrated
     *
     * @param key The key name
     * @param value The value (will be converted to string)
     */
    actual fun setCustomKey(
        key: String,
        value: String,
    ) {
        if (isReportingEnabled()) {
            // TODO: Once CrashlyticsBridge.swift is added to Xcode project, uncomment:
            // CrashlyticsBridge.setCustomKey(key: key, value: value)

            // For now, log locally
            Log.d(TAG, "Custom key: $key = $value")
        }
    }
}
