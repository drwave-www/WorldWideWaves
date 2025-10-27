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
 * Uses local timing measurements. Firebase Performance Monitoring integration
 * requires Firebase/Performance CocoaPod configuration in the iOS project.
 *
 * To enable Firebase Performance on iOS:
 * 1. Add Firebase/Performance pod to iosApp/Podfile
 * 2. Run pod install
 * 3. Import cocoapods.FirebasePerformance
 * 4. Replace this stub with Firebase trace implementation
 */
actual object PerformanceTracer {
    /**
     * Start a performance trace (local timing for iOS).
     */
    actual fun startTrace(name: String): PerformanceTrace {
        val startTime = (NSDate().timeIntervalSince1970() * 1000).toLong()
        return LocalPerformanceTrace(name, startTime)
    }

    /**
     * Record a custom metric (local logging only).
     */
    actual fun recordMetric(
        name: String,
        value: Long,
    ) {
        Log.performance("WWW.Perf", "$name=$value")
    }
}

/**
 * Local performance trace implementation for iOS.
 * Logs timing information but doesn't send to Firebase (requires CocoaPod).
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
        Log.performance("WWW.Perf", "trace=$name duration_ms=$duration metrics=[$metricsStr]")
    }
}
