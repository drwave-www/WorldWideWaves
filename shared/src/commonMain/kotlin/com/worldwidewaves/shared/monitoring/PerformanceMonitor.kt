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

package com.worldwidewaves.shared.monitoring

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource

/**
 * Comprehensive Performance Monitoring System for WorldWideWaves
 *
 * This APM solution provides:
 * - Real-time performance metrics collection
 * - Wave timing accuracy monitoring
 * - UI responsiveness tracking
 * - Memory and network performance
 * - Custom business metrics for wave coordination
 * - Cross-platform performance insights
 */
interface IPerformanceMonitor {

    // Core Metrics
    fun startTrace(name: String): PerformanceTrace
    fun recordMetric(name: String, value: Double, unit: String = "")
    fun recordEvent(name: String, parameters: Map<String, Any> = emptyMap())

    // Wave-Specific Metrics
    fun recordWaveTimingAccuracy(expectedTime: Long, actualTime: Long)
    fun recordWaveParticipation(eventId: String, participationSuccess: Boolean)
    fun recordChoreographyPerformance(sequenceId: String, renderTime: Duration)

    // UI Performance
    fun recordScreenLoad(screenName: String, loadTime: Duration)
    fun recordUserInteraction(action: String, responseTime: Duration)
    fun recordAnimationPerformance(animationName: String, frameDrops: Int)

    // System Metrics
    fun recordMemoryUsage(used: Long, available: Long)
    fun recordNetworkLatency(endpoint: String, latency: Duration)
    fun recordLocationAccuracy(accuracy: Float)

    // Performance Health
    val performanceMetrics: StateFlow<PerformanceMetrics>
    fun getPerformanceReport(): PerformanceReport
}

/**
 * Performance trace for measuring operation duration
 */
interface PerformanceTrace {
    val name: String
    val startTime: TimeSource.Monotonic.ValueTimeMark

    fun addAttribute(key: String, value: String)
    fun addMetric(key: String, value: Long)
    fun stop()
}

/**
 * Real-time performance metrics state
 */
data class PerformanceMetrics(
    val averageWaveTimingAccuracy: Double = 0.0,
    val waveParticipationRate: Double = 0.0,
    val averageScreenLoadTime: Duration = Duration.ZERO,
    val averageNetworkLatency: Duration = Duration.ZERO,
    val memoryUsagePercent: Double = 0.0,
    val locationAccuracy: Float = 0.0f,
    val totalEvents: Long = 0,
    val lastUpdated: Long = 0
)

/**
 * Comprehensive performance report
 */
data class PerformanceReport(
    val appVersion: String,
    val platform: String,
    val deviceInfo: String,
    val reportPeriod: Duration,
    val metrics: PerformanceMetrics,
    val criticalIssues: List<PerformanceIssue>,
    val recommendations: List<String>
)

/**
 * Performance issue detection
 */
data class PerformanceIssue(
    val severity: Severity,
    val category: Category,
    val description: String,
    val impact: String,
    val occurrence: Long
) {
    enum class Severity { LOW, MEDIUM, HIGH, CRITICAL }
    enum class Category { WAVE_TIMING, UI_RESPONSIVENESS, MEMORY, NETWORK, LOCATION }
}

/**
 * Default implementation of performance monitoring
 */
@OptIn(ExperimentalTime::class)
open class PerformanceMonitor : IPerformanceMonitor {

    private val scope = CoroutineScope(Dispatchers.Default)
    private val traces = mutableMapOf<String, PerformanceTraceImpl>()
    private val metrics = mutableMapOf<String, MutableList<Double>>()
    private val events = mutableListOf<PerformanceEvent>()

    private val _performanceMetrics = MutableStateFlow(PerformanceMetrics())
    override val performanceMetrics: StateFlow<PerformanceMetrics> = _performanceMetrics

    override fun startTrace(name: String): PerformanceTrace {
        val trace = PerformanceTraceImpl(name) { finishedTrace ->
            recordMetric("trace_${name}_duration", finishedTrace.duration.inWholeMilliseconds.toDouble(), "ms")
            traces.remove(name)
        }
        traces[name] = trace
        return trace
    }

    override fun recordMetric(name: String, value: Double, unit: String) {
        metrics.getOrPut(name) { mutableListOf() }.add(value)
        updateMetrics()
    }

    @OptIn(ExperimentalTime::class)
    override fun recordEvent(name: String, parameters: Map<String, Any>) {
        events.add(PerformanceEvent(name, parameters, Clock.System.now().toEpochMilliseconds()))
    }

    override fun recordWaveTimingAccuracy(expectedTime: Long, actualTime: Long) {
        val accuracy = 1.0 - (kotlin.math.abs(expectedTime - actualTime).toDouble() / expectedTime)
        recordMetric("wave_timing_accuracy", accuracy * 100, "percent")
        recordEvent("wave_timing", mapOf(
            "expected" to expectedTime,
            "actual" to actualTime,
            "accuracy" to accuracy
        ))
    }

    override fun recordWaveParticipation(eventId: String, participationSuccess: Boolean) {
        recordMetric("wave_participation", if (participationSuccess) 1.0 else 0.0)
        recordEvent("wave_participation", mapOf(
            "eventId" to eventId,
            "success" to participationSuccess
        ))
    }

    override fun recordChoreographyPerformance(sequenceId: String, renderTime: Duration) {
        recordMetric("choreography_render_time", renderTime.inWholeMilliseconds.toDouble(), "ms")
        recordEvent("choreography_performance", mapOf(
            "sequenceId" to sequenceId,
            "renderTime" to renderTime.inWholeMilliseconds
        ))
    }

    override fun recordScreenLoad(screenName: String, loadTime: Duration) {
        recordMetric("screen_load_${screenName}", loadTime.inWholeMilliseconds.toDouble(), "ms")
        recordEvent("screen_load", mapOf(
            "screen" to screenName,
            "loadTime" to loadTime.inWholeMilliseconds
        ))
    }

    override fun recordUserInteraction(action: String, responseTime: Duration) {
        recordMetric("user_interaction_${action}", responseTime.inWholeMilliseconds.toDouble(), "ms")
        recordEvent("user_interaction", mapOf(
            "action" to action,
            "responseTime" to responseTime.inWholeMilliseconds
        ))
    }

    override fun recordAnimationPerformance(animationName: String, frameDrops: Int) {
        recordMetric("animation_frame_drops_${animationName}", frameDrops.toDouble())
        recordEvent("animation_performance", mapOf(
            "animation" to animationName,
            "frameDrops" to frameDrops
        ))
    }

    override fun recordMemoryUsage(used: Long, available: Long) {
        val usagePercent = (used.toDouble() / available) * 100
        recordMetric("memory_usage_percent", usagePercent, "percent")
    }

    override fun recordNetworkLatency(endpoint: String, latency: Duration) {
        recordMetric("network_latency_${endpoint}", latency.inWholeMilliseconds.toDouble(), "ms")
    }

    override fun recordLocationAccuracy(accuracy: Float) {
        recordMetric("location_accuracy", accuracy.toDouble(), "meters")
    }

    override fun getPerformanceReport(): PerformanceReport {
        return PerformanceReport(
            appVersion = "1.0.0", // This should come from build config
            platform = getPlatformName(),
            deviceInfo = getDeviceInfo(),
            reportPeriod = 24.hours,
            metrics = _performanceMetrics.value,
            criticalIssues = detectCriticalIssues(),
            recommendations = generateRecommendations()
        )
    }

    @OptIn(ExperimentalTime::class)
    private fun updateMetrics() {
        scope.launch {
            val current = _performanceMetrics.value
            _performanceMetrics.value = current.copy(
                averageWaveTimingAccuracy = metrics["wave_timing_accuracy"]?.average() ?: 0.0,
                waveParticipationRate = metrics["wave_participation"]?.average() ?: 0.0,
                averageScreenLoadTime = getAverageScreenLoadTime(),
                averageNetworkLatency = getAverageNetworkLatency(),
                memoryUsagePercent = metrics["memory_usage_percent"]?.lastOrNull() ?: 0.0,
                locationAccuracy = metrics["location_accuracy"]?.lastOrNull()?.toFloat() ?: 0.0f,
                totalEvents = events.size.toLong(),
                lastUpdated = Clock.System.now().toEpochMilliseconds()
            )
        }
    }

    private fun getAverageScreenLoadTime(): Duration {
        return metrics.filterKeys { it.startsWith("screen_load_") }
            .values.flatten()
            .average().takeIf { !it.isNaN() }?.toLong()?.milliseconds ?: Duration.ZERO
    }

    private fun getAverageNetworkLatency(): Duration {
        return metrics.filterKeys { it.startsWith("network_latency_") }
            .values.flatten()
            .average().takeIf { !it.isNaN() }?.toLong()?.milliseconds ?: Duration.ZERO
    }

    private fun detectCriticalIssues(): List<PerformanceIssue> {
        val issues = mutableListOf<PerformanceIssue>()

        // Check wave timing accuracy
        val timingAccuracy = metrics["wave_timing_accuracy"]?.average() ?: 100.0
        if (timingAccuracy < 95.0) {
            issues.add(PerformanceIssue(
                severity = PerformanceIssue.Severity.HIGH,
                category = PerformanceIssue.Category.WAVE_TIMING,
                description = "Wave timing accuracy below 95%: ${timingAccuracy}%",
                impact = "Poor user experience and wave coordination",
                occurrence = metrics["wave_timing_accuracy"]?.size?.toLong() ?: 0
            ))
        }

        // Check memory usage
        val memoryUsage = metrics["memory_usage_percent"]?.lastOrNull() ?: 0.0
        if (memoryUsage > 80.0) {
            issues.add(PerformanceIssue(
                severity = PerformanceIssue.Severity.MEDIUM,
                category = PerformanceIssue.Category.MEMORY,
                description = "High memory usage: ${memoryUsage}%",
                impact = "Potential app crashes and poor performance",
                occurrence = 1
            ))
        }

        return issues
    }

    private fun generateRecommendations(): List<String> {
        val recommendations = mutableListOf<String>()

        val timingAccuracy = metrics["wave_timing_accuracy"]?.average() ?: 100.0
        if (timingAccuracy < 95.0) {
            recommendations.add("Optimize wave timing synchronization algorithms")
            recommendations.add("Check network latency and GPS accuracy")
        }

        val avgScreenLoad = getAverageScreenLoadTime()
        if (avgScreenLoad > 2000.milliseconds) {
            recommendations.add("Optimize screen loading performance")
            recommendations.add("Consider lazy loading and caching strategies")
        }

        return recommendations
    }

    private fun getPlatformName(): String = "Unknown" // Platform-specific implementation needed
    private fun getDeviceInfo(): String = "Unknown" // Platform-specific implementation needed
}

/**
 * Performance trace implementation
 */
private class PerformanceTraceImpl(
    override val name: String,
    private val onComplete: (PerformanceTraceImpl) -> Unit
) : PerformanceTrace {

    override val startTime = TimeSource.Monotonic.markNow()
    private val attributes = mutableMapOf<String, String>()
    private val metrics = mutableMapOf<String, Long>()
    private var stopped = false

    val duration: Duration get() = startTime.elapsedNow()

    override fun addAttribute(key: String, value: String) {
        if (!stopped) {
            attributes[key] = value
        }
    }

    override fun addMetric(key: String, value: Long) {
        if (!stopped) {
            metrics[key] = value
        }
    }

    override fun stop() {
        if (!stopped) {
            stopped = true
            onComplete(this)
        }
    }
}

/**
 * Performance event data
 */
private data class PerformanceEvent(
    val name: String,
    val parameters: Map<String, Any>,
    val timestamp: Long
)

