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

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

/**
 * iOS implementation of PerformanceTracer.
 *
 * DISABLED: Performance tracing is disabled in production to avoid costs.
 * All traces are no-ops. Local timing can be enabled via ENABLE_TRACING flag.
 */
actual object PerformanceTracer {
    // Performance tracing disabled - all operations are no-ops
    private const val ENABLE_TRACING = false // Set to true only for local debugging

    /**
     * Start a performance trace (no-op by default).
     */
    actual fun startTrace(name: String): PerformanceTrace =
        if (ENABLE_TRACING) {
            val startTime = (NSDate().timeIntervalSince1970() * 1000).toLong()
            LocalPerformanceTrace(name, startTime)
        } else {
            NoOpPerformanceTrace()
        }

    /**
     * Record a custom metric (no-op).
     */
    actual fun recordMetric(
        name: String,
        value: Long,
    ) {
        // No-op - tracing disabled
    }
}

/**
 * Local performance trace for debugging (no Firebase, no cost).
 * Logs timing information to console only.
 */
private class LocalPerformanceTrace(
    private val name: String,
    private val startTimeMs: Long,
) : PerformanceTrace {
    private val metrics = mutableMapOf<String, Long>()

    override fun putMetric(
        name: String,
        value: Long,
    ) {
        metrics[name] = value
    }

    override fun incrementMetric(
        name: String,
        by: Long,
    ) {
        val current = metrics[name] ?: 0L
        metrics[name] = current + by
    }

    override fun stop() {
        val duration = (NSDate().timeIntervalSince1970() * 1000).toLong() - startTimeMs
        val metricsStr = metrics.entries.joinToString(", ") { "${it.key}=${it.value}" }
        Log.d("WWW.Perf", "trace=$name duration_ms=$duration metrics=[$metricsStr]")
    }
}

/**
 * No-op implementation for production (zero overhead, no costs).
 */
private class NoOpPerformanceTrace : PerformanceTrace {
    override fun putMetric(
        name: String,
        value: Long,
    ) {
        // No-op - tracing disabled
    }

    override fun incrementMetric(
        name: String,
        by: Long,
    ) {
        // No-op - tracing disabled
    }

    override fun stop() {
        // No-op - tracing disabled
    }
}
