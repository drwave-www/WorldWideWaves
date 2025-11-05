package com.worldwidewaves.shared

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

import com.worldwidewaves.shared.utils.CrashlyticsLogger
import com.worldwidewaves.shared.utils.Log
import kotlin.experimental.ExperimentalNativeApi

/**
 * Installs iOS lifecycle hooks for crash reporting and debugging.
 *
 * ## Purpose
 * - Captures uncaught Kotlin/Native exceptions
 * - Logs exceptions locally for debugging
 * - Reports exceptions to Firebase Crashlytics (when bridge is configured)
 *
 * ## Usage
 * Call once at app initialization from SceneDelegate.swift:
 * ```swift
 * IosLifecycleHookKt.installIosLifecycleHook()
 * ```
 *
 * ## Exception Handling Flow
 * 1. Kotlin/Native throws unhandled exception
 * 2. setUnhandledExceptionHook captures it
 * 3. Log exception locally (console + Log)
 * 4. Report to Crashlytics (if bridge is configured)
 * 5. Print stack trace in debug builds
 *
 * ## Production Safety
 * - Does not prevent app termination (expected behavior for fatal crashes)
 * - Ensures crash is logged before termination
 * - No additional crashes from crash handling itself
 */
@OptIn(ExperimentalNativeApi::class)
@Throws(Throwable::class)
fun installIosLifecycleHook() {
    setUnhandledExceptionHook { throwable ->
        val exceptionClass = throwable::class.qualifiedName ?: "UnknownException"
        val exceptionMessage = throwable.message ?: "No message"

        // Log to console immediately (survives app termination)
        println("⚠️ FATAL K/N Exception: $exceptionClass: $exceptionMessage")

        try {
            // Log to our logging system
            Log.wtf(
                "IosLifecycleHook",
                "Uncaught Kotlin/Native exception: $exceptionClass",
            )

            // Report to Crashlytics (will be logged locally if bridge not configured)
            CrashlyticsLogger.recordException(
                throwable = throwable,
                tag = "UncaughtException",
                message = "Fatal Kotlin/Native exception",
            )

            // Print full stack trace in debug builds
            if (BuildKonfig.DEBUG) {
                println("Stack trace:")
                throwable.printStackTrace()
            }
        } catch (e: Exception) {
            // Emergency fallback: don't let crash reporting crash the app
            println("ERROR: Failed to report crash: ${e.message}")
        }
    }
}
