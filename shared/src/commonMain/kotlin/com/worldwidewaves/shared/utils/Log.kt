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
import io.github.aakira.napier.Napier

/**
 * Production-ready logging wrapper that respects build configuration flags for performance and security.
 *
 * This utility bridges Napier cross-platform logging with build-specific configuration to ensure:
 * - Verbose/debug logging is disabled in release builds for performance
 * - High-frequency performance logging can be controlled independently
 * - Consistent logging behavior across the entire application
 * - Cross-platform compatibility through Napier
 * - Distributed tracing support via correlation IDs
 * - Structured logging with key-value pairs
 *
 * ## Correlation ID Support
 * Correlation IDs are available via [CorrelationContext] but are not automatically included
 * in log messages to avoid iOS threading issues. To include correlation IDs:
 * ```kotlin
 * suspend fun logWithCorrelation() = withCorrelation("REQUEST-123") {
 *     val cid = CorrelationContext.getCurrentId()
 *     Log.i("Handler", "[$cid] Processing request")
 * }
 * ```
 *
 * ## Structured Logging
 * Use structured logging for machine-parseable logs:
 * ```kotlin
 * Log.structured(TAG, LogLevel.INFO,
 *     "event" to "wave_detected",
 *     "event_id" to event.id,
 *     "distance_m" to distance
 * )
 * // Output: event=wave_detected event_id=abc123 distance_m=50.0
 * ```
 *
 * See [CorrelationContext] for correlation usage examples.
 */
object Log {
    /**
     * Get the current correlation ID prefix for log messages.
     * Returns empty string since correlation IDs are only available in suspend contexts.
     *
     * Note: Correlation ID support in non-suspend log methods has been removed to avoid
     * iOS threading issues with runBlocking. For correlation ID support, use structured
     * logging within suspend functions or access CorrelationContext.getCurrentId() manually.
     */
    private fun getCorrelationPrefix(): String = ""

    /**
     * Verbose logging - disabled in release builds for performance.
     * Use for detailed debugging information.
     */
    @Throws(Throwable::class)
    fun v(
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        if (WWWGlobals.LogConfig.ENABLE_VERBOSE_LOGGING) {
            val prefix = getCorrelationPrefix()
            Napier.v(tag = tag, message = "$prefix$message", throwable = throwable)
        }
    }

    @Throws(Throwable::class)
    fun v(
        tag: String,
        message: String,
    ) = v(tag, message, null)

    /**
     * Debug logging - disabled in release builds for performance.
     * Use for general debugging information.
     */
    @Throws(Throwable::class)
    fun d(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    ) {
        if (WWWGlobals.LogConfig.ENABLE_DEBUG_LOGGING) {
            val prefix = getCorrelationPrefix()
            Napier.d(tag = tag, message = "$prefix$message", throwable = throwable)
        }
    }

    @Throws(Throwable::class)
    fun d(
        tag: String,
        message: String,
    ) = d(tag, message, null)

    /**
     * Performance logging - controlled by build configuration.
     * Use for high-frequency performance measurements that may impact app performance.
     */
    @Throws(Throwable::class)
    fun performance(
        tag: String,
        message: String,
    ) {
        if (WWWGlobals.LogConfig.ENABLE_PERFORMANCE_LOGGING) {
            val prefix = getCorrelationPrefix()
            Napier.v(tag = tag, message = "$prefix[PERF] $message")
        }
    }

    /**
     * Info logging - always enabled as it's essential for production monitoring.
     * Use for important application state changes and user actions.
     */
    @Throws(Throwable::class)
    fun i(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    ) {
        val prefix = getCorrelationPrefix()
        Napier.i(tag = tag, message = "$prefix$message", throwable = throwable)
    }

    @Throws(Throwable::class)
    fun i(
        tag: String,
        message: String,
    ) = i(tag, message, null)

    /**
     * Warning logging - always enabled for production issue detection.
     * Use for recoverable errors and important warnings.
     */
    @Throws(Throwable::class)
    fun w(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    ) {
        val prefix = getCorrelationPrefix()
        Napier.w(tag = tag, message = "$prefix$message", throwable = throwable)
    }

    @Throws(Throwable::class)
    fun w(
        tag: String,
        message: String,
    ) = w(tag, message, null)

    /**
     * Error logging - always enabled for critical issue tracking.
     * Use for errors that need immediate attention.
     */
    @Throws(Throwable::class)
    fun e(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    ) {
        val prefix = getCorrelationPrefix()
        Napier.e(tag = tag, message = "$prefix$message", throwable = throwable)
    }

    @Throws(Throwable::class)
    fun e(
        tag: String,
        message: String,
    ) = e(tag, message, null)

    /**
     * Critical error logging - always enabled for catastrophic failures.
     * Use for errors that should never happen in production.
     */
    @Throws(Throwable::class)
    fun wtf(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    ) {
        val prefix = getCorrelationPrefix()
        Napier.wtf(tag = tag, message = "$prefix$message", throwable = throwable)
    }

    @Throws(Throwable::class)
    fun wtf(
        tag: String,
        message: String,
    ) = wtf(tag, message, null)

    /**
     * Structured logging with key-value pairs for easier parsing.
     *
     * Example:
     * ```kotlin
     * Log.structured(TAG, LogLevel.INFO,
     *     "event" to "wave_detected",
     *     "event_id" to event.id,
     *     "distance_m" to distance
     * )
     * ```
     * Output: `event=wave_detected event_id=abc123 distance_m=50.0`
     *
     * Respects build configuration flags (verbose/debug disabled in release builds).
     */
    fun structured(
        tag: String,
        level: LogLevel,
        vararg pairs: Pair<String, Any?>,
    ) {
        val message =
            pairs.joinToString(" ") { (key, value) ->
                "$key=$value"
            }

        when (level) {
            LogLevel.VERBOSE -> v(tag, message)
            LogLevel.DEBUG -> d(tag, message)
            LogLevel.INFO -> i(tag, message)
            LogLevel.WARNING -> w(tag, message)
            LogLevel.ERROR -> e(tag, message)
        }
    }

    /**
     * Log levels for structured logging.
     */
    enum class LogLevel {
        VERBOSE,
        DEBUG,
        INFO,
        WARNING,
        ERROR,
    }
}
