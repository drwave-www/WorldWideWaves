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

package com.worldwidewaves.shared.testing

import com.worldwidewaves.shared.utils.Log
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

    fun recordMetric(
        name: String,
        value: Double,
        unit: String = "",
    )

    fun recordEvent(
        name: String,
        parameters: Map<String, Any> = emptyMap(),
    )

    // Wave-Specific Metrics
    fun recordWaveTimingAccuracy(
        expectedTime: Long,
        actualTime: Long,
    )

    fun recordWaveParticipation(
        eventId: String,
        participationSuccess: Boolean,
    )

    fun recordChoreographyPerformance(
        sequenceId: String,
        renderTime: Duration,
    )

    // UI Performance
    fun recordScreenLoad(
        screenName: String,
        loadTime: Duration,
    )

    fun recordUserInteraction(
        action: String,
        responseTime: Duration,
    )

    fun recordAnimationPerformance(
        animationName: String,
        frameDrops: Int,
    )

    // System Metrics
    fun recordMemoryUsage(
        used: Long,
        available: Long,
    )

    fun recordNetworkLatency(
        endpoint: String,
        latency: Duration,
    )

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

    fun addAttribute(
        key: String,
        value: String,
    )

    fun addMetric(
        key: String,
        value: Long,
    )

    fun stop()

    fun getDurationMs(): Long

    // Battery and performance monitoring methods
    fun getBatteryUsage(): BatteryUsage

    fun getBackgroundTaskUsage(): BackgroundTaskUsage
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
    val lastUpdated: Long = 0,
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
    val recommendations: List<String>,
)

/**
 * Performance issue detection
 */
data class PerformanceIssue(
    val severity: Severity,
    val category: Category,
    val description: String,
    val impact: String,
    val occurrence: Long,
) {
    enum class Severity { LOW, MEDIUM, HIGH, CRITICAL }

    enum class Category { WAVE_TIMING, UI_RESPONSIVENESS, MEMORY, NETWORK, LOCATION }
}

/**
 * Battery usage metrics
 */
data class BatteryUsage(
    val totalPowerMah: Double,
    val backgroundCpuMs: Long,
    val averageCpuPercent: Double,
)

/**
 * Background task usage metrics
 */
data class BackgroundTaskUsage(
    val nonEssentialTasksLimited: Boolean,
    val essentialTasksMaintained: Boolean,
)

/**
 * Default implementation of performance monitoring
 */
@OptIn(ExperimentalTime::class)
open class PerformanceMonitor : IPerformanceMonitor {
    companion object {
        // Performance thresholds
        private const val MIN_WAVE_TIMING_ACCURACY_PERCENT = 95.0
        private const val MAX_MEMORY_USAGE_PERCENT = 80.0
        private const val PERCENT_MULTIPLIER = 100.0
        private const val MAX_SCREEN_LOAD_TIME_MS = 2000L
        private const val DEFAULT_APP_VERSION = "1.0.0"
        private const val PERFORMANCE_ACCURACY_PERCENT_MULTIPLIER = 100.0

        // Battery usage estimation constants
        const val BATTERY_MAH_PER_MINUTE = 2.5
        const val BACKGROUND_CPU_TIME_PER_MINUTE_MS = 100.0
        const val MAX_CPU_PERCENT = 15.0
        const val CPU_PERCENT_PER_MINUTE = 0.5

        // Memory leak prevention: bounded storage limits
        private const val MAX_METRICS_PER_KEY = 1000 // Keep last 1000 values per metric
        private const val MAX_EVENTS = 500 // Keep last 500 events
        private const val MAX_TRACES = 100 // Maximum concurrent traces

        // Time conversion constants (currently unused but kept for future performance calculations)
        @Suppress("UnusedPrivateProperty") // Reserved for future timing calculations
        private const val MICROSECONDS_PER_SECOND = 1_000_000.0
    }

    private val scope = CoroutineScope(Dispatchers.Default)
    private val traces = mutableMapOf<String, PerformanceTraceImpl>()
    private val metrics = mutableMapOf<String, MutableList<Double>>()
    private val events = mutableListOf<PerformanceEvent>()

    private val _performanceMetrics = MutableStateFlow(PerformanceMetrics())
    override val performanceMetrics: StateFlow<PerformanceMetrics> = _performanceMetrics

    override fun startTrace(name: String): PerformanceTrace {
        // Enforce maximum concurrent traces to prevent memory leaks
        if (traces.size >= MAX_TRACES) {
            // Remove oldest traces if limit exceeded
            traces.entries.firstOrNull()?.let { oldest ->
                oldest.value.stop()
                traces.remove(oldest.key)
            }
        }

        val trace =
            PerformanceTraceImpl(name) { finishedTrace ->
                recordMetric("trace_${name}_duration", finishedTrace.duration.inWholeMilliseconds.toDouble(), "ms")
                traces.remove(name)
            }
        traces[name] = trace
        return trace
    }

    override fun recordMetric(
        name: String,
        value: Double,
        unit: String,
    ) {
        val metricList = metrics.getOrPut(name) { mutableListOf() }
        metricList.add(value)

        // Enforce bounded storage: keep only last MAX_METRICS_PER_KEY values
        if (metricList.size > MAX_METRICS_PER_KEY) {
            metricList.removeAt(0) // Remove oldest value
        }

        updateMetrics()
    }

    @OptIn(ExperimentalTime::class)
    override fun recordEvent(
        name: String,
        parameters: Map<String, Any>,
    ) {
        events.add(PerformanceEvent(name, parameters, Clock.System.now().toEpochMilliseconds()))

        // Enforce bounded storage: keep only last MAX_EVENTS events
        if (events.size > MAX_EVENTS) {
            events.removeAt(0) // Remove oldest event
        }
    }

    override fun recordWaveTimingAccuracy(
        expectedTime: Long,
        actualTime: Long,
    ) {
        val accuracy = 1.0 - (kotlin.math.abs(expectedTime - actualTime).toDouble() / expectedTime)
        recordMetric("wave_timing_accuracy", accuracy * PERFORMANCE_ACCURACY_PERCENT_MULTIPLIER, "percent")
        recordEvent(
            "wave_timing",
            mapOf(
                "expected" to expectedTime,
                "actual" to actualTime,
                "accuracy" to accuracy,
            ),
        )
    }

    override fun recordWaveParticipation(
        eventId: String,
        participationSuccess: Boolean,
    ) {
        recordMetric("wave_participation", if (participationSuccess) 1.0 else 0.0)
        recordEvent(
            "wave_participation",
            mapOf(
                "eventId" to eventId,
                "success" to participationSuccess,
            ),
        )
    }

    override fun recordChoreographyPerformance(
        sequenceId: String,
        renderTime: Duration,
    ) {
        recordMetric("choreography_render_time", renderTime.inWholeMilliseconds.toDouble(), "ms")
        recordEvent(
            "choreography_performance",
            mapOf(
                "sequenceId" to sequenceId,
                "renderTime" to renderTime.inWholeMilliseconds,
            ),
        )
    }

    override fun recordScreenLoad(
        screenName: String,
        loadTime: Duration,
    ) {
        recordMetric("screen_load_$screenName", loadTime.inWholeMilliseconds.toDouble(), "ms")
        recordEvent(
            "screen_load",
            mapOf(
                "screen" to screenName,
                "loadTime" to loadTime.inWholeMilliseconds,
            ),
        )
    }

    override fun recordUserInteraction(
        action: String,
        responseTime: Duration,
    ) {
        recordMetric("user_interaction_$action", responseTime.inWholeMilliseconds.toDouble(), "ms")
        recordEvent(
            "user_interaction",
            mapOf(
                "action" to action,
                "responseTime" to responseTime.inWholeMilliseconds,
            ),
        )
    }

    override fun recordAnimationPerformance(
        animationName: String,
        frameDrops: Int,
    ) {
        recordMetric("animation_frame_drops_$animationName", frameDrops.toDouble())
        recordEvent(
            "animation_performance",
            mapOf(
                "animation" to animationName,
                "frameDrops" to frameDrops,
            ),
        )
    }

    override fun recordMemoryUsage(
        used: Long,
        available: Long,
    ) {
        val usagePercent = (used.toDouble() / available) * PERCENT_MULTIPLIER
        recordMetric("memory_usage_percent", usagePercent, "percent")
    }

    override fun recordNetworkLatency(
        endpoint: String,
        latency: Duration,
    ) {
        recordMetric("network_latency_$endpoint", latency.inWholeMilliseconds.toDouble(), "ms")
    }

    override fun recordLocationAccuracy(accuracy: Float) {
        recordMetric("location_accuracy", accuracy.toDouble(), "meters")
    }

    override fun getPerformanceReport(): PerformanceReport =
        PerformanceReport(
            appVersion = DEFAULT_APP_VERSION, // This should come from build config
            platform = platformName,
            deviceInfo = deviceInfo,
            reportPeriod = 24.hours,
            metrics = _performanceMetrics.value,
            criticalIssues = detectCriticalIssues(),
            recommendations = generateRecommendations(),
        )

    @OptIn(ExperimentalTime::class)
    private fun updateMetrics() {
        scope.launch {
            try {
                val current = _performanceMetrics.value
                _performanceMetrics.value =
                    current.copy(
                        averageWaveTimingAccuracy = metrics["wave_timing_accuracy"]?.average() ?: 0.0,
                        waveParticipationRate = metrics["wave_participation"]?.average() ?: 0.0,
                        averageScreenLoadTime = getAverageScreenLoadTime(),
                        averageNetworkLatency = getAverageNetworkLatency(),
                        memoryUsagePercent = metrics["memory_usage_percent"]?.lastOrNull() ?: 0.0,
                        locationAccuracy = metrics["location_accuracy"]?.lastOrNull()?.toFloat() ?: 0.0f,
                        totalEvents = events.size.toLong(),
                        lastUpdated = Clock.System.now().toEpochMilliseconds(),
                    )
            } catch (e: Exception) {
                // Log error but don't crash performance monitoring
                Log.e("WWW.Performance.Monitor", "Error updating metrics", e)
            }
        }
    }

    private fun getAverageScreenLoadTime(): Duration =
        metrics
            .filterKeys { it.startsWith("screen_load_") }
            .values
            .flatten()
            .average()
            .takeIf { !it.isNaN() }
            ?.toLong()
            ?.milliseconds ?: Duration.ZERO

    private fun getAverageNetworkLatency(): Duration =
        metrics
            .filterKeys { it.startsWith("network_latency_") }
            .values
            .flatten()
            .average()
            .takeIf { !it.isNaN() }
            ?.toLong()
            ?.milliseconds ?: Duration.ZERO

    private fun detectCriticalIssues(): List<PerformanceIssue> {
        val issues = mutableListOf<PerformanceIssue>()

        // Check wave timing accuracy
        val timingAccuracy = metrics["wave_timing_accuracy"]?.average() ?: PERCENT_MULTIPLIER
        if (timingAccuracy < MIN_WAVE_TIMING_ACCURACY_PERCENT) {
            issues.add(
                PerformanceIssue(
                    severity = PerformanceIssue.Severity.HIGH,
                    category = PerformanceIssue.Category.WAVE_TIMING,
                    description = "Wave timing accuracy below ${MIN_WAVE_TIMING_ACCURACY_PERCENT}%: $timingAccuracy%",
                    impact = "Poor user experience and wave coordination",
                    occurrence = metrics["wave_timing_accuracy"]?.size?.toLong() ?: 0,
                ),
            )
        }

        // Check memory usage
        val memoryUsage = metrics["memory_usage_percent"]?.lastOrNull() ?: 0.0
        if (memoryUsage > MAX_MEMORY_USAGE_PERCENT) {
            issues.add(
                PerformanceIssue(
                    severity = PerformanceIssue.Severity.MEDIUM,
                    category = PerformanceIssue.Category.MEMORY,
                    description = "High memory usage: $memoryUsage%",
                    impact = "Potential app crashes and poor performance",
                    occurrence = 1,
                ),
            )
        }

        return issues
    }

    private fun generateRecommendations(): List<String> {
        val recommendations = mutableListOf<String>()

        val timingAccuracy = metrics["wave_timing_accuracy"]?.average() ?: PERCENT_MULTIPLIER
        if (timingAccuracy < MIN_WAVE_TIMING_ACCURACY_PERCENT) {
            recommendations.add("Optimize wave timing synchronization algorithms")
            recommendations.add("Check network latency and GPS accuracy")
        }

        val avgScreenLoad = getAverageScreenLoadTime()
        if (avgScreenLoad > MAX_SCREEN_LOAD_TIME_MS.milliseconds) {
            recommendations.add("Optimize screen loading performance")
            recommendations.add("Consider lazy loading and caching strategies")
        }

        return recommendations
    }

    /**
     * Cleanup method to clear all performance data and cancel ongoing traces.
     * Useful for preventing memory leaks in long-running applications.
     */
    fun cleanup() {
        // Stop and clear all active traces
        // Create a copy of traces to avoid ConcurrentModificationException
        // since stop() modifies the traces map via onComplete callback
        traces.values.toList().forEach { it.stop() }
        traces.clear()

        // Clear metrics and events
        metrics.clear()
        events.clear()

        // Reset performance metrics
        _performanceMetrics.value = PerformanceMetrics()
    }

    /**
     * Get current memory usage statistics for monitoring.
     */
    fun getMemoryStats(): MemoryStats =
        MemoryStats(
            tracesCount = traces.size,
            metricsCount = metrics.size,
            totalMetricValues = metrics.values.sumOf { it.size },
            eventsCount = events.size,
        )

    private val platformName: String = "Unknown" // Platform-specific implementation needed

    private val deviceInfo: String = "Unknown" // Platform-specific implementation needed
}

/**
 * Memory statistics for PerformanceMonitor
 */
data class MemoryStats(
    val tracesCount: Int,
    val metricsCount: Int,
    val totalMetricValues: Int,
    val eventsCount: Int,
)

/**
 * Performance trace implementation
 */
private class PerformanceTraceImpl(
    override val name: String,
    private val onComplete: (PerformanceTraceImpl) -> Unit,
) : PerformanceTrace {
    override val startTime = TimeSource.Monotonic.markNow()
    private val attributes = mutableMapOf<String, String>()
    private val metrics = mutableMapOf<String, Long>()
    private var stopped = false

    val duration: Duration get() = startTime.elapsedNow()

    override fun addAttribute(
        key: String,
        value: String,
    ) {
        if (!stopped) {
            attributes[key] = value
        }
    }

    override fun addMetric(
        key: String,
        value: Long,
    ) {
        if (!stopped) {
            metrics[key] = value
        }
    }

    override fun getDurationMs(): Long = duration.inWholeMilliseconds

    override fun getBatteryUsage(): BatteryUsage {
        // In real implementation, would collect actual battery usage metrics
        val durationMinutes = duration.inWholeMinutes.toDouble()
        return BatteryUsage(
            totalPowerMah = durationMinutes * PerformanceMonitor.BATTERY_MAH_PER_MINUTE, // Estimate: 2.5mAh per minute
            backgroundCpuMs =
                metrics["background_cpu_time"] ?: (durationMinutes * PerformanceMonitor.BACKGROUND_CPU_TIME_PER_MINUTE_MS).toLong(),
            averageCpuPercent =
                kotlin.math.min(
                    PerformanceMonitor.MAX_CPU_PERCENT,
                    durationMinutes * PerformanceMonitor.CPU_PERCENT_PER_MINUTE,
                ),
            // Max 15% CPU
        )
    }

    override fun getBackgroundTaskUsage(): BackgroundTaskUsage =
        BackgroundTaskUsage(
            nonEssentialTasksLimited = true,
            essentialTasksMaintained = true,
        )

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
    val timestamp: Long,
)
