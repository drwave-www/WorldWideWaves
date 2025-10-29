package com.worldwidewaves.shared.platform

/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

import com.worldwidewaves.shared.WWWGlobals
import com.worldwidewaves.shared.WWWPlatform
import com.worldwidewaves.shared.WWWSimulation
import com.worldwidewaves.shared.events.utils.CoroutineScopeProvider
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.position.PositionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime

/**
 * Tests for simulation mode functionality in WWWPlatform.
 *
 * These tests validate:
 * - Simulation mode enable/disable
 * - Simulation state management
 * - StateFlow emissions
 * - Position manager integration
 */
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
class SimulationModeTest {
    private class TestCoroutineScopeProvider(
        private val testScope: TestScope,
    ) : CoroutineScopeProvider {
        override fun launchIO(block: suspend CoroutineScope.() -> Unit): Job = testScope.launch(block = block)

        override fun launchDefault(block: suspend CoroutineScope.() -> Unit): Job = testScope.launch(block = block)

        override fun scopeIO(): CoroutineScope = testScope

        override fun scopeDefault(): CoroutineScope = testScope

        override suspend fun <T> withIOContext(block: suspend CoroutineScope.() -> T): T = testScope.run { block() }

        override suspend fun <T> withDefaultContext(block: suspend CoroutineScope.() -> T): T = testScope.run { block() }

        override fun cancelAllCoroutines() {}
    }

    @Test
    fun simulationMode_initialState_disabled() =
        runTest {
            // GIVEN a fresh platform instance
            val coroutineScopeProvider = TestCoroutineScopeProvider(this)
            val positionManager = PositionManager(coroutineScopeProvider)
            val platform = WWWPlatform("Test", positionManager)

            // WHEN checking initial state
            val isEnabled = platform.simulationModeEnabled.first()

            // THEN simulation mode should be disabled
            assertFalse(isEnabled, "Simulation mode should be disabled by default")
        }

    @Test
    fun simulationMode_enable_emitsStateFlow() =
        runTest {
            val coroutineScopeProvider = TestCoroutineScopeProvider(this)
            val positionManager = PositionManager(coroutineScopeProvider)
            val platform = WWWPlatform("Test", positionManager)

            // GIVEN a platform with simulation mode disabled
            assertFalse(platform.simulationModeEnabled.first())

            // WHEN enabling simulation mode
            platform.enableSimulationMode()

            // THEN the StateFlow should emit true
            val isEnabled = platform.simulationModeEnabled.first()
            assertTrue(isEnabled, "Simulation mode should be enabled")
        }

    @Test
    fun simulationMode_disable_emitsStateFlow() =
        runTest {
            val coroutineScopeProvider = TestCoroutineScopeProvider(this)
            val positionManager = PositionManager(coroutineScopeProvider)
            val platform = WWWPlatform("Test", positionManager)

            // GIVEN a platform with simulation mode enabled
            platform.enableSimulationMode()
            assertTrue(platform.simulationModeEnabled.first())

            // WHEN disabling simulation mode
            platform.disableSimulationMode()

            // THEN the StateFlow should emit false
            val isEnabled = platform.simulationModeEnabled.first()
            assertFalse(isEnabled, "Simulation mode should be disabled")
        }

    @Test
    fun simulation_setSimulation_updatesState() =
        runTest {
            val coroutineScopeProvider = TestCoroutineScopeProvider(this)
            val positionManager = PositionManager(coroutineScopeProvider)
            val platform = WWWPlatform("Test", positionManager)

            // GIVEN a simulation object
            val position = Position(lat = 48.8566, lng = 2.3522)
            val timeZone = TimeZone.of("Europe/Paris")
            val startTime = LocalDateTime(2026, 7, 14, 18, 0).toInstant(timeZone)
            val simulation =
                WWWSimulation(
                    startDateTime = startTime,
                    userPosition = position,
                    initialSpeed = WWWGlobals.Wave.DEFAULT_SPEED_SIMULATION,
                )

            // WHEN setting the simulation
            platform.setSimulation(simulation)

            // THEN the simulation should be set
            assertTrue(platform.isOnSimulation(), "Platform should be on simulation")
            assertNotNull(platform.getSimulation(), "Simulation should not be null")
            assertEquals(position, platform.getSimulation()?.getUserPosition())
        }

    @Test
    fun simulation_disableSimulation_clearsState() =
        runTest {
            val coroutineScopeProvider = TestCoroutineScopeProvider(this)
            val positionManager = PositionManager(coroutineScopeProvider)
            val platform = WWWPlatform("Test", positionManager)

            // GIVEN a platform with active simulation
            val position = Position(lat = 48.8566, lng = 2.3522)
            val timeZone = TimeZone.of("Europe/Paris")
            val startTime = LocalDateTime(2026, 7, 14, 18, 0).toInstant(timeZone)
            val simulation =
                WWWSimulation(
                    startDateTime = startTime,
                    userPosition = position,
                    initialSpeed = WWWGlobals.Wave.DEFAULT_SPEED_SIMULATION,
                )
            platform.setSimulation(simulation)
            assertTrue(platform.isOnSimulation())

            // WHEN disabling simulation
            platform.disableSimulation()

            // THEN the simulation should be cleared
            assertFalse(platform.isOnSimulation(), "Platform should not be on simulation")
            assertNull(platform.getSimulation(), "Simulation should be null")
        }

    @Test
    fun simulation_setSimulation_incrementsSimulationChanged() =
        runTest {
            val coroutineScopeProvider = TestCoroutineScopeProvider(this)
            val positionManager = PositionManager(coroutineScopeProvider)
            val platform = WWWPlatform("Test", positionManager)

            // GIVEN a platform
            val initialValue = platform.simulationChanged.first()

            // WHEN setting a simulation
            val position = Position(lat = 48.8566, lng = 2.3522)
            val timeZone = TimeZone.of("Europe/Paris")
            val startTime = LocalDateTime(2026, 7, 14, 18, 0).toInstant(timeZone)
            val simulation =
                WWWSimulation(
                    startDateTime = startTime,
                    userPosition = position,
                    initialSpeed = WWWGlobals.Wave.DEFAULT_SPEED_SIMULATION,
                )
            platform.setSimulation(simulation)

            // THEN the simulationChanged counter should increment
            val newValue = platform.simulationChanged.first()
            assertEquals(initialValue + 1, newValue, "simulationChanged should increment by 1")
        }

    @Test
    fun simulation_disableSimulation_incrementsSimulationChanged() =
        runTest {
            val coroutineScopeProvider = TestCoroutineScopeProvider(this)
            val positionManager = PositionManager(coroutineScopeProvider)
            val platform = WWWPlatform("Test", positionManager)

            // GIVEN a platform with active simulation
            val position = Position(lat = 48.8566, lng = 2.3522)
            val timeZone = TimeZone.of("Europe/Paris")
            val startTime = LocalDateTime(2026, 7, 14, 18, 0).toInstant(timeZone)
            val simulation =
                WWWSimulation(
                    startDateTime = startTime,
                    userPosition = position,
                    initialSpeed = WWWGlobals.Wave.DEFAULT_SPEED_SIMULATION,
                )
            platform.setSimulation(simulation)
            val valueAfterSet = platform.simulationChanged.first()

            // WHEN disabling simulation
            platform.disableSimulation()

            // THEN the simulationChanged counter should increment
            val valueAfterDisable = platform.simulationChanged.first()
            assertEquals(valueAfterSet + 1, valueAfterDisable, "simulationChanged should increment by 1")
        }

    @Test
    fun simulationMode_independentFromSimulation_canBeEnabledWithoutSimulation() =
        runTest {
            val coroutineScopeProvider = TestCoroutineScopeProvider(this)
            val positionManager = PositionManager(coroutineScopeProvider)
            val platform = WWWPlatform("Test", positionManager)

            // GIVEN simulation mode is enabled
            platform.enableSimulationMode()
            assertTrue(platform.simulationModeEnabled.first())

            // WHEN checking simulation state
            // THEN no simulation should be active
            assertFalse(platform.isOnSimulation(), "Simulation should not be active")
            assertNull(platform.getSimulation(), "Simulation should be null")
        }

    @Test
    fun simulation_multipleSimulations_replacePrevious() =
        runTest {
            val coroutineScopeProvider = TestCoroutineScopeProvider(this)
            val positionManager = PositionManager(coroutineScopeProvider)
            val platform = WWWPlatform("Test", positionManager)

            // GIVEN a first simulation
            val position1 = Position(lat = 48.8566, lng = 2.3522) // Paris
            val timeZone = TimeZone.of("Europe/Paris")
            val startTime1 = LocalDateTime(2026, 7, 14, 18, 0).toInstant(timeZone)
            val simulation1 =
                WWWSimulation(
                    startDateTime = startTime1,
                    userPosition = position1,
                    initialSpeed = 10,
                )
            platform.setSimulation(simulation1)
            val changedValue1 = platform.simulationChanged.first()

            // WHEN setting a second simulation
            val position2 = Position(lat = 40.7128, lng = -74.0060) // New York
            val startTime2 = LocalDateTime(2026, 7, 14, 20, 0).toInstant(timeZone)
            val simulation2 =
                WWWSimulation(
                    startDateTime = startTime2,
                    userPosition = position2,
                    initialSpeed = 50,
                )
            platform.setSimulation(simulation2)

            // THEN the second simulation should replace the first
            assertTrue(platform.isOnSimulation())
            assertEquals(position2, platform.getSimulation()?.getUserPosition())
            val changedValue2 = platform.simulationChanged.first()
            assertEquals(changedValue1 + 1, changedValue2, "simulationChanged should increment")
        }

    // ============================================================
    // SIMULATION SWITCHING TESTS (iOS Map Fix #2)
    // ============================================================

    @Test
    fun simulation_switchBetweenEvents_disablesPreviousSimulation() =
        runTest {
            val coroutineScopeProvider = TestCoroutineScopeProvider(this)
            val positionManager = PositionManager(coroutineScopeProvider)
            val platform = WWWPlatform("Test", positionManager)

            // GIVEN simulation running for Event A
            val positionA = Position(lat = 48.8566, lng = 2.3522) // Paris
            val timeZone = TimeZone.of("Europe/Paris")
            val startTimeA = LocalDateTime(2026, 7, 14, 18, 0).toInstant(timeZone)
            val simulationA =
                WWWSimulation(
                    startDateTime = startTimeA,
                    userPosition = positionA,
                    initialSpeed = WWWGlobals.Wave.DEFAULT_SPEED_SIMULATION,
                )
            platform.setSimulation(simulationA)
            assertTrue(platform.isOnSimulation(), "Simulation A should be active")

            // WHEN switching to Event B simulation (as SimulationButton does)
            platform.disableSimulation() // SimulationButton calls this before setSimulation

            // THEN previous simulation should be cleared
            assertFalse(platform.isOnSimulation(), "Simulation should be disabled after switch")
            assertNull(platform.getSimulation(), "Previous simulation should be null")
        }

    @Test
    fun simulation_switchBetweenEvents_newSimulationReplacesOld() =
        runTest {
            val coroutineScopeProvider = TestCoroutineScopeProvider(this)
            val positionManager = PositionManager(coroutineScopeProvider)
            val platform = WWWPlatform("Test", positionManager)

            // GIVEN simulation running for Event A
            val positionA = Position(lat = 48.8566, lng = 2.3522) // Paris
            val timeZone = TimeZone.of("Europe/Paris")
            val startTimeA = LocalDateTime(2026, 7, 14, 18, 0).toInstant(timeZone)
            val simulationA =
                WWWSimulation(
                    startDateTime = startTimeA,
                    userPosition = positionA,
                    initialSpeed = WWWGlobals.Wave.DEFAULT_SPEED_SIMULATION,
                )
            platform.setSimulation(simulationA)
            assertEquals(positionA, platform.getSimulation()?.getUserPosition())

            // WHEN starting Event B simulation (full switch flow)
            platform.disableSimulation()
            val positionB = Position(lat = 40.7128, lng = -74.0060) // New York
            val startTimeB = LocalDateTime(2026, 7, 14, 20, 0).toInstant(timeZone)
            val simulationB =
                WWWSimulation(
                    startDateTime = startTimeB,
                    userPosition = positionB,
                    initialSpeed = WWWGlobals.Wave.DEFAULT_SPEED_SIMULATION,
                )
            platform.setSimulation(simulationB)

            // THEN new simulation should be active
            assertTrue(platform.isOnSimulation(), "Simulation B should be active")
            assertEquals(positionB, platform.getSimulation()?.getUserPosition(), "Should have Event B position")
        }

    @Test
    fun simulation_switchBetweenEvents_incrementsChangedCounterTwice() =
        runTest {
            val coroutineScopeProvider = TestCoroutineScopeProvider(this)
            val positionManager = PositionManager(coroutineScopeProvider)
            val platform = WWWPlatform("Test", positionManager)

            val initialCount = platform.simulationChanged.first()

            // GIVEN simulation for Event A
            val positionA = Position(lat = 48.8566, lng = 2.3522)
            val timeZone = TimeZone.of("Europe/Paris")
            val startTimeA = LocalDateTime(2026, 7, 14, 18, 0).toInstant(timeZone)
            val simulationA =
                WWWSimulation(
                    startDateTime = startTimeA,
                    userPosition = positionA,
                    initialSpeed = WWWGlobals.Wave.DEFAULT_SPEED_SIMULATION,
                )
            platform.setSimulation(simulationA)
            val countAfterA = platform.simulationChanged.first()

            // WHEN switching to Event B (disable + setSimulation)
            platform.disableSimulation()
            val countAfterDisable = platform.simulationChanged.first()

            val positionB = Position(lat = 40.7128, lng = -74.0060)
            val startTimeB = LocalDateTime(2026, 7, 14, 20, 0).toInstant(timeZone)
            val simulationB =
                WWWSimulation(
                    startDateTime = startTimeB,
                    userPosition = positionB,
                    initialSpeed = WWWGlobals.Wave.DEFAULT_SPEED_SIMULATION,
                )
            platform.setSimulation(simulationB)
            val countAfterB = platform.simulationChanged.first()

            // THEN counter should increment twice (once for disable, once for new simulation)
            assertEquals(initialCount + 1, countAfterA, "Should increment after Event A")
            assertEquals(countAfterA + 1, countAfterDisable, "Should increment after disable")
            assertEquals(countAfterDisable + 1, countAfterB, "Should increment after Event B")
        }

    @Test
    fun simulation_switchWhileActive_doesNotLeaveStaleState() =
        runTest {
            val coroutineScopeProvider = TestCoroutineScopeProvider(this)
            val positionManager = PositionManager(coroutineScopeProvider)
            val platform = WWWPlatform("Test", positionManager)

            // GIVEN active simulation
            val position1 = Position(lat = 48.8566, lng = 2.3522)
            val timeZone = TimeZone.of("Europe/Paris")
            val startTime1 = LocalDateTime(2026, 7, 14, 18, 0).toInstant(timeZone)
            val simulation1 =
                WWWSimulation(
                    startDateTime = startTime1,
                    userPosition = position1,
                    initialSpeed = 10,
                )
            platform.setSimulation(simulation1)

            // WHEN rapidly switching (simulates quick button presses)
            platform.disableSimulation()
            val position2 = Position(lat = 40.7128, lng = -74.0060)
            val startTime2 = LocalDateTime(2026, 7, 14, 20, 0).toInstant(timeZone)
            val simulation2 =
                WWWSimulation(
                    startDateTime = startTime2,
                    userPosition = position2,
                    initialSpeed = 20,
                )
            platform.setSimulation(simulation2)

            platform.disableSimulation()
            val position3 = Position(lat = 51.5074, lng = -0.1278) // London
            val startTime3 = LocalDateTime(2026, 7, 14, 22, 0).toInstant(timeZone)
            val simulation3 =
                WWWSimulation(
                    startDateTime = startTime3,
                    userPosition = position3,
                    initialSpeed = 30,
                )
            platform.setSimulation(simulation3)

            // THEN final state should be clean (only last simulation active)
            assertTrue(platform.isOnSimulation(), "Final simulation should be active")
            assertEquals(position3, platform.getSimulation()?.getUserPosition(), "Should have final position")
        }

    @Test
    fun simulation_disableDuringSwitch_clearsPositionCorrectly() =
        runTest {
            val coroutineScopeProvider = TestCoroutineScopeProvider(this)
            val positionManager = PositionManager(coroutineScopeProvider)
            val platform = WWWPlatform("Test", positionManager)

            // GIVEN simulation with position
            val position1 = Position(lat = 48.8566, lng = 2.3522)
            val timeZone = TimeZone.of("Europe/Paris")
            val startTime1 = LocalDateTime(2026, 7, 14, 18, 0).toInstant(timeZone)
            val simulation1 =
                WWWSimulation(
                    startDateTime = startTime1,
                    userPosition = position1,
                    initialSpeed = WWWGlobals.Wave.DEFAULT_SPEED_SIMULATION,
                )
            platform.setSimulation(simulation1)
            assertNotNull(platform.getSimulation()?.getUserPosition())

            // WHEN disabling simulation
            platform.disableSimulation()

            // THEN simulation and position should be cleared
            assertNull(platform.getSimulation(), "Simulation should be null after disable")
            assertFalse(platform.isOnSimulation(), "isOnSimulation should be false")
        }
}
