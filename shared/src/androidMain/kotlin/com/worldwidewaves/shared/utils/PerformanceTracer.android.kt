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

import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace

/**
 * Android implementation of Firebase Performance Monitoring.
 *
 * Uses Firebase Performance SDK to track custom traces and metrics.
 */
actual object PerformanceTracer {
    private val firebasePerf = FirebasePerformance.getInstance()

    actual fun startTrace(name: String): PerformanceTrace {
        val trace = firebasePerf.newTrace(name)
        trace.start()
        Log.v("PerformanceTracer", "Started trace: $name")
        return AndroidPerformanceTrace(trace)
    }

    actual fun recordMetric(
        name: String,
        value: Long,
    ) {
        Log.v("PerformanceTracer", "Recorded metric: $name = $value")
        // Firebase Performance doesn't have a direct "record metric" API
        // We create a short-lived trace instead
        val trace = firebasePerf.newTrace(name)
        trace.start()
        trace.putMetric("value", value)
        trace.stop()
    }
}

/**
 * Android implementation of PerformanceTrace wrapping Firebase Trace.
 */
private class AndroidPerformanceTrace(
    private val trace: Trace,
) : PerformanceTrace {
    override fun putMetric(
        name: String,
        value: Long,
    ) {
        trace.putMetric(name, value)
    }

    override fun incrementMetric(
        name: String,
        by: Long,
    ) {
        trace.incrementMetric(name, by)
    }

    override fun stop() {
        trace.stop()
        Log.v("PerformanceTracer", "Stopped trace: ${trace.name}")
    }
}
