package com.worldwidewaves.shared

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

import com.worldwidewaves.shared.events.utils.CoroutineScopeProvider
import com.worldwidewaves.shared.events.utils.Position
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Tests for WWWPlatform simulation state management and transitions.
 * These tests ensure that simulation mode and instance state is correctly managed
 * and that change notifications work properly for reactive UI updates.
 */
@OptIn(ExperimentalTime::class)
class PlatformTest {

    @Test
    fun `should initialize with default simulation state`() {
        // GIVEN: A new platform instance
        val platform = WWWPlatform("test-platform")

        // WHEN/THEN: Default state should be correctly initialized
        assertFalse(platform.simulationModeEnabled.value, "Simulation mode should be disabled by default")
        assertFalse(platform.isOnSimulation(), "Should not be on simulation by default")
        assertNull(platform.getSimulation(), "Simulation should be null by default")
        assertEquals(0, platform.simulationChanged.value, "Simulation change counter should start at 0")
    }

    @Test
    fun `should enable and disable simulation mode correctly`() {
        // GIVEN: A platform instance
        val platform = WWWPlatform("test-platform")

        // WHEN: Enabling simulation mode
        platform.enableSimulationMode()

        // THEN: Simulation mode should be enabled
        assertTrue(platform.simulationModeEnabled.value, "Simulation mode should be enabled")

        // WHEN: Disabling simulation mode
        platform.disableSimulationMode()

        // THEN: Simulation mode should be disabled
        assertFalse(platform.simulationModeEnabled.value, "Simulation mode should be disabled")
    }

    @Test
    fun `should set simulation and update change counter`() {
        // GIVEN: A platform instance and a simulation
        val platform = WWWPlatform("test-platform")
        val startTime = Instant.fromEpochMilliseconds(1000)
        val userPosition = Position(37.7749, -122.4194)
        val simulation = WWWSimulation(startTime, userPosition, 10)
        val initialChangeValue = platform.simulationChanged.value

        // WHEN: Setting the simulation
        platform.setSimulation(simulation)

        // THEN: Simulation should be set and change counter incremented
        assertTrue(platform.isOnSimulation(), "Should be on simulation after setting")
        assertEquals(simulation, platform.getSimulation(), "Simulation should match the one set")
        assertEquals(initialChangeValue + 1, platform.simulationChanged.value, "Change counter should be incremented")
    }

    @Test
    fun `should disable simulation and update change counter`() {
        // GIVEN: A platform instance with an active simulation
        val platform = WWWPlatform("test-platform")
        val startTime = Instant.fromEpochMilliseconds(1000)
        val userPosition = Position(37.7749, -122.4194)
        val simulation = WWWSimulation(startTime, userPosition, 5)

        platform.setSimulation(simulation)
        val changeValueAfterSet = platform.simulationChanged.value

        // WHEN: Disabling the simulation
        platform.disableSimulation()

        // THEN: Simulation should be disabled and change counter incremented
        assertFalse(platform.isOnSimulation(), "Should not be on simulation after disabling")
        assertNull(platform.getSimulation(), "Simulation should be null after disabling")
        assertEquals(changeValueAfterSet + 1, platform.simulationChanged.value, "Change counter should be incremented again")
    }

    @Test
    fun `should handle multiple simulation changes with correct change tracking`() {
        // GIVEN: A platform instance and multiple simulations
        val platform = WWWPlatform("test-platform")
        val startTime = Instant.fromEpochMilliseconds(1000)
        val userPosition1 = Position(37.7749, -122.4194)
        val userPosition2 = Position(40.7128, -74.0060)
        val simulation1 = WWWSimulation(startTime, userPosition1, 10)
        val simulation2 = WWWSimulation(startTime, userPosition2, 20)

        val initialChangeValue = platform.simulationChanged.value

        // WHEN: Setting first simulation
        platform.setSimulation(simulation1)
        val firstChangeValue = platform.simulationChanged.value

        // THEN: First simulation should be active
        assertEquals(simulation1, platform.getSimulation(), "First simulation should be active")
        assertEquals(initialChangeValue + 1, firstChangeValue, "Change counter should increment for first simulation")

        // WHEN: Setting second simulation
        platform.setSimulation(simulation2)
        val secondChangeValue = platform.simulationChanged.value

        // THEN: Second simulation should be active
        assertEquals(simulation2, platform.getSimulation(), "Second simulation should be active")
        assertEquals(firstChangeValue + 1, secondChangeValue, "Change counter should increment for second simulation")

        // WHEN: Disabling simulation
        platform.disableSimulation()
        val finalChangeValue = platform.simulationChanged.value

        // THEN: No simulation should be active
        assertNull(platform.getSimulation(), "No simulation should be active after disabling")
        assertEquals(secondChangeValue + 1, finalChangeValue, "Change counter should increment for disabling")
    }

    @Test
    fun `should maintain simulation mode state independent of simulation instance`() {
        // GIVEN: A platform instance
        val platform = WWWPlatform("test-platform")
        val startTime = Instant.fromEpochMilliseconds(1000)
        val userPosition = Position(37.7749, -122.4194)
        val simulation = WWWSimulation(startTime, userPosition, 10)

        // WHEN: Enabling simulation mode without setting simulation
        platform.enableSimulationMode()

        // THEN: Mode should be enabled but no simulation active
        assertTrue(platform.simulationModeEnabled.value, "Simulation mode should be enabled")
        assertFalse(platform.isOnSimulation(), "Should not be on simulation")

        // WHEN: Setting simulation while mode is enabled
        platform.setSimulation(simulation)

        // THEN: Both mode and simulation should be active
        assertTrue(platform.simulationModeEnabled.value, "Simulation mode should still be enabled")
        assertTrue(platform.isOnSimulation(), "Should be on simulation")

        // WHEN: Disabling simulation but keeping mode enabled
        platform.disableSimulation()

        // THEN: Mode should remain enabled but no simulation active
        assertTrue(platform.simulationModeEnabled.value, "Simulation mode should still be enabled")
        assertFalse(platform.isOnSimulation(), "Should not be on simulation")

        // WHEN: Disabling simulation mode
        platform.disableSimulationMode()

        // THEN: Mode should be disabled
        assertFalse(platform.simulationModeEnabled.value, "Simulation mode should be disabled")
        assertFalse(platform.isOnSimulation(), "Should still not be on simulation")
    }

    @Test
    fun `should handle rapid simulation changes correctly`() {
        // GIVEN: A platform instance and multiple simulations
        val platform = WWWPlatform("test-platform")
        val startTime = Instant.fromEpochMilliseconds(1000)
        val userPosition = Position(37.7749, -122.4194)
        val simulations = listOf(
            WWWSimulation(startTime, userPosition, 5),
            WWWSimulation(startTime, userPosition, 10),
            WWWSimulation(startTime, userPosition, 15),
        )

        val initialChangeValue = platform.simulationChanged.value

        // WHEN: Rapidly setting and disabling simulations
        simulations.forEachIndexed { index, simulation ->
            platform.setSimulation(simulation)
            assertEquals(simulation, platform.getSimulation(), "Simulation $index should be active")
            assertTrue(platform.isOnSimulation(), "Should be on simulation $index")

            platform.disableSimulation()
            assertNull(platform.getSimulation(), "No simulation should be active after disabling $index")
            assertFalse(platform.isOnSimulation(), "Should not be on simulation after disabling $index")
        }

        // THEN: Change counter should reflect all operations
        val expectedChangeValue = initialChangeValue + (simulations.size * 2) // Each sim: set + disable
        assertEquals(expectedChangeValue, platform.simulationChanged.value, "Change counter should reflect all operations")
    }

    @Test
    fun `should handle simulation state transitions with different speeds`() {
        // GIVEN: A platform instance
        val platform = WWWPlatform("test-platform")
        val startTime = Instant.fromEpochMilliseconds(1000)
        val userPosition = Position(37.7749, -122.4194)

        // WHEN/THEN: Testing with different simulation speeds
        val speeds = listOf(1, 10, 50, 100, WWWSimulation.MAX_SPEED)
        speeds.forEach { speed ->
            val simulation = WWWSimulation(startTime, userPosition, speed)
            platform.setSimulation(simulation)

            assertTrue(platform.isOnSimulation(), "Should be on simulation with speed $speed")
            assertEquals(simulation, platform.getSimulation(), "Simulation with speed $speed should be active")
            assertEquals(speed, platform.getSimulation()?.speed, "Simulation speed should be $speed")

            platform.disableSimulation()
            assertFalse(platform.isOnSimulation(), "Should not be on simulation after disabling speed $speed")
        }
    }

    @Test
    fun `should preserve simulation properties when accessed through platform`() {
        // GIVEN: A platform instance and a simulation with specific properties
        val platform = WWWPlatform("test-platform")
        val startTime = Instant.fromEpochMilliseconds(5000)
        val userPosition = Position(51.5074, -0.1278) // London coordinates
        val speed = 25
        val simulation = WWWSimulation(startTime, userPosition, speed)

        // WHEN: Setting the simulation
        platform.setSimulation(simulation)

        // THEN: All simulation properties should be preserved
        val retrievedSimulation = platform.getSimulation()
        assertEquals(speed, retrievedSimulation?.speed, "Speed should be preserved")
        assertEquals(userPosition, retrievedSimulation?.getUserPosition(), "User position should be preserved")

        // Verify that we get the same simulation instance back
        assertEquals(simulation, retrievedSimulation, "Should return the same simulation instance")
    }

    @Test
    fun `should handle edge case of setting same simulation multiple times`() {
        // GIVEN: A platform instance and a simulation
        val platform = WWWPlatform("test-platform")
        val startTime = Instant.fromEpochMilliseconds(1000)
        val userPosition = Position(37.7749, -122.4194)
        val simulation = WWWSimulation(startTime, userPosition, 10)

        val initialChangeValue = platform.simulationChanged.value

        // WHEN: Setting the same simulation multiple times
        platform.setSimulation(simulation)
        val firstChangeValue = platform.simulationChanged.value

        platform.setSimulation(simulation)
        val secondChangeValue = platform.simulationChanged.value

        platform.setSimulation(simulation)
        val thirdChangeValue = platform.simulationChanged.value

        // THEN: Each set operation should increment the change counter
        assertEquals(initialChangeValue + 1, firstChangeValue, "First set should increment counter")
        assertEquals(firstChangeValue + 1, secondChangeValue, "Second set should increment counter")
        assertEquals(secondChangeValue + 1, thirdChangeValue, "Third set should increment counter")
        assertEquals(simulation, platform.getSimulation(), "Simulation should remain the same instance")
    }

    @Test
    fun `should provide correct platform name`() {
        // GIVEN: Platform instances with different names
        val platformNames = listOf("android", "ios", "desktop", "test-platform")

        platformNames.forEach { name ->
            // WHEN: Creating platform with specific name
            val platform = WWWPlatform(name)

            // THEN: Platform should return correct name
            assertEquals(name, platform.name, "Platform name should match constructor parameter")
        }
    }
}

/**
 * Tests for WWWShutdownHandler coroutine scope management.
 * These tests ensure that shutdown handling correctly cancels all coroutines.
 */
class ShutdownHandlerTest {

    @Test
    fun `should cancel all coroutines on app shutdown`() {
        // GIVEN: A mocked coroutine scope provider
        val mockScopeProvider = mockk<CoroutineScopeProvider>(relaxed = true)
        val shutdownHandler = WWWShutdownHandler(mockScopeProvider)

        // WHEN: App shutdown is triggered
        shutdownHandler.onAppShutdown()

        // THEN: All coroutines should be cancelled
        verify(exactly = 1) { mockScopeProvider.cancelAllCoroutines() }
    }

    @Test
    fun `should handle multiple shutdown calls gracefully`() {
        // GIVEN: A mocked coroutine scope provider
        val mockScopeProvider = mockk<CoroutineScopeProvider>(relaxed = true)
        val shutdownHandler = WWWShutdownHandler(mockScopeProvider)

        // WHEN: Multiple shutdown calls are made
        shutdownHandler.onAppShutdown()
        shutdownHandler.onAppShutdown()
        shutdownHandler.onAppShutdown()

        // THEN: Cancel should be called for each shutdown call
        verify(exactly = 3) { mockScopeProvider.cancelAllCoroutines() }
    }
}