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
 * Production-ready logging wrapper that respects build configuration flags for performance and security.
 *
 * This utility bridges Napier cross-platform logging with build-specific configuration to ensure:
 * - Verbose/debug logging is disabled in release builds for performance
 * - High-frequency performance logging can be controlled independently
 * - Consistent logging behavior across the entire application
 * - Cross-platform compatibility through Napier
 */
object Log {
    /**
     * Verbose logging - disabled in release builds for performance.
     * Use for detailed debugging information.
     */
    fun v(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    ) {
        if (com.worldwidewaves.shared.WWWGlobals.LogConfig.ENABLE_VERBOSE_LOGGING) {
            Napier.v(tag = tag, message = message, throwable = throwable)
        }
    }

    /**
     * Debug logging - disabled in release builds for performance.
     * Use for general debugging information.
     */
    fun d(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    ) {
        if (com.worldwidewaves.shared.WWWGlobals.LogConfig.ENABLE_DEBUG_LOGGING) {
            Napier.d(tag = tag, message = message, throwable = throwable)
        }
    }

    /**
     * Performance logging - controlled by build configuration.
     * Use for high-frequency performance measurements that may impact app performance.
     */
    fun performance(
        tag: String,
        message: String,
    ) {
        if (com.worldwidewaves.shared.WWWGlobals.LogConfig.ENABLE_PERFORMANCE_LOGGING) {
            Napier.v(tag = tag, message = "[PERF] $message")
        }
    }

    /**
     * Info logging - always enabled as it's essential for production monitoring.
     * Use for important application state changes and user actions.
     */
    fun i(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    ) = Napier.i(tag = tag, message = message, throwable = throwable)

    /**
     * Warning logging - always enabled for production issue detection.
     * Use for recoverable errors and important warnings.
     */
    fun w(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    ) = Napier.w(tag = tag, message = message, throwable = throwable)

    /**
     * Error logging - always enabled for critical issue tracking.
     * Use for errors that need immediate attention.
     */
    fun e(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    ) = Napier.e(tag = tag, message = message, throwable = throwable)

    /**
     * Critical error logging - always enabled for catastrophic failures.
     * Use for errors that should never happen in production.
     */
    fun wtf(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    ) = Napier.wtf(tag = tag, message = message, throwable = throwable)
}
