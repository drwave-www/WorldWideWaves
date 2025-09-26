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

import io.github.aakira.napier.Napier

/**
 * Shared logging abstraction for WorldWideWaves
 *
 * This provides a unified logging interface across all platforms,
 * using Napier as the underlying implementation. This enables easy
 * migration from platform-specific logging (Android Log.e, etc.) to
 * shared logging that works on both Android and iOS.
 *
 * Usage:
 * ```kotlin
 * WWWLogger.e("MyTag", "Error message", exception)
 * WWWLogger.w("MyTag", "Warning message")
 * WWWLogger.d("MyTag", "Debug message")
 * WWWLogger.i("MyTag", "Info message")
 * ```
 */
object WWWLogger {

    /**
     * Log error message with optional throwable
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Napier.e(message, throwable, tag = tag)
    }

    /**
     * Log warning message
     */
    fun w(tag: String, message: String, throwable: Throwable? = null) {
        Napier.w(message, throwable, tag = tag)
    }

    /**
     * Log info message
     */
    fun i(tag: String, message: String, throwable: Throwable? = null) {
        Napier.i(message, throwable, tag = tag)
    }

    /**
     * Log debug message
     */
    fun d(tag: String, message: String, throwable: Throwable? = null) {
        Napier.d(message, throwable, tag = tag)
    }

    /**
     * Log verbose message
     */
    fun v(tag: String, message: String, throwable: Throwable? = null) {
        Napier.v(message, throwable, tag = tag)
    }
}