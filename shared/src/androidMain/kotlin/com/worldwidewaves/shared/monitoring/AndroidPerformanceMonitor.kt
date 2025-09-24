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

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Android-specific Performance Monitor implementation
 *
 * Extends the base performance monitor with Android-specific capabilities:
 * - Memory monitoring using ActivityManager
 * - Device information collection
 * - Frame metrics integration
 * - Compose performance tracking
 * - System resource monitoring
 */
class AndroidPerformanceMonitor(
    private val context: Context,
) : PerformanceMonitor() {
    companion object {
        private const val TAG = "AndroidPerformanceMonitor"
        internal const val SYSTEM_MONITOR_INTERVAL_SECONDS = 30L
        internal const val MEMORY_MONITOR_INTERVAL_SECONDS = 10L
        internal const val JANKY_FRAME_THRESHOLD_PERCENT = 100.0
    }

    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val monitoringScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    init {
        startSystemMonitoring()
    }

    /**
     * Start continuous system monitoring
     */
    private fun startSystemMonitoring() {
        monitoringScope.launch {
            while (isActive) {
                try {
                    monitorMemoryUsage()
                    delay(SYSTEM_MONITOR_INTERVAL_SECONDS.seconds)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in system monitoring", e)
                }
            }
        }
    }

    /**
     * Monitor Android memory usage
     */
    private fun monitorMemoryUsage() {
        try {
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)

            val totalMemory = memoryInfo.totalMem
            val availableMemory = memoryInfo.availMem
            val usedMemory = totalMemory - availableMemory

            recordMemoryUsage(usedMemory, totalMemory)

            // Record low memory threshold
            if (memoryInfo.lowMemory) {
                recordEvent(
                    "low_memory_warning",
                    mapOf(
                        "threshold" to memoryInfo.threshold,
                        "available" to availableMemory,
                        "total" to totalMemory,
                    ),
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error monitoring memory", e)
        }
    }

    /**
     * Get detailed Android device information
     */
    fun getAndroidDeviceInfo(): String =
        buildString {
            append("Android ${Build.VERSION.RELEASE} ")
            append("(API ${Build.VERSION.SDK_INT}) ")
            append("${Build.MANUFACTURER} ${Build.MODEL} ")
            append("ABI: ${Build.SUPPORTED_ABIS.joinToString(",")}")
        }

    /**
     * Monitor app startup performance
     */
    fun recordAppStartup(
        startType: StartupType,
        duration: kotlin.time.Duration,
    ) {
        recordMetric("app_startup_${startType.name.lowercase()}", duration.inWholeMilliseconds.toDouble(), "ms")
        recordEvent(
            "app_startup",
            mapOf(
                "type" to startType.name,
                "duration" to duration.inWholeMilliseconds,
                "deviceInfo" to getAndroidDeviceInfo(),
            ),
        )
    }

    /**
     * Monitor Compose recomposition performance
     */
    fun recordRecomposition(
        composableName: String,
        recompositions: Int,
        skipped: Int,
    ) {
        recordMetric("compose_recompositions_$composableName", recompositions.toDouble())
        recordMetric("compose_skipped_$composableName", skipped.toDouble())
        recordEvent(
            "compose_performance",
            mapOf(
                "composable" to composableName,
                "recompositions" to recompositions,
                "skipped" to skipped,
                "efficiency" to if (recompositions + skipped > 0) skipped.toDouble() / (recompositions + skipped) else 1.0,
            ),
        )
    }

    /**
     * Monitor frame rendering performance
     */
    fun recordFrameMetrics(
        totalFrames: Long,
        jankyFrames: Long,
    ) {
        val jankyPercent = if (totalFrames > 0) (jankyFrames.toDouble() / totalFrames) * JANKY_FRAME_THRESHOLD_PERCENT else 0.0
        recordMetric("frame_jank_percent", jankyPercent, "percent")
        recordEvent(
            "frame_performance",
            mapOf(
                "totalFrames" to totalFrames,
                "jankyFrames" to jankyFrames,
                "jankyPercent" to jankyPercent,
            ),
        )
    }

    /**
     * App startup types for performance tracking
     */
    enum class StartupType {
        COLD, // App process is created
        WARM, // App process exists but activity needs to be created
        HOT, // App and activity exist, just brought to foreground
    }

    /**
     * Cleanup monitoring resources
     */
    fun cleanup() {
        monitoringScope.cancel()
    }
}

/**
 * Compose integration for performance monitoring
 */
@Composable
fun PerformanceMonitoringEffect(
    monitor: AndroidPerformanceMonitor,
    screenName: String,
) {
    val context = LocalContext.current
    val startTime = remember { System.currentTimeMillis() }

    // Track screen load time
    LaunchedEffect(screenName) {
        val loadTime = (System.currentTimeMillis() - startTime).milliseconds
        monitor.recordScreenLoad(screenName, loadTime)
    }

    // Monitor memory during screen lifecycle
    DisposableEffect(screenName) {
        val monitoringScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        val job =
            monitoringScope.launch {
                while (isActive) {
                    try {
                        val runtime = Runtime.getRuntime()
                        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
                        val maxMemory = runtime.maxMemory()
                        monitor.recordMemoryUsage(usedMemory, maxMemory)
                        delay(AndroidPerformanceMonitor.MEMORY_MONITOR_INTERVAL_SECONDS.seconds)
                    } catch (e: Exception) {
                        Log.e("PerformanceMonitoringEffect", "Error monitoring screen memory", e)
                    }
                }
            }

        onDispose {
            monitoringScope.cancel()
        }
    }
}

/**
 * Performance monitoring for Compose UI interactions
 */
@Composable
fun rememberPerformanceMonitor(): AndroidPerformanceMonitor {
    val context = LocalContext.current
    return remember {
        AndroidPerformanceMonitor(context)
    }
}

/**
 * Utility for measuring Compose performance
 */
class ComposePerformanceTracker {
    private val recompositionCounts = mutableMapOf<String, Int>()
    private val skipCounts = mutableMapOf<String, Int>()

    fun trackRecomposition(
        composableName: String,
        skipped: Boolean = false,
    ) {
        if (skipped) {
            skipCounts[composableName] = (skipCounts[composableName] ?: 0) + 1
        } else {
            recompositionCounts[composableName] = (recompositionCounts[composableName] ?: 0) + 1
        }
    }

    fun getStats(composableName: String): Pair<Int, Int> = (recompositionCounts[composableName] ?: 0) to (skipCounts[composableName] ?: 0)

    fun reportStats(monitor: AndroidPerformanceMonitor) {
        recompositionCounts.forEach { (name, recompositions) ->
            val skipped = skipCounts[name] ?: 0
            monitor.recordRecomposition(name, recompositions, skipped)
        }
    }
}
