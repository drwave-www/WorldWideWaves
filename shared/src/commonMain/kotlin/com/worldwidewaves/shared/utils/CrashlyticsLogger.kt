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
 * Platform-specific Crashlytics integration for non-fatal error reporting.
 *
 * This wrapper provides cross-platform access to Firebase Crashlytics for tracking
 * non-fatal exceptions and errors in production. It integrates with the Log utility
 * to automatically forward errors and warnings to Crashlytics for monitoring.
 *
 * ## Behavior
 * - Only sends data in production builds (when DEBUG = false)
 * - Silently fails if Crashlytics is not available
 * - Includes tag and message as breadcrumbs for crash context
 *
 * ## Usage
 * ```kotlin
 * CrashlyticsLogger.recordException(exception, "MyTag", "Operation failed")
 * CrashlyticsLogger.log("User navigated to screen X")
 * CrashlyticsLogger.setCustomKey("user_id", userId)
 * ```
 *
 * Platform implementations:
 * - Android: Uses Firebase Crashlytics SDK
 * - iOS: Uses Firebase Crashlytics via CocoaPods
 */
expect object CrashlyticsLogger {
    /**
     * Records a non-fatal exception to Crashlytics.
     * Only sends in production builds (when DEBUG = false).
     *
     * @param throwable The exception to record
     * @param tag The log tag for context
     * @param message A descriptive message about the error
     */
    fun recordException(
        throwable: Throwable,
        tag: String,
        message: String,
    )

    /**
     * Logs a breadcrumb for crash context.
     * Breadcrumbs appear in crash reports to provide context about what happened before a crash.
     *
     * @param message The breadcrumb message
     */
    fun log(message: String)

    /**
     * Sets a custom key for crash reporting context.
     * Custom keys persist across app sessions and appear in all subsequent crash reports.
     *
     * @param key The custom key name
     * @param value The custom key value
     */
    fun setCustomKey(
        key: String,
        value: String,
    )
}
