@file:Suppress("MatchingDeclarationName") // expect/actual pattern requires .ios.kt suffix

package com.worldwidewaves.shared.utils

/* * Copyright 2025 DrWave
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
 * limitations under the License. */


// TODO: Re-enable once CrashlyticsBridge Swift file is properly linked
// import com.worldwidewaves.crashlytics.CrashlyticsBridge
import com.worldwidewaves.shared.BuildKonfig
// import kotlinx.cinterop.ExperimentalForeignApi

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
 * ## Current Status: TEMPORARILY DISABLED ⚠️
 *
 * Crashlytics integration is temporarily disabled due to linking issues with CrashlyticsBridge.swift.
 * The Swift bridge file exists but is not being compiled into the iOS app binary, causing runtime crashes.
 *
 * ## TODO: Re-enable Crashlytics
 * 1. Ensure CrashlyticsBridge.swift is added to Xcode project target
 * 2. Verify Swift file is being compiled (check build logs)
 * 3. Re-enable cinterop in shared/build.gradle.kts
 * 4. Uncomment import statements above
 * 5. Restore original implementation from git history
 *
 * ## Temporary Behavior
 * - All methods are no-ops (log locally only)
 * - No crash reporting to Firebase
 * - App will not crash due to missing CrashlyticsBridge symbol
 */
// @OptIn(ExperimentalForeignApi::class)
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
        // TODO: Re-enable Firebase reporting once CrashlyticsBridge is properly linked
        // For now, just log locally
        val stackTrace = throwable.stackTraceToString()
        val fullMessage = "$message: ${throwable.message ?: "Unknown error"}"
        Log.e(TAG, "[$tag] $fullMessage\n$stackTrace")
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
        // TODO: Re-enable Firebase reporting once CrashlyticsBridge is properly linked
        // For now, just log locally
        Log.d(TAG, "Breadcrumb: $message")
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
        // TODO: Re-enable Firebase reporting once CrashlyticsBridge is properly linked
        // For now, just log locally
        Log.d(TAG, "Custom key: $key = $value")
    }
}
