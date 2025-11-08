package com.worldwidewaves.shared.position

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
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.events.utils.Position
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
class PositionManagerTest {
    private class TestClock(
        private var currentTime: Instant = Instant.fromEpochMilliseconds(0),
    ) : IClock {
        override fun now(): Instant = currentTime

        override suspend fun delay(duration: Duration) {
            currentTime += duration
        }

        fun advance(duration: Duration) {
            currentTime += duration
        }
    }

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
    fun `should emit initial null position`() =
        runTest {
            val coroutineScopeProvider = TestCoroutineScopeProvider(this)
            val clock = TestClock()
            val positionManager = PositionManager(coroutineScopeProvider, clock = clock)

            assertNull(positionManager.getCurrentPosition())
            assertNull(positionManager.getCurrentSource())
            assertEquals(null, positionManager.position.value)
        }

    @Test
    fun `should update position from GPS source`() =
        runTest {
            val coroutineScopeProvider = TestCoroutineScopeProvider(this)
            val positionManager = PositionManager(coroutineScopeProvider, debounceDelay = 0.milliseconds, clock = TestClock())
            val gpsPosition = Position(lat = 48.8566, lng = 2.3522)

            positionManager.updatePosition(PositionManager.PositionSource.GPS, gpsPosition)

            // Run all pending coroutines
            testScheduler.runCurrent()

            assertEquals(gpsPosition, positionManager.getCurrentPosition())
            assertEquals(PositionManager.PositionSource.GPS, positionManager.getCurrentSource())
            assertEquals(gpsPosition, positionManager.position.value)
        }

    @Test
    fun `should prioritize simulation over GPS`() =
        runTest {
            val coroutineScopeProvider = TestCoroutineScopeProvider(this)
            val positionManager = PositionManager(coroutineScopeProvider, debounceDelay = 0.milliseconds, clock = TestClock())
            val gpsPosition = Position(lat = 48.8566, lng = 2.3522)
            val simulationPosition = Position(lat = 40.7128, lng = -74.0060)

            // Set GPS position first
            positionManager.updatePosition(PositionManager.PositionSource.GPS, gpsPosition)
            testScheduler.runCurrent()
            assertEquals(gpsPosition, positionManager.getCurrentPosition())

            // Simulation should override GPS
            positionManager.updatePosition(PositionManager.PositionSource.SIMULATION, simulationPosition)
            testScheduler.runCurrent()
            assertEquals(simulationPosition, positionManager.getCurrentPosition())
            assertEquals(PositionManager.PositionSource.SIMULATION, positionManager.getCurrentSource())
        }

    @Test
    fun `should reject lower priority position updates`() =
        runTest {
            val coroutineScopeProvider = TestCoroutineScopeProvider(this)
            val positionManager = PositionManager(coroutineScopeProvider, debounceDelay = 0.milliseconds, clock = TestClock())
            val simulationPosition = Position(lat = 48.8566, lng = 2.3522)
            val gpsPosition = Position(lat = 40.7128, lng = -74.0060)

            // Set simulation position first
            positionManager.updatePosition(PositionManager.PositionSource.SIMULATION, simulationPosition)
            testScheduler.runCurrent()
            assertEquals(simulationPosition, positionManager.getCurrentPosition())

            // GPS should be rejected (lower priority)
            positionManager.updatePosition(PositionManager.PositionSource.GPS, gpsPosition)
            testScheduler.runCurrent()
            assertEquals(simulationPosition, positionManager.getCurrentPosition())
            assertEquals(PositionManager.PositionSource.SIMULATION, positionManager.getCurrentSource())
        }

    @Test
    fun `should deduplicate identical positions`() =
        runTest {
            val coroutineScopeProvider = TestCoroutineScopeProvider(this)
            val positionManager = PositionManager(coroutineScopeProvider, debounceDelay = 0.milliseconds, clock = TestClock())
            val position = Position(lat = 48.8566, lng = 2.3522)
            val almostSamePosition = Position(lat = 48.8566001, lng = 2.3522001) // Within epsilon

            // First position should be set
            positionManager.updatePosition(PositionManager.PositionSource.GPS, position)
            testScheduler.runCurrent()
            assertEquals(position, positionManager.getCurrentPosition())

            // Almost same position should be deduplicated (not accepted)
            positionManager.updatePosition(PositionManager.PositionSource.GPS, almostSamePosition)
            testScheduler.runCurrent()
            assertEquals(position, positionManager.getCurrentPosition()) // Should still be original position
        }

    @Test
    fun `should not deduplicate significantly different positions`() =
        runTest {
            val coroutineScopeProvider = TestCoroutineScopeProvider(this)
            val positionManager = PositionManager(coroutineScopeProvider, debounceDelay = 0.milliseconds, clock = TestClock())
            val position1 = Position(lat = 48.8566, lng = 2.3522)
            val position2 = Position(lat = 48.8600, lng = 2.3600) // Significantly different

            positionManager.updatePosition(PositionManager.PositionSource.GPS, position1)
            testScheduler.runCurrent()
            assertEquals(position1, positionManager.getCurrentPosition())

            positionManager.updatePosition(PositionManager.PositionSource.GPS, position2)
            testScheduler.runCurrent()
            assertEquals(position2, positionManager.getCurrentPosition()) // Should accept different position
        }

    @Test
    fun `should debounce rapid position updates`() =
        runTest {
            val coroutineScopeProvider = TestCoroutineScopeProvider(this)
            val positionManager = PositionManager(coroutineScopeProvider, debounceDelay = 100.milliseconds, clock = TestClock())
            val position1 = Position(lat = 48.8566, lng = 2.3522)
            val position2 = Position(lat = 48.8600, lng = 2.3600)
            val position3 = Position(lat = 48.8700, lng = 2.3700)

            // Rapid updates
            positionManager.updatePosition(PositionManager.PositionSource.GPS, position1)
            positionManager.updatePosition(PositionManager.PositionSource.GPS, position2)
            positionManager.updatePosition(PositionManager.PositionSource.GPS, position3)

            // Should return pending position (position3) during debounce window
            assertEquals(position3, positionManager.getCurrentPosition())

            // Advance past debounce delay
            advanceTimeBy(100.milliseconds)
            testScheduler.runCurrent()

            // Should now have the final position committed
            assertEquals(position3, positionManager.getCurrentPosition())
        }

    @Test
    fun `should return pending position during debounce window`() =
        runTest {
            val coroutineScopeProvider = TestCoroutineScopeProvider(this)
            val positionManager = PositionManager(coroutineScopeProvider, debounceDelay = 100.milliseconds, clock = TestClock())
            val gpsPosition = Position(lat = 48.8566, lng = 2.3522)

            // Update position
            positionManager.updatePosition(PositionManager.PositionSource.GPS, gpsPosition)

            // Immediately check - should return pending position
            assertEquals(gpsPosition, positionManager.getCurrentPosition())

            // Advance time but not past debounce
            advanceTimeBy(50.milliseconds)
            testScheduler.runCurrent()

            // Should still return pending position
            assertEquals(gpsPosition, positionManager.getCurrentPosition())

            // Complete debounce
            advanceTimeBy(50.milliseconds)
            testScheduler.runCurrent()

            // Should now return committed position (same value)
            assertEquals(gpsPosition, positionManager.getCurrentPosition())
        }

    @Test
    fun `should clear position from specific source`() =
        runTest {
            val coroutineScopeProvider = TestCoroutineScopeProvider(this)
            val positionManager = PositionManager(coroutineScopeProvider, debounceDelay = 0.milliseconds, clock = TestClock())
            val gpsPosition = Position(lat = 48.8566, lng = 2.3522)

            positionManager.updatePosition(PositionManager.PositionSource.GPS, gpsPosition)
            testScheduler.runCurrent()
            assertEquals(gpsPosition, positionManager.getCurrentPosition())

            positionManager.clearPosition(PositionManager.PositionSource.GPS)
            testScheduler.runCurrent()
            assertNull(positionManager.getCurrentPosition())
            assertNull(positionManager.getCurrentSource())
        }

    @Test
    fun `should clear all position data`() =
        runTest {
            val coroutineScopeProvider = TestCoroutineScopeProvider(this)
            val positionManager = PositionManager(coroutineScopeProvider, debounceDelay = 0.milliseconds, clock = TestClock())
            val position = Position(lat = 48.8566, lng = 2.3522)

            positionManager.updatePosition(PositionManager.PositionSource.SIMULATION, position)
            testScheduler.runCurrent()
            assertEquals(position, positionManager.getCurrentPosition())

            positionManager.clearAll()
            assertNull(positionManager.getCurrentPosition())
            assertNull(positionManager.getCurrentSource())
            assertEquals(null, positionManager.position.value)
        }

    @Test
    fun `should handle concurrent position updates from different sources`() =
        runTest {
            val coroutineScopeProvider = TestCoroutineScopeProvider(this)
            val positionManager = PositionManager(coroutineScopeProvider, debounceDelay = 0.milliseconds, clock = TestClock())
            val gpsPosition = Position(lat = 48.8566, lng = 2.3522)
            val simulationPosition = Position(lat = 40.7128, lng = -74.0060)

            // Concurrent updates from different sources
            positionManager.updatePosition(PositionManager.PositionSource.GPS, gpsPosition)
            positionManager.updatePosition(PositionManager.PositionSource.SIMULATION, simulationPosition)
            testScheduler.runCurrent()

            // Simulation should win (highest priority)
            assertEquals(simulationPosition, positionManager.getCurrentPosition())
            assertEquals(PositionManager.PositionSource.SIMULATION, positionManager.getCurrentSource())
        }

    @Test
    fun `should handle null position updates`() =
        runTest {
            val coroutineScopeProvider = TestCoroutineScopeProvider(this)
            val positionManager = PositionManager(coroutineScopeProvider, debounceDelay = 0.milliseconds, clock = TestClock())
            val position = Position(lat = 48.8566, lng = 2.3522)

            // Set a position first
            positionManager.updatePosition(PositionManager.PositionSource.GPS, position)
            testScheduler.runCurrent()
            assertEquals(position, positionManager.getCurrentPosition())

            // Clear with null
            positionManager.updatePosition(PositionManager.PositionSource.GPS, null)
            testScheduler.runCurrent()
            assertNull(positionManager.getCurrentPosition())
        }

    @Test
    fun `cleanup should cancel pending operations`() =
        runTest {
            val coroutineScopeProvider = TestCoroutineScopeProvider(this)
            val positionManager = PositionManager(coroutineScopeProvider, debounceDelay = 100.milliseconds, clock = TestClock())
            val position = Position(lat = 48.8566, lng = 2.3522)

            // Start a debounced update
            positionManager.updatePosition(PositionManager.PositionSource.GPS, position)

            // Cleanup before debounce completes
            positionManager.cleanup()

            // Advance past debounce delay
            advanceTimeBy(200.milliseconds)
            testScheduler.runCurrent()

            // Position should still be null (debounce was cancelled)
            assertNull(positionManager.getCurrentPosition())
        }

    @Test
    fun `should store GPS position separately even when simulation is active`() =
        runTest {
            val coroutineScopeProvider = TestCoroutineScopeProvider(this)
            val positionManager = PositionManager(coroutineScopeProvider, debounceDelay = 0.milliseconds, clock = TestClock())
            val gpsPosition = Position(lat = 48.8566, lng = 2.3522)
            val simulationPosition = Position(lat = 40.7128, lng = -74.0060)

            // Set GPS position first
            positionManager.updatePosition(PositionManager.PositionSource.GPS, gpsPosition)
            testScheduler.runCurrent()
            assertEquals(gpsPosition, positionManager.getGPSPosition())
            assertEquals(gpsPosition, positionManager.getCurrentPosition())

            // Set simulation position (higher priority)
            positionManager.updatePosition(PositionManager.PositionSource.SIMULATION, simulationPosition)
            testScheduler.runCurrent()

            // Current position should be simulation
            assertEquals(simulationPosition, positionManager.getCurrentPosition())
            assertEquals(PositionManager.PositionSource.SIMULATION, positionManager.getCurrentSource())

            // BUT GPS position should still be available
            assertEquals(gpsPosition, positionManager.getGPSPosition())
        }

    @Test
    fun `should update GPS position even when simulation is active`() =
        runTest {
            val coroutineScopeProvider = TestCoroutineScopeProvider(this)
            val positionManager = PositionManager(coroutineScopeProvider, debounceDelay = 0.milliseconds, clock = TestClock())
            val initialGPS = Position(lat = 48.8566, lng = 2.3522)
            val updatedGPS = Position(lat = 48.8600, lng = 2.3600)
            val simulationPosition = Position(lat = 40.7128, lng = -74.0060)

            // Set simulation position
            positionManager.updatePosition(PositionManager.PositionSource.SIMULATION, simulationPosition)
            testScheduler.runCurrent()
            assertEquals(simulationPosition, positionManager.getCurrentPosition())

            // Update GPS position while simulation is active
            positionManager.updatePosition(PositionManager.PositionSource.GPS, initialGPS)
            testScheduler.runCurrent()

            // GPS position should be stored
            assertEquals(initialGPS, positionManager.getGPSPosition())
            // But current position should still be simulation
            assertEquals(simulationPosition, positionManager.getCurrentPosition())

            // Update GPS again
            positionManager.updatePosition(PositionManager.PositionSource.GPS, updatedGPS)
            testScheduler.runCurrent()

            // GPS position should be updated
            assertEquals(updatedGPS, positionManager.getGPSPosition())
            // Current position still simulation
            assertEquals(simulationPosition, positionManager.getCurrentPosition())
        }

    @Test
    fun `should clear GPS position when clearAll is called`() =
        runTest {
            val coroutineScopeProvider = TestCoroutineScopeProvider(this)
            val positionManager = PositionManager(coroutineScopeProvider, debounceDelay = 0.milliseconds, clock = TestClock())
            val gpsPosition = Position(lat = 48.8566, lng = 2.3522)

            // Set GPS position
            positionManager.updatePosition(PositionManager.PositionSource.GPS, gpsPosition)
            testScheduler.runCurrent()
            assertEquals(gpsPosition, positionManager.getGPSPosition())

            // Clear all
            positionManager.clearAll()

            // GPS position should be null
            assertNull(positionManager.getGPSPosition())
            assertNull(positionManager.getCurrentPosition())
        }
}
