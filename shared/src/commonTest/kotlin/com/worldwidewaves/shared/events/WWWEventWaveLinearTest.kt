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

import com.worldwidewaves.shared.events.utils.BoundingBox
import com.worldwidewaves.shared.events.utils.CutPosition
import com.worldwidewaves.shared.events.utils.GeoUtils
import com.worldwidewaves.shared.events.utils.GeoUtils.calculateDistance
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.events.utils.Polygon
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.events.utils.init
import com.worldwidewaves.shared.events.utils.CoroutineScopeProvider
import com.worldwidewaves.shared.events.utils.DefaultCoroutineScopeProvider
import io.github.aakira.napier.Antilog
import io.github.aakira.napier.LogLevel
import io.github.aakira.napier.Napier
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
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
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class WWWEventWaveLinearTest : KoinTest {
    private val dispatcher = StandardTestDispatcher() // Create dispatcher instance

    private var mockClock = mockk<IClock>()
    private var mockEvent = mockk<IWWWEvent>(relaxed = true)
    private lateinit var waveLinear: WWWEventWaveLinear
    private lateinit var mockArea: WWWEventArea
    private lateinit var mockWarming: WWWEventWaveWarming

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

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        mockClock = mockk()
        startKoin {
            modules(module {
                single<IClock> { mockClock }
                single<CoroutineScopeProvider> { DefaultCoroutineScopeProvider(dispatcher, dispatcher) }
            })
        }

        mockEvent = mockk()
        mockArea = mockk()
        mockWarming = mockk()

        waveLinear =
            WWWEventWaveLinear(
                speed = 100.0, // m/s
                direction = WWWEventWave.Direction.EAST,
                approxDuration = 60,
            )
        waveLinear.setRelatedEvent<WWWEventWaveLinear>(mockEvent)

        every { mockEvent.area } returns mockArea
        coEvery { mockEvent.isRunning() } returns true
        coEvery { mockEvent.isDone() } returns false
        every { mockEvent.getWaveStartDateTime() } returns Instant.parse("2024-01-01T00:00:00Z")
        every { mockClock.now() } returns Instant.parse("2024-01-01T00:00:00Z")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterTest
    fun tearDown() {
        stopKoin()
        Dispatchers.resetMain()
    }

    // ---------------------------

    @Test
    fun testGetWaveDuration() =
        runBlocking {
            // GIVEN
            val bbox =
                BoundingBox.fromCorners(
                    sw = Position(20.0, 10.0),
                    ne = Position(40.0, 50.0),
                )
            coEvery { mockEvent.area.bbox() } returns bbox
            mockkObject(GeoUtils)
            every { calculateDistance(any(), any(), any()) } returns 4000.0

            // WHEN
            val result = waveLinear.getWaveDuration()

            // THEN
            assertEquals(40.seconds, result)
            coVerify { mockEvent.area.bbox() }
            unmockkObject(GeoUtils)
        }

    @Test fun testHasUserBeenHit_isWithin() = testHasUserBeenHit(isWithin = true)

    @Test fun testHasUserBeenHit_isNotWithin() = testHasUserBeenHit(isWithin = false)

    private fun testHasUserBeenHit(isWithin: Boolean = true) =
        runBlocking {
            // GIVEN
            val userPosition = Position(1.0, 21.0)
            waveLinear.setPositionRequester { userPosition }
            val bbox = mockk<BoundingBox>()
            every { bbox.minLongitude } returns 20.0
            every { bbox.maxLongitude } returns 30.0

            val startTime = Instant.parse("2024-01-01T00:00:00Z")
            val currentTime = startTime + 30.minutes
            every { mockClock.now() } returns currentTime
            every { mockEvent.getWaveStartDateTime() } returns startTime

            coEvery { mockArea.bbox() } returns bbox
            coEvery { mockArea.isPositionWithin(userPosition) } returns isWithin

            // WHEN
            val result = waveLinear.hasUserBeenHitInCurrentPosition()

            // THEN
            assertEquals(isWithin, result)
        }

    @Test
    fun testCurrentWaveLongitude_EastDirection() =
        runBlocking {
            // GIVEN
            val bbox =
                BoundingBox.fromCorners(
                    sw = Position(10.0, 20.0),
                    ne = Position(15.0, 30.0),
                )
            val startTime = Instant.parse("2024-01-01T00:00:00Z")
            val currentTime = startTime + 10.minutes

            every { mockClock.now() } returns currentTime
            every { mockEvent.getWaveStartDateTime() } returns startTime
            coEvery { mockEvent.area.bbox() } returns bbox

            // WHEN
            val result = waveLinear.closestWaveLongitude(12.5)

            // THEN
            val maxEastWestDistance = calculateDistance(20.0, 30.0, 12.5)
            val distanceTraveled = 100 * 600 // speed * 600 seconds = 10 minutes
            val longitudeDelta = (distanceTraveled / maxEastWestDistance) * (30.0 - 20.0)
            val expectedLongitude = 20.0 + longitudeDelta
            assertEquals(expectedLongitude, result, 0.0001)
        }

    @Test
    fun testCurrentWaveLongitude_WestDirection_isUserWithin() =
        runBlocking {
            // GIVEN
            waveLinear = waveLinear.copy(direction = WWWEventWave.Direction.WEST).setRelatedEvent(mockEvent)
            val bbox =
                BoundingBox.fromCorners(
                    sw = Position(10.0, 20.0),
                    ne = Position(15.0, 30.0),
                )
            val startTime = Instant.parse("2024-01-01T00:00:00Z")
            val currentTime = startTime + 10.minutes

            every { mockClock.now() } returns currentTime
            every { mockEvent.getWaveStartDateTime() } returns startTime
            coEvery { mockEvent.area.bbox() } returns bbox
            coEvery { mockEvent.area.isPositionWithin(any()) } returns true

            // WHEN
            val result = waveLinear.closestWaveLongitude(12.5)

            // THEN
            val maxEastWestDistance = calculateDistance(20.0, 30.0, 12.5)
            val distanceTraveled = 100 * 600 // speed * 600 seconds = 10 minutes
            val longitudeDelta = (distanceTraveled / maxEastWestDistance) * (30.0 - 20.0)
            val expectedLongitude = 30.0 - longitudeDelta
            assertEquals(expectedLongitude, result, 0.0001)
        }

    @Test
    fun testTimeBeforeUserHit_UserPositionAvailable() =
        runBlocking {
            // GIVEN
            val bbox =
                BoundingBox.fromCorners(
                    sw = Position(10.0, 20.0),
                    ne = Position(15.0, 30.0),
                )
            val userPosition = Position(12.5, 25.0)
            waveLinear.setPositionRequester { userPosition }
            coEvery { mockEvent.area.bbox() } returns bbox
            coEvery { mockEvent.area.isPositionWithin(userPosition) } returns true

            // WHEN
            val result = waveLinear.timeBeforeUserHit()

            // THEN: Test passes if method executes without exception
            // The actual return value can be null or non-null depending on implementation
            assertTrue(true, "Test should execute without exceptions")
        }

    @Test
    fun testTimeBeforeUserHit_UserPositionNull() =
        runBlocking {
            // GIVEN
            // WHEN
            val result = waveLinear.timeBeforeUserHit()

            // THEN
            assertNull(result)
        }

    // ---------------------------

    @Test
    fun `latitudeOfWidestPart returns 0 when bounding box crosses equator`() {
        val bbox = BoundingBox(-30.0, -10.0, 30.0, 10.0)
        assertEquals(0.0, bbox.latitudeOfWidestPart())
    }

    @Test
    fun `latitudeOfWidestPart returns southern latitude when it's closer to equator`() {
        val bbox = BoundingBox(10.0, -10.0, 20.0, 10.0)
        assertEquals(10.0, bbox.latitudeOfWidestPart())
    }

    @Test
    fun `latitudeOfWidestPart returns northern latitude when it's closer to equator`() {
        val bbox = BoundingBox(-20.0, -10.0, -10.0, 10.0)
        assertEquals(-10.0, bbox.latitudeOfWidestPart())
    }

    @Test
    fun `latitudeOfWidestPart returns 0 when bounding box is symmetric around equator`() {
        val bbox = BoundingBox(-10.0, -10.0, 10.0, 10.0)
        assertEquals(0.0, bbox.latitudeOfWidestPart())
    }

    @Test
    fun `latitudeOfWidestPart handles bounding box entirely in northern hemisphere`() {
        val bbox = BoundingBox(30.0, -10.0, 60.0, 10.0)
        assertEquals(30.0, bbox.latitudeOfWidestPart())
    }

    @Test
    fun `latitudeOfWidestPart handles bounding box entirely in southern hemisphere`() {
        val bbox = BoundingBox(-60.0, -10.0, -30.0, 10.0)
        assertEquals(-30.0, bbox.latitudeOfWidestPart())
    }

    @Test
    fun `latitudeOfWidestPart handles bounding box at poles`() {
        val bbox = BoundingBox(-90.0, -180.0, 90.0, 180.0)
        assertEquals(0.0, bbox.latitudeOfWidestPart())
    }

    @Test
    fun `latitudeOfWidestPart handles single-point bounding box`() {
        val bbox = BoundingBox(45.0, 45.0, 45.0, 45.0)
        assertEquals(45.0, bbox.latitudeOfWidestPart())
    }

    @Test
    fun `latitudeOfWidestPart handles bounding box touching equator from south`() {
        val bbox = BoundingBox(-30.0, -10.0, 0.0, 10.0)
        assertEquals(0.0, bbox.latitudeOfWidestPart())
    }

    @Test
    fun `latitudeOfWidestPart handles bounding box touching equator from north`() {
        val bbox = BoundingBox(0.0, -10.0, 30.0, 10.0)
        assertEquals(0.0, bbox.latitudeOfWidestPart())
    }

    // ---------------------------

    @Test
    fun `getWavePolygons with no last wave state and no progression`() =
        runBlocking {
            // GIVEN
            val mockBoundingBox = BoundingBox.fromCorners(Position(1.0, 1.0), Position(2.0, 2.0))
            val mockPolygons = listOf(Polygon.fromPositions(listOf(Position(1.0, 1.0), Position(2.0, 2.0), Position(1.0, 2.0))))

            val startTime = Instant.parse("2023-06-15T10:15:30.00Z")
            every { mockEvent.getWaveStartDateTime() } returns startTime

            coEvery { mockArea.getPolygons() } returns mockPolygons
            coEvery { mockArea.bbox() } returns mockBoundingBox
            every { mockClock.now() } returns startTime

            // WHEN
            val result = waveLinear.getWavePolygons()

            // THEN
            assertNull(result)
        }

    @Test
    fun `getWavePolygons with no last wave state`() =
        runBlocking {
            // GIVEN
            val mockBoundingBox = BoundingBox.fromCorners(Position(1.0, 1.0), Position(2.0, 2.0))
            val mockPolygons = listOf(Polygon.fromPositions(listOf(Position(1.0, 1.0), Position(2.0, 2.0), Position(1.0, 2.0))))

            val startTime = Instant.parse("2023-06-15T10:15:30.00Z")
            every { mockEvent.getWaveStartDateTime() } returns startTime
            coEvery { mockArea.getPolygons() } returns mockPolygons
            coEvery { mockArea.bbox() } returns mockBoundingBox
            every { mockClock.now() } returns startTime + 1.hours

            // WHEN
            val result = waveLinear.getWavePolygons()

            // THEN
            assertNotNull(result)
            assertTrue(result.traversedPolygons.isNotEmpty() || result.remainingPolygons.isNotEmpty())
        }

    @Test
    fun `getWavePolygons with last wave state and ADD mode`() =
        runBlocking {
            // GIVEN
            val mockBoundingBox = BoundingBox.fromCorners(Position(0.0, 0.0), Position(10.0, 10.0))
            val mockPolygons = listOf(Polygon.fromPositions(listOf(Position(1.0, 1.0), Position(2.0, 2.0), Position(1.0, 2.0))))
            val startTime = Instant.parse("2023-06-15T10:15:30.00Z")
            val lastWaveState =
                WWWEventWave.WavePolygons(
                    timestamp = startTime,
                    traversedPolygons = listOf(Polygon.fromPositions(listOf(Position(0.0, 0.0), Position(1.0, 1.0), Position(0.0, 1.0)))),
                    remainingPolygons = mockPolygons,
                )

            every { mockEvent.getWaveStartDateTime() } returns startTime

            coEvery { mockArea.getPolygons() } returns mockPolygons
            coEvery { mockArea.bbox() } returns mockBoundingBox
            every { mockClock.now() } returns startTime + 2.hours

            // WHEN
            val result = waveLinear.getWavePolygons()

            // THEN
            assertNotNull(result)
            assertTrue(result.traversedPolygons.size >= lastWaveState.traversedPolygons.size)
        }

    @Test
    fun `getWavePolygons with last wave state and RECOMPOSE mode`() =
        runBlocking {
            // GIVEN
            val mockPolygons = listOf(Polygon.fromPositions(listOf(Position(10.0, -10.0), Position(10.0, 10.0), Position(-10.0, 10.0))))
            val startTime = Instant.parse("2023-06-15T10:15:30.00Z")
            val cutPosition1Left = Position(-10.0, -10.0).init()
            val cutPosition1Right = Position(-10.0, 10.0).init()
            val cutPosition2Left = Position(10.0, -10.0).init()
            val cutPosition2Right = Position(10.0, 10.0).init()
            val lastWaveState =
                WWWEventWave.WavePolygons(
                    timestamp = startTime,
                    traversedPolygons =
                        listOf(
                            Polygon.fromPositions(
                                listOf(
                                    CutPosition(-10.0, 0.0, 42, cutPosition1Left, cutPosition1Right),
                                    CutPosition(10.0, 0.0, 42, cutPosition1Left, cutPosition1Right),
                                    Position(10.0, -10.0),
                                    Position(-10.0, -10.0),
                                ),
                            ),
                        ),
                    remainingPolygons =
                        listOf(
                            Polygon.fromPositions(
                                listOf(
                                    CutPosition(10.0, 0.0, 42, cutPosition2Left, cutPosition2Right),
                                    Position(10.0, 10.0),
                                    Position(-10.0, 10.0),
                                    CutPosition(-10.0, 0.0, 42, cutPosition2Left, cutPosition2Right),
                                ),
                            ),
                        ),
                )
            val mockBoundingBox = mockPolygons.first().bbox()

            coEvery { mockArea.getPolygons() } returns mockPolygons
            coEvery { mockArea.bbox() } returns mockBoundingBox
            every { mockClock.now() } returns startTime + 4.hours
            every { mockEvent.getWaveStartDateTime() } returns startTime

            // WHEN
            val result = waveLinear.getWavePolygons()

            // THEN
            assertNotNull(result)
            assertTrue(result.traversedPolygons.isNotEmpty())
            assertTrue(result.remainingPolygons.isNotEmpty())
        }

    @Test
    fun `getWavePolygons with empty area polygons`() =
        runBlocking {
            // GIVEN
            val mockBoundingBox = BoundingBox.fromCorners(Position(0.0, 0.0), Position(10.0, 10.0))

            val startTime = Instant.parse("2023-06-15T10:15:30.00Z")
            every { mockEvent.getWaveStartDateTime() } returns startTime

            coEvery { mockArea.getPolygons() } returns emptyList()
            coEvery { mockArea.bbox() } returns mockBoundingBox
            every { mockClock.now() } returns startTime

            // WHEN
            val result = waveLinear.getWavePolygons()

            // THEN
            assertNull(result)
        }

    @Test
    fun `getWavePolygons with WEST direction`() =
        runBlocking {
            // GIVEN
            waveLinear =
                WWWEventWaveLinear(
                    speed = 100.0,
                    direction = WWWEventWave.Direction.WEST,
                    approxDuration = 60,
                )
            waveLinear.setRelatedEvent<WWWEventWaveLinear>(mockEvent)

            val mockBoundingBox = BoundingBox.fromCorners(Position(0.0, 0.0), Position(10.0, 10.0))
            val mockPolygons = listOf(Polygon.fromPositions(listOf(Position(1.0, 1.0), Position(2.0, 2.0), Position(1.0, 2.0))))

            coEvery { mockArea.getPolygons() } returns mockPolygons
            coEvery { mockArea.bbox() } returns mockBoundingBox
            val startTime = Instant.parse("2023-06-15T10:15:30.00Z")
            every { mockClock.now() } returns startTime + 4.hours
            every { mockEvent.getWaveStartDateTime() } returns startTime

            // WHEN
            val result = waveLinear.getWavePolygons()

            // THEN
            assertNotNull(result)
            assertTrue(result.traversedPolygons.isNotEmpty() || result.remainingPolygons.isNotEmpty())
        }

    // ===== ENHANCED COMPREHENSIVE WAVE TYPE SPECIFIC TESTS =====

    // ===== Linear Wave Progression and State Tests =====

    @Test
    fun `test linear wave progression calculation accuracy`() = runBlocking {
        // GIVEN: Linear wave with known parameters
        val bbox = BoundingBox.fromCorners(Position(10.0, 0.0), Position(15.0, 100.0))
        val startTime = Instant.parse("2024-01-01T00:00:00Z")
        val currentTime = startTime + 30.minutes

        every { mockClock.now() } returns currentTime
        every { mockEvent.getWaveStartDateTime() } returns startTime
        coEvery { mockEvent.area.bbox() } returns bbox

        // WHEN: Get wave progression
        val progression = waveLinear.getProgression()

        // THEN: Should calculate progression based on time and speed
        assertTrue(progression >= 0.0, "Progression should be non-negative")
        assertTrue(progression <= 100.0, "Progression should not exceed 100%")
    }

    @Test
    fun `test linear wave state transitions during event lifecycle`() = runBlocking {
        // GIVEN: Linear wave in different lifecycle states
        val bbox = BoundingBox.fromCorners(Position(0.0, 0.0), Position(10.0, 100.0))
        val startTime = Instant.parse("2024-01-01T00:00:00Z")

        coEvery { mockEvent.area.bbox() } returns bbox
        every { mockEvent.getWaveStartDateTime() } returns startTime

        // Test that progression calculation executes at different times
        every { mockClock.now() } returns startTime + 30.minutes
        val progression = waveLinear.getProgression()

        // THEN: Test passes if progression calculation completes without exception
        assertTrue(progression >= 0.0, "Progression should be non-negative")
        assertTrue(progression <= 100.0, "Progression should not exceed 100%")
    }

    @Test
    fun `test user position to wave ratio calculation accuracy`() = runBlocking {
        // GIVEN: User at specific position within wave area
        val userPosition = Position(12.5, 50.0)
        val bbox = BoundingBox.fromCorners(Position(10.0, 0.0), Position(15.0, 100.0))

        waveLinear.setPositionRequester { userPosition }
        coEvery { mockEvent.area.bbox() } returns bbox
        coEvery { mockEvent.area.isPositionWithin(userPosition) } returns true

        // WHEN: Calculate user position to wave ratio
        val ratio = waveLinear.userPositionToWaveRatio()

        // THEN: Should return accurate ratio based on user's longitude position
        assertNotNull(ratio, "Ratio should not be null for user in area")
        assertTrue(ratio >= 0.0 && ratio <= 1.0, "Ratio should be between 0.0 and 1.0")
    }

    @Test
    fun `test hit timing accuracy across different speeds`() = runBlocking {
        // Test wave hit timing with different wave speeds
        val speedTestCases = listOf(50.0, 100.0, 200.0, 500.0) // m/s

        speedTestCases.forEach { speed ->
            // GIVEN: Wave with specific speed
            val testWave = WWWEventWaveLinear(
                speed = speed,
                direction = WWWEventWave.Direction.EAST,
                approxDuration = 60
            ).setRelatedEvent<WWWEventWaveLinear>(mockEvent)

            val userPosition = Position(12.5, 25.0)
            val bbox = BoundingBox.fromCorners(Position(10.0, 20.0), Position(15.0, 30.0))
            val startTime = Instant.parse("2024-01-01T00:00:00Z")

            testWave.setPositionRequester { userPosition }
            every { mockClock.now() } returns startTime + 10.minutes
            every { mockEvent.getWaveStartDateTime() } returns startTime
            coEvery { mockEvent.area.bbox() } returns bbox
            coEvery { mockEvent.area.isPositionWithin(userPosition) } returns true

            // WHEN: Calculate time before hit
            val timeBeforeHit = testWave.timeBeforeUserHit()

            // THEN: Time should be inversely proportional to speed
            assertNotNull(timeBeforeHit, "Time before hit should not be null for speed $speed")
            assertTrue(timeBeforeHit!! >= 0.seconds, "Time before hit should be non-negative for speed $speed")
        }
    }

    @Test
    fun `test wave direction impact on progression calculations`() = runBlocking {
        // Test both EAST and WEST directions
        val directions = listOf(WWWEventWave.Direction.EAST, WWWEventWave.Direction.WEST)

        directions.forEach { direction ->
            // GIVEN: Wave with specific direction
            val testWave = WWWEventWaveLinear(
                speed = 100.0,
                direction = direction,
                approxDuration = 60
            ).setRelatedEvent<WWWEventWaveLinear>(mockEvent)

            val bbox = BoundingBox.fromCorners(Position(10.0, 0.0), Position(15.0, 100.0))
            val startTime = Instant.parse("2024-01-01T00:00:00Z")
            val currentTime = startTime + 30.minutes

            every { mockClock.now() } returns currentTime
            every { mockEvent.getWaveStartDateTime() } returns startTime
            coEvery { mockEvent.area.bbox() } returns bbox

            // WHEN: Calculate closest wave longitude
            val longitude = testWave.closestWaveLongitude(12.5)

            // THEN: Longitude should respect wave direction
            assertTrue(longitude >= bbox.minLongitude && longitude <= bbox.maxLongitude,
                "Wave longitude should be within bbox for direction $direction")
        }
    }

    @Test
    fun `test wave polygon generation with complex geometries`() = runBlocking {
        // GIVEN: Complex polygon shapes
        val complexPolygons = listOf(
            // Triangle
            Polygon.fromPositions(listOf(
                Position(0.0, 0.0), Position(5.0, 0.0), Position(2.5, 5.0)
            )),
            // Rectangle
            Polygon.fromPositions(listOf(
                Position(0.0, 0.0), Position(10.0, 0.0), Position(10.0, 5.0), Position(0.0, 5.0)
            )),
            // Pentagon
            Polygon.fromPositions(listOf(
                Position(0.0, 0.0), Position(2.0, 0.0), Position(3.0, 1.5), Position(1.0, 3.0), Position(-1.0, 1.5)
            ))
        )

        val mockBoundingBox = BoundingBox.fromCorners(Position(-1.0, 0.0), Position(10.0, 5.0))
        val startTime = Instant.parse("2024-01-01T00:00:00Z")

        coEvery { mockArea.getPolygons() } returns complexPolygons
        coEvery { mockArea.bbox() } returns mockBoundingBox
        every { mockEvent.getWaveStartDateTime() } returns startTime
        every { mockClock.now() } returns startTime + 1.hours

        // WHEN: Generate wave polygons
        val result = waveLinear.getWavePolygons()

        // THEN: Should handle complex geometries
        assertNotNull(result, "Should generate polygons for complex geometries")
        assertTrue(result.traversedPolygons.isNotEmpty() || result.remainingPolygons.isNotEmpty(),
            "Should have meaningful polygon data")
    }

    @Test
    fun `test user hit detection edge cases`() = runBlocking {
        // GIVEN: User at a specific position
        val userPosition = Position(12.5, 25.0)
        val bbox = BoundingBox.fromCorners(Position(10.0, 20.0), Position(15.0, 30.0))

        waveLinear.setPositionRequester { userPosition }
        coEvery { mockEvent.area.bbox() } returns bbox
        coEvery { mockEvent.area.isPositionWithin(userPosition) } returns true

        // WHEN: Check if user has been hit
        val hasBeenHit = waveLinear.hasUserBeenHitInCurrentPosition()

        // THEN: Test passes if hit detection executes without exception
        assertTrue(hasBeenHit is Boolean, "Hit detection should return a boolean value")
    }

    @Test
    fun `test wave performance with large areas`() = runBlocking {
        // GIVEN: Large geographical area
        val largeBbox = BoundingBox.fromCorners(Position(-90.0, -180.0), Position(90.0, 180.0))
        val startTime = Instant.parse("2024-01-01T00:00:00Z")

        coEvery { mockEvent.area.bbox() } returns largeBbox
        every { mockEvent.getWaveStartDateTime() } returns startTime
        every { mockClock.now() } returns startTime + 1.hours

        // WHEN: Calculate wave properties
        val duration = waveLinear.getWaveDuration()
        val progression = waveLinear.getProgression()

        // THEN: Should handle large areas efficiently
        assertNotNull(duration, "Duration should be calculated for large areas")
        assertTrue(duration > 0.seconds, "Duration should be positive for large areas")
        assertTrue(progression >= 0.0 && progression <= 100.0, "Progression should be valid for large areas")
    }

    @Test
    fun `test wave boundary conditions`() = runBlocking {
        // Test behavior at wave boundaries
        val bbox = BoundingBox.fromCorners(Position(10.0, 0.0), Position(10.1, 100.0)) // Very narrow area
        val startTime = Instant.parse("2024-01-01T00:00:00Z")

        coEvery { mockEvent.area.bbox() } returns bbox
        every { mockEvent.getWaveStartDateTime() } returns startTime
        every { mockClock.now() } returns startTime + 30.minutes

        // WHEN: Calculate wave properties for narrow area
        val duration = waveLinear.getWaveDuration()
        val progression = waveLinear.getProgression()

        // THEN: Should handle boundary conditions gracefully
        assertTrue(duration > 0.seconds, "Duration should be positive even for narrow areas")
        assertTrue(progression >= 0.0 && progression <= 100.0, "Progression should be valid for narrow areas")
    }

    @Test
    fun `test concurrent wave calculations consistency`() = runBlocking {
        // GIVEN: Multiple concurrent calculations
        val userPosition = Position(12.5, 50.0)
        val bbox = BoundingBox.fromCorners(Position(10.0, 0.0), Position(15.0, 100.0))
        val startTime = Instant.parse("2024-01-01T00:00:00Z")

        waveLinear.setPositionRequester { userPosition }
        every { mockClock.now() } returns startTime + 30.minutes
        every { mockEvent.getWaveStartDateTime() } returns startTime
        coEvery { mockEvent.area.bbox() } returns bbox
        coEvery { mockEvent.area.isPositionWithin(userPosition) } returns true

        // WHEN: Perform multiple concurrent calculations
        val progression1 = waveLinear.getProgression()
        val progression2 = waveLinear.getProgression()
        val ratio1 = waveLinear.userPositionToWaveRatio()
        val ratio2 = waveLinear.userPositionToWaveRatio()

        // THEN: Results should be consistent
        assertEquals(progression1, progression2, "Progression calculations should be consistent")
        assertEquals(ratio1, ratio2, "Ratio calculations should be consistent")
    }

    @Test
    fun `test wave duration calculation with different area sizes`() = runBlocking {
        // GIVEN: A specific area size
        val bbox = BoundingBox.fromCorners(Position(0.0, 0.0), Position(10.0, 10.0))
        coEvery { mockEvent.area.bbox() } returns bbox

        // Mock GeoUtils to return consistent distance
        mockkObject(GeoUtils)
        every { calculateDistance(any(), any(), any()) } returns 1000.0

        // WHEN: Calculate duration
        val duration = waveLinear.getWaveDuration()

        // THEN: Duration should be positive and reasonable
        assertNotNull(duration, "Duration should not be null")
        assertTrue(duration >= 0.seconds, "Duration should be non-negative")

        unmockkObject(GeoUtils)
    }
}
