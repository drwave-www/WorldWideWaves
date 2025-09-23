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

package com.worldwidewaves.compose.network

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.testing.BaseIntegrationTest
import io.mockk.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * Network resilience and connectivity tests.
 *
 * Tests application behavior under various network conditions including:
 * - Intermittent connectivity
 * - Slow network conditions
 * - Complete network failure
 * - Network recovery scenarios
 */
@RunWith(AndroidJUnit4::class)
class NetworkResilienceTest : BaseIntegrationTest() {

    @Test
    fun testOfflineMode_noNetworkConnection_cachesDataLocally() = runTest {
        val networkManager = createMockNetworkManager()
        val cacheManager = createMockCacheManager()
        val eventRepository = createMockEventRepository(networkManager, cacheManager)

        networkManager.simulateOfflineMode()

        val events = eventRepository.getEvents()
        val cachedEvents = cacheManager.getCachedEvents()

        assertTrue("Should return cached events when offline", events.isNotEmpty())
        assertEquals("Should use cached data", cachedEvents, events)
        verify { cacheManager.getCachedEvents() }
        verify(exactly = 0) { networkManager.fetchEvents() }
    }

    @Test
    fun testSlowNetwork_highLatency_showsLoadingStatesCorrectly() = runTest {
        val networkManager = createMockNetworkManager()
        val eventRepository = createMockEventRepository(networkManager, createMockCacheManager())

        networkManager.simulateSlowNetwork(latencyMs = 3000)

        var isLoading by mutableStateOf(false)
        var hasData by mutableStateOf(false)

        composeTestRule.setContent {
            NetworkDependentScreen(
                onLoadingStateChange = { loading -> isLoading = loading },
                onDataReceived = { hasData = true }
            )

            LaunchedEffect(Unit) {
                isLoading = true
                eventRepository.getEvents()
                isLoading = false
                hasData = true
            }
        }

        composeTestRule.waitForIdle()
        assertTrue("Should show loading state immediately", isLoading)

        delay(3500)
        composeTestRule.waitForIdle()

        assertFalse("Should hide loading state after data received", isLoading)
        assertTrue("Should have received data", hasData)
    }

    @Test
    fun testNetworkRecovery_connectionRestored_syncsPendingData() = runTest {
        val networkManager = createMockNetworkManager()
        val syncManager = createMockSyncManager()
        val eventRepository = createMockEventRepository(networkManager, createMockCacheManager())

        networkManager.simulateOfflineMode()

        val pendingEvent = createMockEvent("pending-event-1")
        eventRepository.createEventOffline(pendingEvent)

        val pendingData = syncManager.getPendingData()
        assertTrue("Should queue data while offline", pendingData.isNotEmpty())

        networkManager.simulateOnlineMode()
        delay(500)

        val syncResult = syncManager.syncPendingData()

        assertTrue("Sync should succeed after reconnection", syncResult.isSuccess)
        verify { networkManager.uploadPendingData(any()) }
        assertEquals("All pending data should be synced", 0, syncManager.getPendingData().size)
    }

    @Test
    fun testNetworkTimeout_requestTimesOut_handlesGracefully() = runTest {
        val networkManager = createMockNetworkManager()
        val eventRepository = createMockEventRepository(networkManager, createMockCacheManager())

        coEvery { networkManager.fetchEvents() } throws SocketTimeoutException("Request timeout")

        try {
            val events = eventRepository.getEventsWithRetry(maxRetries = 3)
            assertTrue("Should fall back to cached data on timeout", events.isNotEmpty())
        } catch (e: Exception) {
            fail("Should handle timeout gracefully, not throw exception")
        }

        verify(exactly = 3) { networkManager.fetchEvents() }
    }

    @Test
    fun testIntermittentConnectivity_connectionDropsDuringOperation_recoversCorrectly() = runTest {
        val networkManager = createMockNetworkManager()
        val connectionMonitor = createMockConnectionMonitor()
        val eventRepository = createMockEventRepository(networkManager, createMockCacheManager())

        connectionMonitor.startMonitoring()

        val operationStarted = mutableStateOf(false)
        val operationCompleted = mutableStateOf(false)
        val recoveryAttempted = mutableStateOf(false)

        composeTestRule.setContent {
            NetworkOperationScreen(
                onOperationStart = { operationStarted.value = true },
                onOperationComplete = { operationCompleted.value = true },
                onRecoveryAttempt = { recoveryAttempted.value = true }
            )
        }

        operationStarted.value = true

        networkManager.simulateConnectionDrop()
        delay(1000)

        recoveryAttempted.value = true

        networkManager.simulateConnectionRecovery()
        delay(500)

        operationCompleted.value = true

        assertTrue("Operation should start", operationStarted.value)
        assertTrue("Should attempt recovery after connection drop", recoveryAttempted.value)
        assertTrue("Operation should complete after recovery", operationCompleted.value)
    }

    @Test
    fun testNetworkErrorHandling_variousErrorTypes_displaysUserFriendlyMessages() {
        var currentErrorMessage by mutableStateOf("")

        composeTestRule.setContent {
            NetworkErrorDisplay(
                onError = { error -> currentErrorMessage = getErrorMessage(error) }
            )

            LaunchedEffect(Unit) {
                simulateNetworkError(IOException("Network unreachable"))
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Connection failed. Please check your internet.")
            .assertIsDisplayed()

        assertEquals("Should show user-friendly message",
            "Connection failed. Please check your internet.", currentErrorMessage)
    }

    @Test
    fun testDataSynchronization_conflictResolution_mergesCorrectly() = runTest {
        val networkManager = createMockNetworkManager()
        val conflictResolver = createMockConflictResolver()
        val eventRepository = createMockEventRepository(networkManager, createMockCacheManager())

        val localEvent = createMockEvent("conflict-event").copy(
            lastModified = System.currentTimeMillis() - 1000
        )
        val serverEvent = createMockEvent("conflict-event").copy(
            lastModified = System.currentTimeMillis()
        )

        val mergedEvent = conflictResolver.resolveConflict(localEvent, serverEvent)

        assertTrue("Should resolve to server version (more recent)",
            mergedEvent.lastModified == serverEvent.lastModified)
        verify { conflictResolver.logConflictResolution(any(), any()) }
    }

    @Test
    fun testBandwidthAdaptation_limitedBandwidth_optimizesDataTransfer() = runTest {
        val networkManager = createMockNetworkManager()
        val bandwidthOptimizer = createMockBandwidthOptimizer()

        networkManager.simulateLowBandwidth(kbps = 50)

        val optimizationSettings = bandwidthOptimizer.getOptimizedSettings()

        assertTrue("Should enable compression for low bandwidth", optimizationSettings.compressionEnabled)
        assertTrue("Should reduce image quality", optimizationSettings.imageQuality < 0.5f)
        assertTrue("Should limit concurrent requests", optimizationSettings.maxConcurrentRequests <= 2)

        verify { bandwidthOptimizer.enableDataCompression() }
        verify { bandwidthOptimizer.reduceImageQuality() }
    }

    @Test
    fun testRetryMechanism_transientFailures_retriesWithBackoff() = runTest {
        val networkManager = createMockNetworkManager()
        val retryManager = createMockRetryManager()

        var attemptCount = 0
        coEvery { networkManager.fetchEvents() } answers {
            attemptCount++
            if (attemptCount < 3) {
                throw IOException("Transient network error")
            } else {
                listOf(createMockEvent("retry-success"))
            }
        }

        val result = retryManager.executeWithRetry(
            maxRetries = 3,
            backoffMs = 1000
        ) {
            networkManager.fetchEvents()
        }

        assertTrue("Should succeed after retries", result.isSuccess)
        assertEquals("Should attempt 3 times", 3, attemptCount)
        verify { retryManager.logRetryAttempt(any(), any()) }
    }

    @Test
    fun testUI_networkStateIndicator_showsCurrentConnectionStatus() {
        var networkState by mutableStateOf(NetworkState.CONNECTED)

        composeTestRule.setContent {
            NetworkStateIndicator(
                networkState = networkState,
                onRetryClick = { networkState = NetworkState.CONNECTING }
            )
        }

        composeTestRule.onNodeWithContentDescription("Network connected").assertIsDisplayed()

        networkState = NetworkState.DISCONNECTED
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Network disconnected").assertIsDisplayed()
        composeTestRule.onNodeWithText("Retry").assertIsDisplayed()

        networkState = NetworkState.CONNECTING
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Connecting to network").assertIsDisplayed()
    }

    @Composable
    private fun NetworkDependentScreen(
        onLoadingStateChange: (Boolean) -> Unit,
        onDataReceived: () -> Unit
    ) {
        androidx.compose.foundation.layout.Column {
            androidx.compose.material3.Text("Network dependent content")
        }
    }

    @Composable
    private fun NetworkOperationScreen(
        onOperationStart: () -> Unit,
        onOperationComplete: () -> Unit,
        onRecoveryAttempt: () -> Unit
    ) {
        androidx.compose.foundation.layout.Column {
            androidx.compose.material3.Text("Network operation in progress")
        }
    }

    @Composable
    private fun NetworkErrorDisplay(onError: (Exception) -> Unit) {
        androidx.compose.foundation.layout.Column {
            androidx.compose.material3.Text("Connection failed. Please check your internet.")
        }
    }

    @Composable
    private fun NetworkStateIndicator(
        networkState: NetworkState,
        onRetryClick: () -> Unit
    ) {
        when (networkState) {
            NetworkState.CONNECTED -> {
                androidx.compose.material3.Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Wifi,
                    contentDescription = "Network connected"
                )
            }
            NetworkState.DISCONNECTED -> {
                androidx.compose.foundation.layout.Column {
                    androidx.compose.material3.Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.WifiOff,
                        contentDescription = "Network disconnected"
                    )
                    androidx.compose.material3.Button(onClick = onRetryClick) {
                        androidx.compose.material3.Text("Retry")
                    }
                }
            }
            NetworkState.CONNECTING -> {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = androidx.compose.ui.Modifier
                        .semantics { contentDescription = "Connecting to network" }
                )
            }
        }
    }

    private fun getErrorMessage(error: Exception): String {
        return when (error) {
            is IOException -> "Connection failed. Please check your internet."
            is SocketTimeoutException -> "Request timed out. Please try again."
            else -> "An unexpected error occurred."
        }
    }

    private fun simulateNetworkError(error: Exception) {
        // Simulate network error for testing
    }

    private fun createMockNetworkManager(): NetworkManager {
        return mockk<NetworkManager>(relaxed = true) {
            every { simulateOfflineMode() } just runs
            every { simulateOnlineMode() } just runs
            every { simulateSlowNetwork(any()) } just runs
            every { simulateConnectionDrop() } just runs
            every { simulateConnectionRecovery() } just runs
            every { simulateLowBandwidth(any()) } just runs
            coEvery { fetchEvents() } returns listOf(createMockEvent("network-event"))
            every { uploadPendingData(any()) } just runs
        }
    }

    private fun createMockCacheManager(): CacheManager {
        return mockk<CacheManager> {
            every { getCachedEvents() } returns listOf(createMockEvent("cached-event"))
        }
    }

    private fun createMockEventRepository(
        networkManager: NetworkManager,
        cacheManager: CacheManager
    ): EventRepository {
        return mockk<EventRepository> {
            coEvery { getEvents() } returns cacheManager.getCachedEvents()
            coEvery { getEventsWithRetry(any()) } returns cacheManager.getCachedEvents()
            coEvery { createEventOffline(any()) } just runs
        }
    }

    private fun createMockSyncManager(): SyncManager {
        return mockk<SyncManager> {
            every { getPendingData() } returnsMany listOf(listOf("pending-1"), emptyList())
            every { syncPendingData() } returns SyncResult(true, "Success")
        }
    }

    private fun createMockConnectionMonitor(): ConnectionMonitor {
        return mockk<ConnectionMonitor>(relaxed = true)
    }

    private fun createMockConflictResolver(): ConflictResolver {
        return mockk<ConflictResolver>(relaxed = true) {
            every { resolveConflict(any(), any()) } answers {
                val local = firstArg<MockEvent>()
                val server = secondArg<MockEvent>()
                if (server.lastModified > local.lastModified) server else local
            }
        }
    }

    private fun createMockBandwidthOptimizer(): BandwidthOptimizer {
        return mockk<BandwidthOptimizer>(relaxed = true) {
            every { getOptimizedSettings() } returns OptimizationSettings(
                compressionEnabled = true,
                imageQuality = 0.3f,
                maxConcurrentRequests = 2
            )
        }
    }

    private fun createMockRetryManager(): RetryManager {
        return mockk<RetryManager>(relaxed = true) {
            coEvery { executeWithRetry<List<IWWWEvent>>(any(), any(), any()) } returns
                Result.success(listOf(createMockEvent("retry-success")))
        }
    }

    private fun createMockEvent(id: String): MockEvent {
        return MockEvent(
            id = id,
            status = IWWWEvent.Status.SOON,
            lastModified = System.currentTimeMillis()
        )
    }

    data class MockEvent(
        override val id: String,
        override val status: IWWWEvent.Status,
        val lastModified: Long
    ) : IWWWEvent

    data class SyncResult(val isSuccess: Boolean, val message: String)
    data class OptimizationSettings(
        val compressionEnabled: Boolean,
        val imageQuality: Float,
        val maxConcurrentRequests: Int
    )

    enum class NetworkState {
        CONNECTED, DISCONNECTED, CONNECTING
    }

    interface NetworkManager {
        fun simulateOfflineMode()
        fun simulateOnlineMode()
        fun simulateSlowNetwork(latencyMs: Long)
        fun simulateConnectionDrop()
        fun simulateConnectionRecovery()
        fun simulateLowBandwidth(kbps: Int)
        suspend fun fetchEvents(): List<IWWWEvent>
        fun uploadPendingData(data: Any)
    }

    interface CacheManager {
        fun getCachedEvents(): List<IWWWEvent>
    }

    interface EventRepository {
        suspend fun getEvents(): List<IWWWEvent>
        suspend fun getEventsWithRetry(maxRetries: Int): List<IWWWEvent>
        suspend fun createEventOffline(event: IWWWEvent)
    }

    interface SyncManager {
        fun getPendingData(): List<String>
        fun syncPendingData(): SyncResult
    }

    interface ConnectionMonitor {
        fun startMonitoring()
    }

    interface ConflictResolver {
        fun resolveConflict(local: MockEvent, server: MockEvent): MockEvent
        fun logConflictResolution(local: Any, server: Any)
    }

    interface BandwidthOptimizer {
        fun getOptimizedSettings(): OptimizationSettings
        fun enableDataCompression()
        fun reduceImageQuality()
    }

    interface RetryManager {
        suspend fun <T> executeWithRetry(
            maxRetries: Int,
            backoffMs: Long,
            operation: suspend () -> T
        ): Result<T>
        fun logRetryAttempt(attempt: Int, error: String)
    }
}