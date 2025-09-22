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

package com.worldwidewaves.monitoring

import android.content.Context
import com.worldwidewaves.shared.monitoring.AndroidPerformanceMonitor

/**
 * Performance monitoring integration for WorldWideWaves
 *
 * Provides a centralized way to access performance monitoring throughout the app.
 * This singleton ensures consistent monitoring across all activities and screens.
 */
object PerformanceIntegration {

    private var _monitor: AndroidPerformanceMonitor? = null

    /**
     * Initialize performance monitoring
     * Call this from Application.onCreate()
     */
    fun initialize(context: Context) {
        if (_monitor == null) {
            _monitor = AndroidPerformanceMonitor(context.applicationContext)
        }
    }

    /**
     * Get the performance monitor instance
     * Throws exception if not initialized
     */
    fun getMonitor(): AndroidPerformanceMonitor {
        return _monitor ?: throw IllegalStateException(
            "PerformanceIntegration not initialized. Call initialize() first."
        )
    }

    /**
     * Check if monitoring is initialized
     */
    fun isInitialized(): Boolean = _monitor != null

    /**
     * Record wave timing for performance tracking
     */
    fun recordWaveTiming(expectedTime: Long, actualTime: Long) {
        _monitor?.recordWaveTimingAccuracy(expectedTime, actualTime)
    }

    /**
     * Record user participation in wave
     */
    fun recordWaveParticipation(eventId: String, success: Boolean) {
        _monitor?.recordWaveParticipation(eventId, success)
    }

    /**
     * Record screen navigation performance
     */
    fun recordScreenLoad(screenName: String, loadTime: kotlin.time.Duration) {
        _monitor?.recordScreenLoad(screenName, loadTime)
    }

    /**
     * Record user interaction response time
     */
    fun recordUserInteraction(action: String, responseTime: kotlin.time.Duration) {
        _monitor?.recordUserInteraction(action, responseTime)
    }
}

/**
 * Extension functions for easy performance tracking
 */

/**
 * Measure and record screen load time
 */
inline fun <T> measureScreenLoad(screenName: String, block: () -> T): T {
    val startTime = System.currentTimeMillis()
    val result = block()
    val duration = (System.currentTimeMillis() - startTime).let(kotlin.time.Duration.Companion::milliseconds)
    PerformanceIntegration.recordScreenLoad(screenName, duration)
    return result
}

/**
 * Measure and record user interaction time
 */
inline fun <T> measureUserInteraction(action: String, block: () -> T): T {
    val startTime = System.currentTimeMillis()
    val result = block()
    val duration = (System.currentTimeMillis() - startTime).let(kotlin.time.Duration.Companion::milliseconds)
    PerformanceIntegration.recordUserInteraction(action, duration)
    return result
}