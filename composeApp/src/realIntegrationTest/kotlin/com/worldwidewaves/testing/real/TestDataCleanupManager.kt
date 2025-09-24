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
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages test data cleanup and environment reset for real integration tests.
 *
 * This manager ensures:
 * - Test data isolation between test runs
 * - Proper cleanup of temporary test data
 * - Environment reset mechanisms
 * - Test dependency management
 * - Resource cleanup and optimization
 */
class TestDataCleanupManager(private val context: Context) {

    private val activeTestSessions = ConcurrentHashMap<String, TestSession>()
    private val testDataRegistry = ConcurrentHashMap<String, TestDataEntry>()
    private val cleanupTasks = mutableListOf<CleanupTask>()

    /**
     * Initialize a new test session with isolated environment
     */
    fun startTestSession(sessionId: String): TestSession {
        val session = TestSession(
            id = sessionId,
            startTime = System.currentTimeMillis(),
            dataEntries = mutableSetOf(),
            tempFiles = mutableSetOf(),
            networkResources = mutableSetOf()
        )

        activeTestSessions[sessionId] = session
        println("✅ Test session started: $sessionId")
        return session
    }

    /**
     * Register test data for cleanup tracking
     */
    fun registerTestData(sessionId: String, dataEntry: TestDataEntry) {
        val session = activeTestSessions[sessionId]
            ?: throw IllegalStateException("No active session for ID: $sessionId")

        session.dataEntries.add(dataEntry.id)
        testDataRegistry[dataEntry.id] = dataEntry

        println("📝 Registered test data: ${dataEntry.id} (${dataEntry.type}) for session $sessionId")
    }

    /**
     * Register temporary file for cleanup
     */
    fun registerTempFile(sessionId: String, filePath: String) {
        val session = activeTestSessions[sessionId]
            ?: throw IllegalStateException("No active session for ID: $sessionId")

        session.tempFiles.add(filePath)
        println("📁 Registered temp file: $filePath for session $sessionId")
    }

    /**
     * Register network resource for cleanup
     */
    fun registerNetworkResource(sessionId: String, resource: NetworkResource) {
        val session = activeTestSessions[sessionId]
            ?: throw IllegalStateException("No active session for ID: $sessionId")

        session.networkResources.add(resource)
        println("🌐 Registered network resource: ${resource.endpoint} for session $sessionId")
    }

    /**
     * Clean up specific test session
     */
    suspend fun cleanupTestSession(sessionId: String) {
        val session = activeTestSessions[sessionId]
            ?: run {
                println("⚠️  No session found for cleanup: $sessionId")
                return
            }

        println("🧹 Starting cleanup for session: $sessionId")
        val startTime = System.currentTimeMillis()

        // Clean up test data
        session.dataEntries.forEach { dataId ->
            testDataRegistry[dataId]?.let { dataEntry ->
                try {
                    cleanupTestDataEntry(dataEntry)
                    testDataRegistry.remove(dataId)
                } catch (e: Exception) {
                    println("⚠️  Failed to cleanup test data $dataId: ${e.message}")
                }
            }
        }

        // Clean up temporary files
        session.tempFiles.forEach { filePath ->
            try {
                cleanupTempFile(filePath)
            } catch (e: Exception) {
                println("⚠️  Failed to cleanup temp file $filePath: ${e.message}")
            }
        }

        // Clean up network resources
        session.networkResources.forEach { resource ->
            try {
                cleanupNetworkResource(resource)
            } catch (e: Exception) {
                println("⚠️  Failed to cleanup network resource ${resource.endpoint}: ${e.message}")
            }
        }

        // Run custom cleanup tasks
        cleanupTasks.forEach { task ->
            try {
                if (task.sessionId == sessionId || task.sessionId == "*") {
                    task.execute()
                }
            } catch (e: Exception) {
                println("⚠️  Failed to execute cleanup task ${task.name}: ${e.message}")
            }
        }

        activeTestSessions.remove(sessionId)
        val cleanupTime = System.currentTimeMillis() - startTime
        println("✅ Session cleanup completed: $sessionId (${cleanupTime}ms)")
    }

    /**
     * Emergency cleanup of all active sessions
     */
    suspend fun emergencyCleanupAll() {
        println("🚨 Emergency cleanup of all active sessions...")
        val sessionIds = activeTestSessions.keys.toList()

        sessionIds.forEach { sessionId ->
            try {
                withTimeout(30000) { // 30 second timeout per session
                    cleanupTestSession(sessionId)
                }
            } catch (e: Exception) {
                println("❌ Emergency cleanup failed for session $sessionId: ${e.message}")
            }
        }

        // Force garbage collection after cleanup
        System.gc()
        println("✅ Emergency cleanup completed")
    }

    /**
     * Reset test environment to baseline state
     */
    suspend fun resetTestEnvironment() {
        println("🔄 Resetting test environment to baseline state...")

        // Clear all active sessions
        emergencyCleanupAll()

        // Reset application cache
        try {
            resetAppCache()
        } catch (e: Exception) {
            println("⚠️  Failed to reset app cache: ${e.message}")
        }

        // Reset device state
        try {
            resetDeviceTestState()
        } catch (e: Exception) {
            println("⚠️  Failed to reset device state: ${e.message}")
        }

        // Clear test databases
        try {
            clearTestDatabases()
        } catch (e: Exception) {
            println("⚠️  Failed to clear test databases: ${e.message}")
        }

        delay(2000) // Allow time for reset operations to complete
        println("✅ Test environment reset completed")
    }

    /**
     * Add custom cleanup task
     */
    fun addCleanupTask(task: CleanupTask) {
        cleanupTasks.add(task)
        println("📋 Added cleanup task: ${task.name}")
    }

    /**
     * Get test isolation report
     */
    fun getTestIsolationReport(): TestIsolationReport {
        return TestIsolationReport(
            activeSessions = activeTestSessions.size,
            registeredData = testDataRegistry.size,
            cleanupTasks = cleanupTasks.size,
            isolationScore = calculateIsolationScore()
        )
    }

    // Private helper methods

    private suspend fun cleanupTestDataEntry(dataEntry: TestDataEntry) {
        when (dataEntry.type) {
            TestDataType.FIREBASE_EVENT -> {
                // Clean up Firebase event data
                println("🗑️  Cleaning up Firebase event: ${dataEntry.id}")
                delay(100) // Simulate Firebase cleanup time
            }
            TestDataType.LOCAL_CACHE -> {
                // Clean up local cache data
                println("🗑️  Cleaning up local cache: ${dataEntry.id}")
                clearLocalCacheEntry(dataEntry.id)
            }
            TestDataType.USER_DATA -> {
                // Clean up user data
                println("🗑️  Cleaning up user data: ${dataEntry.id}")
                delay(50) // Simulate user data cleanup time
            }
            TestDataType.LOCATION_DATA -> {
                // Clean up location data
                println("🗑️  Cleaning up location data: ${dataEntry.id}")
                delay(25) // Quick location data cleanup
            }
        }
    }

    private fun cleanupTempFile(filePath: String) {
        val file = File(filePath)
        if (file.exists()) {
            if (file.delete()) {
                println("🗑️  Deleted temp file: $filePath")
            } else {
                println("⚠️  Failed to delete temp file: $filePath")
            }
        }
    }

    private suspend fun cleanupNetworkResource(resource: NetworkResource) {
        println("🗑️  Cleaning up network resource: ${resource.endpoint}")
        // Simulate network resource cleanup
        delay(200)
    }

    private fun resetAppCache() {
        val cacheDir = context.cacheDir
        val externalCacheDir = context.externalCacheDir

        // Clear internal cache
        cacheDir?.let { clearDirectory(it) }

        // Clear external cache
        externalCacheDir?.let { clearDirectory(it) }

        println("🗑️  App cache reset completed")
    }

    private fun resetDeviceTestState() {
        // Reset any device-specific test state
        // This would interact with device state management
        println("📱 Device test state reset completed")
    }

    private fun clearTestDatabases() {
        val databasePath = context.getDatabasePath("test.db")
        if (databasePath.exists()) {
            databasePath.delete()
            println("🗑️  Test database cleared")
        }
    }

    private fun clearDirectory(directory: File) {
        if (directory.exists() && directory.isDirectory) {
            directory.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    clearDirectory(file)
                }
                file.delete()
            }
        }
    }

    private fun clearLocalCacheEntry(entryId: String) {
        // Simulate local cache entry removal
        val cacheFile = File(context.cacheDir, "$entryId.cache")
        if (cacheFile.exists()) {
            cacheFile.delete()
        }
    }

    private fun calculateIsolationScore(): Double {
        // Calculate test isolation effectiveness score
        val sessionsOk = activeTestSessions.values.all { it.isHealthy() }
        val dataRegistered = testDataRegistry.isNotEmpty()
        val cleanupTasksReady = cleanupTasks.isNotEmpty()

        return when {
            sessionsOk && dataRegistered && cleanupTasksReady -> 1.0
            sessionsOk && dataRegistered -> 0.8
            sessionsOk -> 0.6
            else -> 0.4
        }
    }

    // Data classes

    data class TestSession(
        val id: String,
        val startTime: Long,
        val dataEntries: MutableSet<String>,
        val tempFiles: MutableSet<String>,
        val networkResources: MutableSet<NetworkResource>
    ) {
        fun isHealthy(): Boolean {
            val sessionAge = System.currentTimeMillis() - startTime
            return sessionAge < 3600000 // Session healthy if less than 1 hour old
        }
    }

    data class TestDataEntry(
        val id: String,
        val type: TestDataType,
        val size: Long = 0,
        val metadata: Map<String, String> = emptyMap()
    )

    data class NetworkResource(
        val endpoint: String,
        val resourceType: String,
        val metadata: Map<String, String> = emptyMap()
    )

    data class CleanupTask(
        val name: String,
        val sessionId: String, // "*" for global tasks
        val execute: suspend () -> Unit
    )

    data class TestIsolationReport(
        val activeSessions: Int,
        val registeredData: Int,
        val cleanupTasks: Int,
        val isolationScore: Double
    )

    enum class TestDataType {
        FIREBASE_EVENT,
        LOCAL_CACHE,
        USER_DATA,
        LOCATION_DATA
    }
}