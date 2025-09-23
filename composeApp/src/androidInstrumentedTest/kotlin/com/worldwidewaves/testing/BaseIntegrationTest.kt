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

package com.worldwidewaves.testing

import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.WWWEvents
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Before

/**
 * Base class for integration tests.
 *
 * Provides setup for testing interactions between multiple components,
 * Firebase integration, network scenarios, and end-to-end flows.
 */
abstract class BaseIntegrationTest : BaseInstrumentedTest() {

    protected lateinit var mockWWWEvents: WWWEvents

    @Before
    override fun setUp() {
        super.setUp()
        setupIntegrationMocks()
    }

    private fun setupIntegrationMocks() {
        mockWWWEvents = mockk(relaxed = true)
    }

    /**
     * Creates a test event flow with specified number of events
     */
    protected fun createTestEventFlow(eventCount: Int): MutableStateFlow<List<IWWWEvent>> {
        val testEvents = (1..eventCount).map { index ->
            createMockEvent("test-event-$index")
        }
        return MutableStateFlow(testEvents)
    }

    /**
     * Creates a mock event with basic properties
     */
    protected fun createMockEvent(eventId: String): IWWWEvent {
        return mockk<IWWWEvent>(relaxed = true) {
            io.mockk.every { id } returns eventId
            io.mockk.every { status } returns IWWWEvent.Status.SOON
        }
    }

    /**
     * Simulates network connectivity states
     */
    protected fun simulateNetworkState(isConnected: Boolean, isSlowConnection: Boolean = false) {
        // This would integrate with actual network state management
        // For now, providing structure for future implementation
    }

    /**
     * Simulates location permission states
     */
    protected fun simulateLocationPermission(isGranted: Boolean) {
        // This would integrate with actual permission management
        // For now, providing structure for future implementation
    }

    /**
     * Simulates Firebase authentication states
     */
    protected fun simulateFirebaseAuth(isAuthenticated: Boolean, userId: String? = null) {
        // This would integrate with actual Firebase auth
        // For now, providing structure for future implementation
    }

    /**
     * Extended timeouts for integration tests that may involve network calls
     */
    companion object {
        const val INTEGRATION_TIMEOUT = 10000L
        const val NETWORK_TIMEOUT = 15000L
        const val AUTH_TIMEOUT = 8000L
    }
}