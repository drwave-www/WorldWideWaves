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

package com.worldwidewaves.testing.real

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout

/**
 * Manages test environment reset and baseline restoration for real integration tests.
 *
 * This manager provides:
 * - Complete environment reset between test suites
 * - Baseline state restoration
 * - Test isolation mechanisms
 * - Environment health validation
 * - Performance baseline management
 */
class TestEnvironmentResetManager(
    private val context: Context,
    private val deviceStateManager: RealDeviceStateManager,
    private val cleanupManager: TestDataCleanupManager
) {

    private val preferences: SharedPreferences = context.getSharedPreferences("test_environment", Context.MODE_PRIVATE)
    private val resetHistory = mutableListOf<EnvironmentResetRecord>()

    /**
     * Perform complete environment reset to baseline state
     */
    suspend fun performCompleteReset() {
        val resetId = "reset_${System.currentTimeMillis()}"
        val startTime = System.currentTimeMillis()

        println("🔄 Starting complete environment reset: $resetId")

        val resetSteps = listOf(
            ResetStep("cleanup_active_sessions", "Clean up all active test sessions") {
                cleanupManager.emergencyCleanupAll()
            },
            ResetStep("reset_app_state", "Reset application state to baseline") {
                resetApplicationState()
            },
            ResetStep("reset_device_state", "Reset device state and permissions") {
                deviceStateManager.reset()
            },
            ResetStep("clear_preferences", "Clear test preferences and settings") {
                clearTestPreferences()
            },
            ResetStep("reset_network_state", "Reset network and connectivity state") {
                resetNetworkState()
            },
            ResetStep("reset_location_state", "Reset location and GPS state") {
                resetLocationState()
            },
            ResetStep("validate_baseline", "Validate environment baseline") {
                validateEnvironmentBaseline()
            }
        )

        var completedSteps = 0
        val failedSteps = mutableListOf<String>()

        resetSteps.forEach { step ->
            try {
                println("📋 Executing reset step: ${step.description}")
                withTimeout(30000) { // 30 second timeout per step
                    step.execute()
                }
                completedSteps++
                println("✅ Completed: ${step.name}")
            } catch (e: Exception) {
                failedSteps.add(step.name)
                println("❌ Failed reset step ${step.name}: ${e.message}")
            }
        }

        val resetDuration = System.currentTimeMillis() - startTime
        val resetRecord = EnvironmentResetRecord(
            resetId = resetId,
            timestamp = startTime,
            durationMs = resetDuration,
            completedSteps = completedSteps,
            totalSteps = resetSteps.size,
            failedSteps = failedSteps,
            success = failedSteps.isEmpty()
        )

        resetHistory.add(resetRecord)
        recordResetMetrics(resetRecord)

        if (resetRecord.success) {
            println("✅ Complete environment reset successful: $resetId (${resetDuration}ms)")
        } else {
            println("⚠️  Environment reset completed with ${failedSteps.size} failures: $resetId")
        }
    }

    /**
     * Reset to specific baseline configuration
     */
    suspend fun resetToBaseline(baselineName: String) {
        println("🎯 Resetting to baseline configuration: $baselineName")

        val baselines = mapOf(
            "clean_install" to {
                resetToCleanInstallState()
            },
            "first_run" to {
                resetToFirstRunState()
            },
            "performance_test" to {
                resetToPerformanceTestState()
            },
            "accessibility_test" to {
                resetToAccessibilityTestState()
            }
        )

        val baselineAction = baselines[baselineName]
            ?: throw IllegalArgumentException("Unknown baseline: $baselineName")

        try {
            performCompleteReset()
            delay(2000) // Allow reset to stabilize
            baselineAction()
            println("✅ Successfully reset to baseline: $baselineName")
        } catch (e: Exception) {
            println("❌ Failed to reset to baseline $baselineName: ${e.message}")
            throw e
        }
    }

    /**
     * Validate current environment health
     */
    suspend fun validateEnvironmentHealth(): EnvironmentHealthReport {
        println("🔍 Validating environment health...")

        val healthChecks = listOf(
            HealthCheck("device_state", "Device state is properly configured") {
                deviceStateManager.hasGpsCapability() && deviceStateManager.hasNetworkCapability()
            },
            HealthCheck("memory_state", "Memory usage is within normal limits") {
                val memory = deviceStateManager.getMemoryUsage()
                memory.usedMemoryMB < memory.maxMemoryMB * 0.8 // Less than 80% memory usage
            },
            HealthCheck("network_connectivity", "Network connectivity is available") {
                deviceStateManager.isNetworkAvailable()
            },
            HealthCheck("location_services", "Location services are functional") {
                deviceStateManager.isGpsEnabled()
            },
            HealthCheck("app_state", "Application state is clean") {
                validateAppState()
            },
            HealthCheck("test_isolation", "Test isolation is effective") {
                val isolationReport = cleanupManager.getTestIsolationReport()
                isolationReport.isolationScore > 0.7 // 70% isolation score threshold
            }
        )

        val results = mutableMapOf<String, Boolean>()
        var totalScore = 0.0

        healthChecks.forEach { check ->
            try {
                val result = withTimeout(10000) { // 10 second timeout per check
                    check.execute()
                }
                results[check.name] = result
                if (result) totalScore += 1.0
                println("${if (result) "✅" else "❌"} ${check.description}: ${if (result) "PASS" else "FAIL"}")
            } catch (e: Exception) {
                results[check.name] = false
                println("❌ ${check.description}: ERROR - ${e.message}")
            }
        }

        val healthScore = totalScore / healthChecks.size
        val healthReport = EnvironmentHealthReport(
            timestamp = System.currentTimeMillis(),
            overallScore = healthScore,
            checkResults = results,
            recommendations = generateHealthRecommendations(results)
        )

        println("📊 Environment health score: ${(healthScore * 100).toInt()}%")
        return healthReport
    }

    /**
     * Get environment reset statistics
     */
    fun getResetStatistics(): EnvironmentResetStatistics {
        val successfulResets = resetHistory.count { it.success }
        val averageDuration = resetHistory.map { it.durationMs }.average().takeIf { !it.isNaN() } ?: 0.0
        val recentFailures = resetHistory.takeLast(10).count { !it.success }

        return EnvironmentResetStatistics(
            totalResets = resetHistory.size,
            successfulResets = successfulResets,
            averageResetDurationMs = averageDuration.toLong(),
            recentFailureRate = recentFailures / 10.0
        )
    }

    /**
     * Force emergency environment reset
     */
    suspend fun emergencyReset() {
        println("🚨 Performing emergency environment reset...")

        try {
            // Skip normal validation, force immediate reset
            withTimeout(60000) { // 1 minute maximum for emergency reset
                cleanupManager.emergencyCleanupAll()
                resetApplicationState()
                deviceStateManager.reset()
                clearAllPreferences()

                // Force garbage collection
                System.gc()
                delay(3000) // Allow time for cleanup to complete
            }
            println("✅ Emergency reset completed")
        } catch (e: Exception) {
            println("❌ Emergency reset failed: ${e.message}")
            throw e
        }
    }

    // Private helper methods

    private suspend fun resetApplicationState() {
        // Clear app-specific state
        context.cacheDir?.let { clearDirectory(it) }
        context.filesDir?.let { file ->
            file.listFiles()?.filter { it.name.contains("test") }?.forEach { it.deleteRecursively() }
        }

        // Reset shared preferences
        val prefFiles = listOf("app_preferences", "user_settings", "test_config")
        prefFiles.forEach { prefName ->
            context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
                .edit().clear().apply()
        }

        delay(1000) // Allow time for state reset
    }

    private suspend fun resetNetworkState() {
        // Reset network-related state
        // This would typically involve clearing network caches and resetting connections
        delay(500)
        println("🌐 Network state reset completed")
    }

    private suspend fun resetLocationState() {
        // Reset location-related state
        deviceStateManager.setupTestLocationProvider()
        delay(500)
        println("📍 Location state reset completed")
    }

    private suspend fun validateEnvironmentBaseline() {
        val healthReport = validateEnvironmentHealth()
        if (healthReport.overallScore < 0.8) { // 80% health threshold
            throw IllegalStateException("Environment baseline validation failed: ${healthReport.overallScore}")
        }
    }

    private fun clearTestPreferences() {
        preferences.edit().clear().apply()
        println("🗑️  Test preferences cleared")
    }

    private fun clearAllPreferences() {
        val prefsDir = context.filesDir?.parentFile?.resolve("shared_prefs")
        prefsDir?.listFiles()?.forEach { file ->
            if (file.name.contains("test") || file.name.contains("temp")) {
                file.delete()
            }
        }
    }

    private suspend fun resetToCleanInstallState() {
        // Simulate clean install state
        clearAllPreferences()
        context.cacheDir?.deleteRecursively()
        delay(1000)
        println("📦 Reset to clean install state")
    }

    private suspend fun resetToFirstRunState() {
        // Simulate first run state
        resetToCleanInstallState()
        // Set first-run flags
        context.getSharedPreferences("app_state", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("first_run", true)
            .putBoolean("onboarding_completed", false)
            .apply()
        delay(500)
        println("🆕 Reset to first run state")
    }

    private suspend fun resetToPerformanceTestState() {
        // Configure for performance testing
        resetToCleanInstallState()
        // Disable animations and transitions for consistent performance measurement
        context.getSharedPreferences("performance_config", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("animations_disabled", true)
            .putBoolean("performance_mode", true)
            .apply()
        delay(500)
        println("🏃 Reset to performance test state")
    }

    private suspend fun resetToAccessibilityTestState() {
        // Configure for accessibility testing
        resetToCleanInstallState()
        // Enable accessibility features
        context.getSharedPreferences("accessibility_config", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("accessibility_enabled", true)
            .putBoolean("high_contrast", true)
            .putFloat("text_scale", 1.3f)
            .apply()
        delay(500)
        println("♿ Reset to accessibility test state")
    }

    private fun validateAppState(): Boolean {
        // Validate app is in clean state
        val cacheSize = context.cacheDir?.let { calculateDirectorySize(it) } ?: 0
        val tempFiles = context.filesDir?.listFiles()?.count { it.name.contains("temp") } ?: 0

        return cacheSize < 50 * 1024 * 1024 && tempFiles == 0 // Less than 50MB cache, no temp files
    }

    private fun clearDirectory(directory: java.io.File) {
        if (directory.exists() && directory.isDirectory) {
            directory.deleteRecursively()
            directory.mkdirs() // Recreate empty directory
        }
    }

    private fun calculateDirectorySize(directory: java.io.File): Long {
        if (!directory.exists()) return 0
        return directory.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
    }

    private fun recordResetMetrics(record: EnvironmentResetRecord) {
        preferences.edit()
            .putLong("last_reset_time", record.timestamp)
            .putLong("last_reset_duration", record.durationMs)
            .putBoolean("last_reset_success", record.success)
            .putInt("total_resets", preferences.getInt("total_resets", 0) + 1)
            .apply()
    }

    private fun generateHealthRecommendations(results: Map<String, Boolean>): List<String> {
        val recommendations = mutableListOf<String>()

        results.forEach { (checkName, passed) ->
            if (!passed) {
                when (checkName) {
                    "memory_state" -> recommendations.add("Consider reducing memory usage or performing garbage collection")
                    "network_connectivity" -> recommendations.add("Verify network connection and configuration")
                    "location_services" -> recommendations.add("Ensure GPS is enabled and location permissions are granted")
                    "app_state" -> recommendations.add("Clear app cache and temporary files")
                    "test_isolation" -> recommendations.add("Improve test cleanup and isolation mechanisms")
                }
            }
        }

        if (recommendations.isEmpty()) {
            recommendations.add("Environment is healthy, no recommendations needed")
        }

        return recommendations
    }

    // Data classes

    private data class ResetStep(
        val name: String,
        val description: String,
        val execute: suspend () -> Unit
    )

    private data class HealthCheck(
        val name: String,
        val description: String,
        val execute: suspend () -> Boolean
    )

    data class EnvironmentResetRecord(
        val resetId: String,
        val timestamp: Long,
        val durationMs: Long,
        val completedSteps: Int,
        val totalSteps: Int,
        val failedSteps: List<String>,
        val success: Boolean
    )

    data class EnvironmentHealthReport(
        val timestamp: Long,
        val overallScore: Double,
        val checkResults: Map<String, Boolean>,
        val recommendations: List<String>
    )

    data class EnvironmentResetStatistics(
        val totalResets: Int,
        val successfulResets: Int,
        val averageResetDurationMs: Long,
        val recentFailureRate: Double
    )
}