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

package com.worldwidewaves.shared.utils

/**
 * Platform-specific Firebase Performance Monitoring integration.
 *
 * Provides performance tracing capabilities for critical code paths to monitor
 * app performance in production. Traces are automatically sent to Firebase Console.
 *
 * Usage:
 * ```kotlin
 * val trace = PerformanceTracer.startTrace("operation_name")
 * try {
 *     // Perform operation
 *     trace.putMetric("items_processed", count.toLong())
 * } finally {
 *     trace.stop()
 * }
 * ```
 */
expect object PerformanceTracer {
    /**
     * Start a trace for performance monitoring.
     *
     * @param name Unique identifier for this trace (lowercase_with_underscores recommended)
     * @return PerformanceTrace instance to record metrics and stop trace
     */
    fun startTrace(name: String): PerformanceTrace

    /**
     * Record a custom metric value as a one-off measurement.
     *
     * @param name Metric name (lowercase_with_underscores recommended)
     * @param value Metric value
     */
    fun recordMetric(
        name: String,
        value: Long,
    )
}

/**
 * A performance trace that can record metrics and be stopped.
 *
 * Traces measure duration automatically from start() to stop().
 * Additional metrics can be recorded during the trace lifetime.
 */
interface PerformanceTrace {
    /**
     * Set a custom metric value for this trace.
     *
     * @param name Metric name
     * @param value Metric value
     */
    fun putMetric(
        name: String,
        value: Long,
    )

    /**
     * Increment a custom metric by a specified amount.
     *
     * @param name Metric name
     * @param by Amount to increment (can be negative to decrement)
     */
    fun incrementMetric(
        name: String,
        by: Long,
    )

    /**
     * Stop the trace and send data to Firebase Performance Monitoring.
     * Should be called in a finally block to ensure traces are stopped.
     */
    fun stop()
}
