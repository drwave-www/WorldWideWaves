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

/**
 * iOS implementation of Crashlytics integration.
 *
 * ## Current Status: DISABLED (No-op Implementation)
 *
 * **Why Disabled:**
 * Firebase iOS SDK contains Swift dependencies that conflict with Kotlin/Native linking.
 * Attempting to bridge Firebase through cinterop results in linker errors due to Swift
 * compatibility libraries not being available in Kotlin/Native context.
 *
 * ## Current Crash Reporting Strategy
 *
 * **iOS Native Crashes** (Swift/ObjC):
 * - ✅ **Fully working** via Firebase Crashlytics SDK directly in iOS app
 * - Captures Swift/ObjC crashes, exceptions, and native issues
 * - No Kotlin/Native bridge needed
 *
 * **Android Crashes** (Kotlin shared code):
 * - ✅ **Fully working** via CrashlyticsLogger.android.kt
 * - Captures all Kotlin exceptions and crashes
 * - Reports to Firebase Crashlytics
 *
 * **iOS Kotlin Crashes** (Kotlin shared code on iOS):
 * - ❌ **Not reported** to Crashlytics (logged locally only)
 * - Rare occurrence - most logic is in platform-specific code
 * - Can be debugged via local logs
 *
 * ## Implementation Details
 * - All methods are no-ops (do nothing)
 * - Logs locally for debugging visibility
 * - Matches expect/actual contract for compilation
 *
 * ## Future Improvement
 * If needed, could implement via:
 * 1. Swift package that wraps Firebase and exposes C API
 * 2. XCFramework approach
 * 3. Wait for Kotlin/Native Swift interop improvements
 *
 * ## Architecture (Current)
 * ```
 * iOS App (Swift) → Firebase Crashlytics SDK (native crashes) ✅
 * Kotlin Shared → CrashlyticsLogger.ios.kt (no-op, local logs only) ⏭️
 * Android Kotlin → CrashlyticsLogger.android.kt → Firebase ✅
 * ```
 *
 * See: docs/ios/crashlytics-static-library-architecture.md for full context
 */
actual object CrashlyticsLogger {
    private const val TAG = "CrashlyticsLogger.iOS"

    /**
     * Record a non-fatal exception from Kotlin code.
     *
     * **iOS Implementation: No-op**
     * - Logs locally for debugging
     * - Does NOT send to Firebase (Kotlin/Native bridge unavailable)
     * - Native iOS crashes still reported via direct Firebase integration
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
        // Log locally for debugging (not sent to Crashlytics)
        val stackTrace = throwable.stackTraceToString()
        val fullMessage = "$message: ${throwable.message ?: "Unknown error"}"
        Log.e(TAG, "[$tag] $fullMessage\n$stackTrace")
        Log.w(TAG, "Note: Kotlin exceptions on iOS are not reported to Crashlytics")
    }

    /**
     * Log a message that will appear as breadcrumb in crash reports.
     *
     * **iOS Implementation: No-op**
     * - Logs locally for debugging
     * - Does NOT send to Firebase
     *
     * @param message The breadcrumb message
     */
    actual fun log(message: String) {
        // Log locally for debugging (not sent to Crashlytics)
        Log.d(TAG, "Breadcrumb (local only): $message")
    }

    /**
     * Set a custom key-value pair that will appear in crash reports.
     *
     * **iOS Implementation: No-op**
     * - Logs locally for debugging
     * - Does NOT send to Firebase
     *
     * @param key The key name
     * @param value The value (will be converted to string)
     */
    actual fun setCustomKey(
        key: String,
        value: String,
    ) {
        // Log locally for debugging (not sent to Crashlytics)
        Log.d(TAG, "Custom key (local only): $key = $value")
    }
}
