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
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Comprehensive error handling tests for PositionManager.
 * Tests GPS/Location error scenarios, invalid coordinates, null handling, and edge cases.
 */
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
class PositionManagerErrorTest {
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

    // =============================================================================
    // Invalid Coordinate Tests
    // =============================================================================

    @Test
    fun `should accept NaN coordinates as Position allows them`() =
        runTest {
            val coroutineScopeProvider = TestCoroutineScopeProvider(this)
            val positionManager = PositionManager(coroutineScopeProvider, debounceDelay = 0.milliseconds, clock = TestClock())
            val nanPosition = Position(lat = Double.NaN, lng = Double.NaN)

            positionManager.updatePosition(PositionManager.PositionSource.GPS, nanPosition)
            testScheduler.runCurrent()

            // Position class doesn't validate, so PositionManager accepts NaN values
            assertEquals(nanPosition, positionManager.getCurrentPosition())
        }

    @Test
    fun `should accept latitude above 90 degrees as Position allows it`() =
        runTest {
            val coroutineScopeProvider = TestCoroutineScopeProvider(this)
            val positionManager = PositionManager(coroutineScopeProvider, debounceDelay = 0.milliseconds, clock = TestClock())
            val invalidPosition = Position(lat = 95.0, lng = 0.0)

            positionManager.updatePosition(PositionManager.PositionSource.GPS, invalidPosition)
            testScheduler.runCurrent()

            // Position class doesn't validate, so PositionManager accepts any value
            assertEquals(invalidPosition, positionManager.getCurrentPosition())
        }

    @Test
    fun `should accept latitude below -90 degrees as Position allows it`() =
        runTest {
            val coroutineScopeProvider = TestCoroutineScopeProvider(this)
            val positionManager = PositionManager(coroutineScopeProvider, debounceDelay = 0.milliseconds, clock = TestClock())
            val invalidPosition = Position(lat = -95.0, lng = 0.0)

            positionManager.updatePosition(PositionManager.PositionSource.GPS, invalidPosition)
            testScheduler.runCurrent()

            assertEquals(invalidPosition, positionManager.getCurrentPosition())
        }

    @Test
    fun `should accept longitude above 180 degrees as Position allows it`() =
        runTest {
            val coroutineScopeProvider = TestCoroutineScopeProvider(this)
            val positionManager = PositionManager(coroutineScopeProvider, debounceDelay = 0.milliseconds, clock = TestClock())
            val invalidPosition = Position(lat = 0.0, lng = 185.0)

            positionManager.updatePosition(PositionManager.PositionSource.GPS, invalidPosition)
            testScheduler.runCurrent()

            assertEquals(invalidPosition, positionManager.getCurrentPosition())
        }

    @Test
    fun `should accept longitude below -180 degrees as Position allows it`() =
        runTest {
            val coroutineScopeProvider = TestCoroutineScopeProvider(this)
            val positionManager = PositionManager(coroutineScopeProvider, debounceDelay = 0.milliseconds, clock = TestClock())
            val invalidPosition = Position(lat = 0.0, lng = -185.0)

            positionManager.updatePosition(PositionManager.PositionSource.GPS, invalidPosition)
            testScheduler.runCurrent()

            assertEquals(invalidPosition, positionManager.getCurrentPosition())
        }

    @Test
    fun `should accept infinite coordinates as Position allows them`() =
        runTest {
            val coroutineScopeProvider = TestCoroutineScopeProvider(this)
            val positionManager = PositionManager(coroutineScopeProvider, debounceDelay = 0.milliseconds, clock = TestClock())
            val infinitePosition = Position(lat = Double.POSITIVE_INFINITY, lng = Double.NEGATIVE_INFINITY)

            positionManager.updatePosition(PositionManager.PositionSource.GPS, infinitePosition)
            testScheduler.runCurrent()

            assertEquals(infinitePosition, positionManager.getCurrentPosition())
        }

    @Test
    fun `should accept extremely large coordinates`() =
        runTest {
            val coroutineScopeProvider = TestCoroutineScopeProvider(this)
            val positionManager = PositionManager(coroutineScopeProvider, debounceDelay = 0.milliseconds, clock = TestClock())
            val largePosition = Position(lat = 1000.0, lng = -1000.0)

            positionManager.updatePosition(PositionManager.PositionSource.GPS, largePosition)
            testScheduler.runCurrent()

            assertEquals(largePosition, positionManager.getCurrentPosition())
        }

    // =============================================================================
    // Null Handling Tests
    // =============================================================================

    @Test
    fun `should handle null position updates from GPS`() =
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
    fun `should handle null position updates from simulation`() =
        runTest {
            val coroutineScopeProvider = TestCoroutineScopeProvider(this)
            val positionManager = PositionManager(coroutineScopeProvider, debounceDelay = 0.milliseconds, clock = TestClock())
            val position = Position(lat = 40.7128, lng = -74.0060)

            positionManager.updatePosition(PositionManager.PositionSource.SIMULATION, position)
            testScheduler.runCurrent()
            assertEquals(position, positionManager.getCurrentPosition())

            positionManager.updatePosition(PositionManager.PositionSource.SIMULATION, null)
            testScheduler.runCurrent()
            assertNull(positionManager.getCurrentPosition())
        }

    @Test
    fun `should clear position when null is provided after multiple updates`() =
        runTest {
            val coroutineScopeProvider = TestCoroutineScopeProvider(this)
            val positionManager = PositionManager(coroutineScopeProvider, debounceDelay = 0.milliseconds, clock = TestClock())
            val position1 = Position(lat = 48.8566, lng = 2.3522)
            val position2 = Position(lat = 40.7128, lng = -74.0060)

            // Multiple position updates
            positionManager.updatePosition(PositionManager.PositionSource.GPS, position1)
            testScheduler.runCurrent()
            positionManager.updatePosition(PositionManager.PositionSource.GPS, position2)
            testScheduler.runCurrent()
            assertEquals(position2, positionManager.getCurrentPosition())

            // Clear
            positionManager.updatePosition(PositionManager.PositionSource.GPS, null)
            testScheduler.runCurrent()
            assertNull(positionManager.getCurrentPosition())
            assertNull(positionManager.getCurrentSource())
        }

    @Test
    fun `should accept null as initial position`() =
        runTest {
            val coroutineScopeProvider = TestCoroutineScopeProvider(this)
            val positionManager = PositionManager(coroutineScopeProvider, debounceDelay = 0.milliseconds, clock = TestClock())

            // Null update when already null
            positionManager.updatePosition(PositionManager.PositionSource.GPS, null)
            testScheduler.runCurrent()
            assertNull(positionManager.getCurrentPosition())
        }

    // =============================================================================
    // Edge Case Tests - Coordinate Boundaries
    // =============================================================================

    @Test
    fun `should handle position at exact coordinate boundaries`() =
        runTest {
            val coroutineScopeProvider = TestCoroutineScopeProvider(this)
            val positionManager = PositionManager(coroutineScopeProvider, debounceDelay = 0.milliseconds, clock = TestClock())

            // Test all four corners
            val northEast = Position(lat = 90.0, lng = 180.0)
            val northWest = Position(lat = 90.0, lng = -180.0)
            val southEast = Position(lat = -90.0, lng = 180.0)
            val southWest = Position(lat = -90.0, lng = -180.0)

            // North-East corner
            positionManager.updatePosition(PositionManager.PositionSource.GPS, northEast)
            testScheduler.runCurrent()
            assertEquals(northEast, positionManager.getCurrentPosition())

            // North-West corner
            positionManager.updatePosition(PositionManager.PositionSource.GPS, northWest)
            testScheduler.runCurrent()
            assertEquals(northWest, positionManager.getCurrentPosition())

            // South-East corner
            positionManager.updatePosition(PositionManager.PositionSource.GPS, southEast)
            testScheduler.runCurrent()
            assertEquals(southEast, positionManager.getCurrentPosition())

            // South-West corner
            positionManager.updatePosition(PositionManager.PositionSource.GPS, southWest)
            testScheduler.runCurrent()
            assertEquals(southWest, positionManager.getCurrentPosition())
        }

    @Test
    fun `should validate position at north pole`() =
        runTest {
            val coroutineScopeProvider = TestCoroutineScopeProvider(this)
            val positionManager = PositionManager(coroutineScopeProvider, debounceDelay = 0.milliseconds, clock = TestClock())
            val northPole = Position(lat = 90.0, lng = 0.0)

            positionManager.updatePosition(PositionManager.PositionSource.GPS, northPole)
            testScheduler.runCurrent()

            assertEquals(northPole, positionManager.getCurrentPosition())
            assertEquals(90.0, positionManager.getCurrentPosition()?.lat)
        }

    @Test
    fun `should validate position at south pole`() =
        runTest {
            val coroutineScopeProvider = TestCoroutineScopeProvider(this)
            val positionManager = PositionManager(coroutineScopeProvider, debounceDelay = 0.milliseconds, clock = TestClock())
            val southPole = Position(lat = -90.0, lng = 0.0)

            positionManager.updatePosition(PositionManager.PositionSource.GPS, southPole)
            testScheduler.runCurrent()

            assertEquals(southPole, positionManager.getCurrentPosition())
            assertEquals(-90.0, positionManager.getCurrentPosition()?.lat)
        }

    @Test
    fun `should handle position at equator`() =
        runTest {
            val coroutineScopeProvider = TestCoroutineScopeProvider(this)
            val positionManager = PositionManager(coroutineScopeProvider, debounceDelay = 0.milliseconds, clock = TestClock())
            val equator = Position(lat = 0.0, lng = 0.0)

            positionManager.updatePosition(PositionManager.PositionSource.GPS, equator)
            testScheduler.runCurrent()

            assertEquals(equator, positionManager.getCurrentPosition())
            assertEquals(0.0, positionManager.getCurrentPosition()?.lat)
        }

    @Test
    fun `should handle position at prime meridian`() =
        runTest {
            val coroutineScopeProvider = TestCoroutineScopeProvider(this)
            val positionManager = PositionManager(coroutineScopeProvider, debounceDelay = 0.milliseconds, clock = TestClock())
            val primeMeridian = Position(lat = 51.4779, lng = 0.0)

            positionManager.updatePosition(PositionManager.PositionSource.GPS, primeMeridian)
            testScheduler.runCurrent()

            assertEquals(primeMeridian, positionManager.getCurrentPosition())
            assertEquals(0.0, positionManager.getCurrentPosition()?.lng)
        }

    @Test
    fun `should handle position at international date line`() =
        runTest {
            val coroutineScopeProvider = TestCoroutineScopeProvider(this)
            val positionManager = PositionManager(coroutineScopeProvider, debounceDelay = 0.milliseconds, clock = TestClock())
            val dateLine180 = Position(lat = 0.0, lng = 180.0)
            val dateLineNeg180 = Position(lat = 0.0, lng = -180.0)

            positionManager.updatePosition(PositionManager.PositionSource.GPS, dateLine180)
            testScheduler.runCurrent()
            assertEquals(dateLine180, positionManager.getCurrentPosition())

            positionManager.updatePosition(PositionManager.PositionSource.GPS, dateLineNeg180)
            testScheduler.runCurrent()
            assertEquals(dateLineNeg180, positionManager.getCurrentPosition())
        }

    // =============================================================================
    // Error Recovery Tests
    // =============================================================================

    @Test
    fun `should recover from invalid position by accepting valid one`() =
        runTest {
            val coroutineScopeProvider = TestCoroutineScopeProvider(this)
            val positionManager = PositionManager(coroutineScopeProvider, debounceDelay = 0.milliseconds, clock = TestClock())
            val invalidPosition = Position(lat = Double.NaN, lng = Double.NaN)
            val validPosition = Position(lat = 48.8566, lng = 2.3522)

            // Set invalid position
            positionManager.updatePosition(PositionManager.PositionSource.GPS, invalidPosition)
            testScheduler.runCurrent()
            assertEquals(invalidPosition, positionManager.getCurrentPosition())

            // Recover with valid position
            positionManager.updatePosition(PositionManager.PositionSource.GPS, validPosition)
            testScheduler.runCurrent()
            assertEquals(validPosition, positionManager.getCurrentPosition())
        }

    @Test
    fun `should clear invalid position with null`() =
        runTest {
            val coroutineScopeProvider = TestCoroutineScopeProvider(this)
            val positionManager = PositionManager(coroutineScopeProvider, debounceDelay = 0.milliseconds, clock = TestClock())
            val invalidPosition = Position(lat = 1000.0, lng = -1000.0)

            positionManager.updatePosition(PositionManager.PositionSource.GPS, invalidPosition)
            testScheduler.runCurrent()
            assertEquals(invalidPosition, positionManager.getCurrentPosition())

            positionManager.updatePosition(PositionManager.PositionSource.GPS, null)
            testScheduler.runCurrent()
            assertNull(positionManager.getCurrentPosition())
        }

    @Test
    fun `should handle alternating valid and invalid positions`() =
        runTest {
            val coroutineScopeProvider = TestCoroutineScopeProvider(this)
            val positionManager = PositionManager(coroutineScopeProvider, debounceDelay = 0.milliseconds, clock = TestClock())
            val validPosition = Position(lat = 48.8566, lng = 2.3522)
            val invalidPosition = Position(lat = Double.NaN, lng = Double.NaN)

            // Valid -> Invalid -> Valid
            positionManager.updatePosition(PositionManager.PositionSource.GPS, validPosition)
            testScheduler.runCurrent()
            assertEquals(validPosition, positionManager.getCurrentPosition())

            positionManager.updatePosition(PositionManager.PositionSource.GPS, invalidPosition)
            testScheduler.runCurrent()
            assertEquals(invalidPosition, positionManager.getCurrentPosition())

            positionManager.updatePosition(PositionManager.PositionSource.GPS, validPosition)
            testScheduler.runCurrent()
            assertEquals(validPosition, positionManager.getCurrentPosition())
        }

    // =============================================================================
    // Source Priority with Invalid Data Tests
    // =============================================================================

    @Test
    fun `should prioritize simulation over GPS even with invalid coordinates`() =
        runTest {
            val coroutineScopeProvider = TestCoroutineScopeProvider(this)
            val positionManager = PositionManager(coroutineScopeProvider, debounceDelay = 0.milliseconds, clock = TestClock())
            val validGPS = Position(lat = 48.8566, lng = 2.3522)
            val invalidSimulation = Position(lat = Double.NaN, lng = Double.NaN)

            positionManager.updatePosition(PositionManager.PositionSource.GPS, validGPS)
            testScheduler.runCurrent()
            assertEquals(validGPS, positionManager.getCurrentPosition())

            // Even invalid simulation should override GPS due to priority
            positionManager.updatePosition(PositionManager.PositionSource.SIMULATION, invalidSimulation)
            testScheduler.runCurrent()
            assertEquals(invalidSimulation, positionManager.getCurrentPosition())
            assertEquals(PositionManager.PositionSource.SIMULATION, positionManager.getCurrentSource())
        }

    @Test
    fun `should reject lower priority GPS after invalid simulation position`() =
        runTest {
            val coroutineScopeProvider = TestCoroutineScopeProvider(this)
            val positionManager = PositionManager(coroutineScopeProvider, debounceDelay = 0.milliseconds, clock = TestClock())
            val invalidSimulation = Position(lat = 1000.0, lng = -1000.0)
            val validGPS = Position(lat = 48.8566, lng = 2.3522)

            positionManager.updatePosition(PositionManager.PositionSource.SIMULATION, invalidSimulation)
            testScheduler.runCurrent()
            assertEquals(invalidSimulation, positionManager.getCurrentPosition())

            // GPS should be rejected
            positionManager.updatePosition(PositionManager.PositionSource.GPS, validGPS)
            testScheduler.runCurrent()
            assertEquals(invalidSimulation, positionManager.getCurrentPosition())
            assertEquals(PositionManager.PositionSource.SIMULATION, positionManager.getCurrentSource())
        }

    // =============================================================================
    // Cleanup and State Management Tests
    // =============================================================================

    @Test
    fun `should handle clearAll after invalid position`() =
        runTest {
            val coroutineScopeProvider = TestCoroutineScopeProvider(this)
            val positionManager = PositionManager(coroutineScopeProvider, debounceDelay = 0.milliseconds, clock = TestClock())
            val invalidPosition = Position(lat = Double.NaN, lng = Double.NaN)

            positionManager.updatePosition(PositionManager.PositionSource.GPS, invalidPosition)
            testScheduler.runCurrent()
            assertEquals(invalidPosition, positionManager.getCurrentPosition())

            positionManager.clearAll()
            assertNull(positionManager.getCurrentPosition())
            assertNull(positionManager.getCurrentSource())
        }

    @Test
    fun `should handle cleanup with pending invalid position update`() =
        runTest {
            val coroutineScopeProvider = TestCoroutineScopeProvider(this)
            val positionManager = PositionManager(coroutineScopeProvider, debounceDelay = 100.milliseconds)
            val invalidPosition = Position(lat = Double.NaN, lng = Double.NaN)

            // Start a debounced update with invalid position
            positionManager.updatePosition(PositionManager.PositionSource.GPS, invalidPosition)

            // Cleanup before debounce completes
            positionManager.cleanup()
            testScheduler.runCurrent()

            // Position should still be null (debounce was cancelled)
            assertNull(positionManager.getCurrentPosition())
        }

    // =============================================================================
    // Deduplication with Edge Cases
    // =============================================================================

    @Test
    fun `should not deduplicate NaN positions`() =
        runTest {
            val coroutineScopeProvider = TestCoroutineScopeProvider(this)
            val positionManager = PositionManager(coroutineScopeProvider, debounceDelay = 0.milliseconds, clock = TestClock())
            val nanPosition1 = Position(lat = Double.NaN, lng = Double.NaN)
            val nanPosition2 = Position(lat = Double.NaN, lng = Double.NaN)

            positionManager.updatePosition(PositionManager.PositionSource.GPS, nanPosition1)
            testScheduler.runCurrent()
            assertEquals(nanPosition1, positionManager.getCurrentPosition())

            // NaN != NaN, so deduplication should not occur
            positionManager.updatePosition(PositionManager.PositionSource.GPS, nanPosition2)
            testScheduler.runCurrent()
            // NaN comparison returns false, so should accept second position
            assertEquals(nanPosition2, positionManager.getCurrentPosition())
        }

    @Test
    fun `should handle zero coordinates`() =
        runTest {
            val coroutineScopeProvider = TestCoroutineScopeProvider(this)
            val positionManager = PositionManager(coroutineScopeProvider, debounceDelay = 0.milliseconds, clock = TestClock())
            val zeroPosition = Position(lat = 0.0, lng = 0.0)

            positionManager.updatePosition(PositionManager.PositionSource.GPS, zeroPosition)
            testScheduler.runCurrent()

            assertEquals(zeroPosition, positionManager.getCurrentPosition())
            assertEquals(0.0, positionManager.getCurrentPosition()?.lat)
            assertEquals(0.0, positionManager.getCurrentPosition()?.lng)
        }

    @Test
    fun `should handle negative zero coordinates`() =
        runTest {
            val coroutineScopeProvider = TestCoroutineScopeProvider(this)
            val positionManager = PositionManager(coroutineScopeProvider, debounceDelay = 0.milliseconds, clock = TestClock())
            val negZeroPosition = Position(lat = -0.0, lng = -0.0)

            positionManager.updatePosition(PositionManager.PositionSource.GPS, negZeroPosition)
            testScheduler.runCurrent()

            assertEquals(negZeroPosition, positionManager.getCurrentPosition())
        }

    @Test
    fun `should deduplicate positions at exact coordinate boundaries`() =
        runTest {
            val coroutineScopeProvider = TestCoroutineScopeProvider(this)
            val positionManager = PositionManager(coroutineScopeProvider, debounceDelay = 0.milliseconds, clock = TestClock())
            val northPole1 = Position(lat = 90.0, lng = 0.0)
            val northPole2 = Position(lat = 90.00000001, lng = 0.00000001) // Within epsilon

            positionManager.updatePosition(PositionManager.PositionSource.GPS, northPole1)
            testScheduler.runCurrent()
            assertEquals(northPole1, positionManager.getCurrentPosition())

            // Should be deduplicated
            positionManager.updatePosition(PositionManager.PositionSource.GPS, northPole2)
            testScheduler.runCurrent()
            assertEquals(northPole1, positionManager.getCurrentPosition()) // Should still be original
        }
}
