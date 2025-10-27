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

import kotlinx.datetime.Clock

/**
 * iOS implementation of Firebase Performance Monitoring.
 *
 * Note: Firebase Performance monitoring for iOS is implemented via Swift wrapper
 * because Firebase Performance SDK is not available via Kotlin/Native cinterop.
 * The Swift-side implementation should call Firebase Performance APIs directly.
 *
 * This implementation provides local timing and logging for development purposes.
 * In production, the Swift layer handles actual Firebase Performance integration.
 */
actual object PerformanceTracer {
    actual fun startTrace(name: String): PerformanceTrace {
        Log.v("PerformanceTracer", "Started trace: $name")
        return IOSPerformanceTrace(name, Clock.System.now().toEpochMilliseconds())
    }

    actual fun recordMetric(
        name: String,
        value: Long,
    ) {
        Log.v("PerformanceTracer", "Recorded metric: $name = $value")
    }
}

/**
 * iOS implementation of PerformanceTrace with local timing.
 *
 * Records trace duration and logs metrics for development.
 * Production Firebase Performance integration is handled on Swift side.
 */
private class IOSPerformanceTrace(
    private val name: String,
    private val startTimeMs: Long,
) : PerformanceTrace {
    private val metrics = mutableMapOf<String, Long>()

    override fun putMetric(
        name: String,
        value: Long,
    ) {
        metrics[name] = value
        Log.v("PerformanceTracer", "[$name] Metric: $name = $value")
    }

    override fun incrementMetric(
        name: String,
        by: Long,
    ) {
        metrics[name] = (metrics[name] ?: 0L) + by
        Log.v("PerformanceTracer", "[$name] Incremented metric: $name by $by (now ${metrics[name]})")
    }

    override fun stop() {
        val durationMs = Clock.System.now().toEpochMilliseconds() - startTimeMs
        Log.v("PerformanceTracer", "Stopped trace: $name (duration: ${durationMs}ms)")
        if (metrics.isNotEmpty()) {
            Log.v("PerformanceTracer", "Trace metrics: $metrics")
        }
    }
}
