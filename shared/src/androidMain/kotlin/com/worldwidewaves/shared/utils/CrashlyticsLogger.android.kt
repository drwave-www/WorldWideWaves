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

import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.worldwidewaves.shared.BuildKonfig

/**
 * Android implementation of Crashlytics integration using Firebase Crashlytics SDK.
 *
 * ## Implementation Details
 * - Uses Firebase Crashlytics KTX extension for modern Kotlin API
 * - Only reports in production (BuildKonfig.DEBUG = false)
 * - Silent fallback prevents crashes if Crashlytics fails to initialize
 * - Tag included in log breadcrumbs for better context
 *
 * ## Production Safety
 * All methods wrapped in try-catch to prevent Crashlytics failures from affecting app functionality.
 */
actual object CrashlyticsLogger {
    actual fun recordException(
        throwable: Throwable,
        tag: String,
        message: String,
    ) {
        if (!BuildKonfig.DEBUG) {
            try {
                Firebase.crashlytics.log("[$tag] $message")
                Firebase.crashlytics.recordException(throwable)
            } catch (e: Exception) {
                // Silent fallback - don't crash if Crashlytics fails
            }
        }
    }

    actual fun log(message: String) {
        if (!BuildKonfig.DEBUG) {
            try {
                Firebase.crashlytics.log(message)
            } catch (e: Exception) {
                // Silent fallback
            }
        }
    }

    actual fun setCustomKey(
        key: String,
        value: String,
    ) {
        if (!BuildKonfig.DEBUG) {
            try {
                Firebase.crashlytics.setCustomKey(key, value)
            } catch (e: Exception) {
                // Silent fallback
            }
        }
    }
}
