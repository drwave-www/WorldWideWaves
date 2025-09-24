package com.worldwidewaves.shared.events

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

import com.worldwidewaves.shared.events.utils.IClock
import io.github.aakira.napier.Antilog
import io.github.aakira.napier.LogLevel
import io.github.aakira.napier.Napier
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class WWWEventWaveTest : KoinTest {
    private var mockClock = mockk<IClock>()
    private var mockEvent = mockk<IWWWEvent>(relaxed = true)
    private lateinit var wave: WWWEventWave

    // ---------------------------

    init {
        Napier.base(
            object : Antilog() {
                override fun performLog(
                    priority: LogLevel,
                    tag: String?,
                    throwable: Throwable?,
                    message: String?,
                ) {
                    println(message)
                }
            },
        )
    }

    @BeforeTest
    fun setUp() {
        every { mockClock.now() } returns Instant.fromEpochMilliseconds(System.currentTimeMillis())
        startKoin { modules(module { single { mockClock } }) }

        wave =
            object : WWWEventWave() {
                override val speed: Double = 10.0
                override val direction: Direction = Direction.EAST
                override val approxDuration: Int = 60

                override suspend fun getWavePolygons(): WavePolygons =
                    WavePolygons(
                        clock.now(),
                        emptyList(),
                        emptyList(),
                    )

                override suspend fun getWaveDuration(): Duration = Duration.ZERO

                override suspend fun hasUserBeenHitInCurrentPosition(): Boolean = false

                override suspend fun userHitDateTime(): Instant? = null

                override suspend fun closestWaveLongitude(latitude: Double): Double = 0.0

                override suspend fun userPositionToWaveRatio() = 0.0
            }.setRelatedEvent(mockEvent)
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    // ---------------------------

    // ===== COMPREHENSIVE WAVE TYPE SPECIFIC TESTS =====

    @Test
    fun `test wave base class abstract method contracts`() {
        // GIVEN: Concrete wave implementation
        // WHEN & THEN: All abstract methods should be implemented
        assertNotNull(wave.speed, "Speed should be defined")
        assertNotNull(wave.direction, "Direction should be defined")
        assertTrue(wave.approxDuration > 0, "Approximate duration should be positive")
    }

    @Test
    fun `test wave direction enum values`() {
        // Test that all direction values are available
        val directions = WWWEventWave.Direction.values()
        assertTrue(directions.contains(WWWEventWave.Direction.EAST), "Should have EAST direction")
        assertTrue(directions.contains(WWWEventWave.Direction.WEST), "Should have WEST direction")
        assertEquals(2, directions.size, "Should have exactly 2 direction values")
    }

    @Test
    fun `test wave polygons structure`() {
        runBlocking {
            // GIVEN: Wave with polygons
            val polygons = wave.getWavePolygons()

            // WHEN & THEN: Polygons should have proper structure
            assertNotNull(polygons?.timestamp, "Timestamp should not be null")
            assertNotNull(polygons?.traversedPolygons, "Traversed polygons should not be null")
            assertNotNull(polygons?.remainingPolygons, "Remaining polygons should not be null")
        }
    }

    @Test
    fun `test wave progression bounds`() {
        runBlocking {
            // GIVEN: Wave at various states
            // WHEN: Get progression
            val progression = wave.getProgression()

            // THEN: Progression should be within valid bounds
            assertTrue(progression >= 0.0, "Progression should be non-negative")
            assertTrue(progression <= 100.0, "Progression should not exceed 100%")
        }
    }

    @Test
    fun `test user position handling when null`() {
        runBlocking {
            // GIVEN: Wave without user position
            wave.setPositionRequester { null }

            // WHEN: Get user-dependent values
            val ratio = wave.userPositionToWaveRatio()
            val timeBeforeHit = wave.timeBeforeUserHit()

            // THEN: Should handle null position gracefully
            assertEquals(0.0, ratio, "Ratio should be 0.0 when no user position")
            assertNull(timeBeforeHit, "Time before hit should be null when no user position")
        }
    }
}

// ===== COMPREHENSIVE WAVE DEEP TESTS =====

@OptIn(ExperimentalTime::class)
class WWWEventWaveDeepTest {
    private val mockEvent = mockk<IWWWEvent>(relaxed = true)
    private lateinit var waveDeep: WWWEventWaveDeep

    @BeforeTest
    fun setUp() {
        waveDeep =
            WWWEventWaveDeep(
                speed = 15.0,
                direction = WWWEventWave.Direction.EAST,
                approxDuration = 120,
            ).setRelatedEvent(mockEvent)
    }

    @Test
    fun `test deep wave initialization`() {
        // WHEN: Create deep wave
        // THEN: Properties should be set correctly
        assertEquals(15.0, waveDeep.speed, "Speed should match constructor value")
        assertEquals(WWWEventWave.Direction.EAST, waveDeep.direction, "Direction should match constructor value")
        assertEquals(120, waveDeep.approxDuration, "Duration should match constructor value")
    }

    @Test
    fun `test deep wave duration calculation`() {
        runBlocking {
            // WHEN: Get wave duration
            val duration = waveDeep.getWaveDuration()

            // THEN: Should return approximate duration (120 minutes as configured)
            assertEquals(2.hours, duration, "Deep wave duration should match approxDuration (120 minutes)")
        }
    }

    @Test
    fun `test deep wave copy functionality`() {
        // WHEN: Copy wave with different parameters
        val copiedWave = waveDeep.copy(speed = 10.0, direction = WWWEventWave.Direction.WEST)

        // THEN: Should create new instance with updated values
        assertEquals(10.0, copiedWave.speed, "Copied wave should have new speed")
        assertEquals(WWWEventWave.Direction.WEST, copiedWave.direction, "Copied wave should have new direction")
        assertEquals(120, copiedWave.approxDuration, "Copied wave should retain original duration")
    }

    @Test
    fun `test deep wave validation errors`() {
        // WHEN: Check validation
        val errors = waveDeep.validationErrors()

        // THEN: Deep wave should not have validation errors for basic setup
        assertNull(errors, "Basic deep wave setup should not have validation errors")
    }

    @Test
    fun `test deep wave TODO implementations`() {
        runBlocking {
            // Test that TODO methods throw appropriate exceptions
            try {
                waveDeep.getWavePolygons()
                assertTrue(false, "getWavePolygons should throw NotImplementedError")
            } catch (e: NotImplementedError) {
                assertTrue(true, "Expected NotImplementedError for getWavePolygons")
            }

            try {
                waveDeep.hasUserBeenHitInCurrentPosition()
                assertTrue(false, "hasUserBeenHitInCurrentPosition should throw NotImplementedError")
            } catch (e: NotImplementedError) {
                assertTrue(true, "Expected NotImplementedError for hasUserBeenHitInCurrentPosition")
            }

            try {
                waveDeep.userHitDateTime()
                assertTrue(false, "userHitDateTime should throw NotImplementedError")
            } catch (e: NotImplementedError) {
                assertTrue(true, "Expected NotImplementedError for userHitDateTime")
            }

            try {
                waveDeep.closestWaveLongitude(0.0)
                assertTrue(false, "closestWaveLongitude should throw NotImplementedError")
            } catch (e: NotImplementedError) {
                assertTrue(true, "Expected NotImplementedError for closestWaveLongitude")
            }

            try {
                waveDeep.userPositionToWaveRatio()
                assertTrue(false, "userPositionToWaveRatio should throw NotImplementedError")
            } catch (e: NotImplementedError) {
                assertTrue(true, "Expected NotImplementedError for userPositionToWaveRatio")
            }
        }
    }
}

// ===== COMPREHENSIVE WAVE LINEAR SPLIT TESTS =====

@OptIn(ExperimentalTime::class)
class WWWEventWaveLinearSplitTest {
    private val mockEvent = mockk<IWWWEvent>(relaxed = true)
    private lateinit var waveLinearSplit: WWWEventWaveLinearSplit

    @BeforeTest
    fun setUp() {
        waveLinearSplit =
            WWWEventWaveLinearSplit(
                speed = 18.0,
                direction = WWWEventWave.Direction.WEST,
                approxDuration = 90,
                nbSplits = 5,
            ).setRelatedEvent(mockEvent)
    }

    @Test
    fun `test linear split wave initialization`() {
        // WHEN: Create linear split wave
        // THEN: Properties should be set correctly
        assertEquals(18.0, waveLinearSplit.speed, "Speed should match constructor value")
        assertEquals(WWWEventWave.Direction.WEST, waveLinearSplit.direction, "Direction should match constructor value")
        assertEquals(90, waveLinearSplit.approxDuration, "Duration should match constructor value")
        assertEquals(5, waveLinearSplit.nbSplits, "Number of splits should match constructor value")
    }

    @Test
    fun `test linear split wave duration calculation`() {
        runBlocking {
            // WHEN: Get wave duration
            val duration = waveLinearSplit.getWaveDuration()

            // THEN: Should return approximate duration (90 minutes as configured)
            assertEquals(1.5.hours, duration, "Linear split wave duration should match approxDuration (90 minutes)")
        }
    }

    @Test
    fun `test linear split wave copy functionality`() {
        // WHEN: Copy wave with different parameters
        val copiedWave =
            waveLinearSplit.copy(
                speed = 12.0,
                direction = WWWEventWave.Direction.EAST,
                nbSplits = 8,
            )

        // THEN: Should create new instance with updated values
        assertEquals(12.0, copiedWave.speed, "Copied wave should have new speed")
        assertEquals(WWWEventWave.Direction.EAST, copiedWave.direction, "Copied wave should have new direction")
        assertEquals(8, copiedWave.nbSplits, "Copied wave should have new number of splits")
        assertEquals(90, copiedWave.approxDuration, "Copied wave should retain original duration")
    }

    @Test
    fun `test linear split wave validation with valid splits`() {
        // GIVEN: Wave with valid number of splits (> 2)
        val validWave =
            WWWEventWaveLinearSplit(
                speed = 8.0,
                direction = WWWEventWave.Direction.EAST,
                approxDuration = 60,
                nbSplits = 5,
            )

        // WHEN: Check validation
        val errors = validWave.validationErrors()

        // THEN: Should not have validation errors
        assertNull(errors, "Wave with valid splits should not have validation errors")
    }

    @Test
    fun `test linear split wave validation with invalid splits`() {
        // GIVEN: Wave with invalid number of splits (<= 2)
        val invalidWave =
            WWWEventWaveLinearSplit(
                speed = 14.0,
                direction = WWWEventWave.Direction.EAST,
                approxDuration = 60,
                nbSplits = 2,
            )

        // WHEN: Check validation
        val errors = invalidWave.validationErrors()

        // THEN: Should have validation error
        assertNotNull(errors, "Wave with invalid splits should have validation errors")
        assertTrue(
            errors.any { it.contains("Number of splits must be greater than 2") },
            "Should contain specific validation error message",
        )
    }

    @Test
    fun `test linear split wave validation with edge case splits`() {
        // Test edge cases for split validation
        val edgeCases =
            listOf(
                Pair(1, true), // Should be invalid
                Pair(2, true), // Should be invalid
                Pair(3, false), // Should be valid
                Pair(10, false), // Should be valid
                Pair(100, false), // Should be valid
            )

        edgeCases.forEach { (splits, shouldHaveErrors) ->
            // GIVEN: Wave with specific number of splits
            val testWave =
                WWWEventWaveLinearSplit(
                    speed = 16.0,
                    direction = WWWEventWave.Direction.EAST,
                    approxDuration = 60,
                    nbSplits = splits,
                )

            // WHEN: Check validation
            val errors = testWave.validationErrors()

            // THEN: Should match expected validation result
            if (shouldHaveErrors) {
                assertNotNull(errors, "Wave with $splits splits should have validation errors")
            } else {
                assertNull(errors, "Wave with $splits splits should not have validation errors")
            }
        }
    }

    @Test
    fun `test linear split wave TODO implementations`() {
        runBlocking {
            // Test that TODO methods throw appropriate exceptions
            try {
                waveLinearSplit.getWavePolygons()
                assertTrue(false, "getWavePolygons should throw NotImplementedError")
            } catch (e: NotImplementedError) {
                assertTrue(true, "Expected NotImplementedError for getWavePolygons")
            }

            try {
                waveLinearSplit.hasUserBeenHitInCurrentPosition()
                assertTrue(false, "hasUserBeenHitInCurrentPosition should throw NotImplementedError")
            } catch (e: NotImplementedError) {
                assertTrue(true, "Expected NotImplementedError for hasUserBeenHitInCurrentPosition")
            }

            try {
                waveLinearSplit.userHitDateTime()
                assertTrue(false, "userHitDateTime should throw NotImplementedError")
            } catch (e: NotImplementedError) {
                assertTrue(true, "Expected NotImplementedError for userHitDateTime")
            }

            try {
                waveLinearSplit.closestWaveLongitude(0.0)
                assertTrue(false, "closestWaveLongitude should throw NotImplementedError")
            } catch (e: NotImplementedError) {
                assertTrue(true, "Expected NotImplementedError for closestWaveLongitude")
            }

            try {
                waveLinearSplit.userPositionToWaveRatio()
                assertTrue(false, "userPositionToWaveRatio should throw NotImplementedError")
            } catch (e: NotImplementedError) {
                assertTrue(true, "Expected NotImplementedError for userPositionToWaveRatio")
            }
        }
    }
}
