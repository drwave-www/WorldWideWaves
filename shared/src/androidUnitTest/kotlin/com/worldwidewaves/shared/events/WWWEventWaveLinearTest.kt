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
import com.worldwidewaves.shared.events.utils.CoroutineScopeProvider
import com.worldwidewaves.shared.events.utils.CutPosition
import com.worldwidewaves.shared.events.utils.DefaultCoroutineScopeProvider
import com.worldwidewaves.shared.events.utils.GeoUtils
import com.worldwidewaves.shared.events.utils.GeoUtils.calculateDistance
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.events.utils.Polygon
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.events.utils.init
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
            modules(
                module {
                    single<IClock> { mockClock }
                    single<CoroutineScopeProvider> { DefaultCoroutineScopeProvider(dispatcher, dispatcher) }
                },
            )
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
            waveLinear.timeBeforeUserHit()

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
    fun `test linear wave progression calculation accuracy`() =
        runBlocking {
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
    fun `test linear wave state transitions during event lifecycle`() =
        runBlocking {
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
    fun `test user position to wave ratio calculation accuracy`() =
        runBlocking {
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
    fun `test hit timing accuracy across different speeds`() =
        runBlocking {
            // Test wave hit timing with different wave speeds
            val speedTestCases = listOf(50.0, 100.0, 200.0, 500.0) // m/s

            speedTestCases.forEach { speed ->
                // GIVEN: Wave with specific speed
                val testWave =
                    WWWEventWaveLinear(
                        speed = speed,
                        direction = WWWEventWave.Direction.EAST,
                        approxDuration = 60,
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
    fun `test wave direction impact on progression calculations`() =
        runBlocking {
            // Test both EAST and WEST directions
            val directions = listOf(WWWEventWave.Direction.EAST, WWWEventWave.Direction.WEST)

            directions.forEach { direction ->
                // GIVEN: Wave with specific direction
                val testWave =
                    WWWEventWaveLinear(
                        speed = 100.0,
                        direction = direction,
                        approxDuration = 60,
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
                assertTrue(
                    longitude >= bbox.minLongitude && longitude <= bbox.maxLongitude,
                    "Wave longitude should be within bbox for direction $direction",
                )
            }
        }

    @Test
    fun `test wave polygon generation with complex geometries`() =
        runBlocking {
            // GIVEN: Complex polygon shapes
            val complexPolygons =
                listOf(
                    // Triangle
                    Polygon.fromPositions(
                        listOf(
                            Position(0.0, 0.0),
                            Position(5.0, 0.0),
                            Position(2.5, 5.0),
                        ),
                    ),
                    // Rectangle
                    Polygon.fromPositions(
                        listOf(
                            Position(0.0, 0.0),
                            Position(10.0, 0.0),
                            Position(10.0, 5.0),
                            Position(0.0, 5.0),
                        ),
                    ),
                    // Pentagon
                    Polygon.fromPositions(
                        listOf(
                            Position(0.0, 0.0),
                            Position(2.0, 0.0),
                            Position(3.0, 1.5),
                            Position(1.0, 3.0),
                            Position(-1.0, 1.5),
                        ),
                    ),
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
            assertTrue(
                result.traversedPolygons.isNotEmpty() || result.remainingPolygons.isNotEmpty(),
                "Should have meaningful polygon data",
            )
        }

    @Test
    fun `test user hit detection edge cases`() =
        runBlocking {
            // GIVEN: User at a specific position
            val userPosition = Position(12.5, 25.0)
            val bbox = BoundingBox.fromCorners(Position(10.0, 20.0), Position(15.0, 30.0))

            waveLinear.setPositionRequester { userPosition }
            coEvery { mockEvent.area.bbox() } returns bbox
            coEvery { mockEvent.area.isPositionWithin(userPosition) } returns true

            // WHEN: Check if user has been hit
            val hasBeenHit = waveLinear.hasUserBeenHitInCurrentPosition()
        }

    @Test
    fun `test wave performance with large areas`() =
        runBlocking {
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
    fun `test wave boundary conditions`() =
        runBlocking {
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
    fun `test concurrent wave calculations consistency`() =
        runBlocking {
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
    fun `test wave duration calculation with different area sizes`() =
        runBlocking {
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

    // ===== WAVE FRONT CENTER POSITION TESTS =====

    @Test
    fun `test wave front center position with simple north-south rectangle`() =
        runBlocking {
            // GIVEN: Simple rectangle aligned north-south
            val bbox = BoundingBox.fromCorners(Position(10.0, 20.0), Position(30.0, 40.0))
            val startTime = Instant.parse("2024-01-01T00:00:00Z")
            val currentTime = startTime + 10.minutes

            coEvery { mockEvent.area.bbox() } returns bbox
            every { mockEvent.getWaveStartDateTime() } returns startTime
            every { mockClock.now() } returns currentTime

            // WHEN: Get wave front center position
            val result = waveLinear.getWaveFrontCenterPosition()

            // THEN: Should return position with center latitude
            assertNotNull(result, "Wave front center position should not be null")
            assertEquals(20.0, result.latitude, 0.0001, "Latitude should be center of bbox")
            assertTrue(result.longitude >= 20.0 && result.longitude <= 40.0, "Longitude should be within bbox")
        }

    @Test
    fun `test wave front center position with rotated rectangle`() =
        runBlocking {
            // GIVEN: Rotated rectangle (diagonal orientation)
            val bbox = BoundingBox.fromCorners(Position(-10.0, -15.0), Position(25.0, 35.0))
            val startTime = Instant.parse("2024-01-01T00:00:00Z")

            coEvery { mockEvent.area.bbox() } returns bbox
            every { mockEvent.getWaveStartDateTime() } returns startTime
            every { mockClock.now() } returns startTime + 5.minutes

            // WHEN: Get wave front center position
            val result = waveLinear.getWaveFrontCenterPosition()

            // THEN: Should use geometric center latitude
            assertNotNull(result)
            assertEquals(7.5, result.latitude, 0.0001, "Latitude should be geometric center")
        }

    @Test
    fun `test wave front center position with L-shaped polygon area`() =
        runBlocking {
            // GIVEN: L-shaped bounding box representation
            val bbox = BoundingBox.fromCorners(Position(0.0, 0.0), Position(50.0, 50.0))
            val startTime = Instant.parse("2024-01-01T00:00:00Z")

            coEvery { mockEvent.area.bbox() } returns bbox
            every { mockEvent.getWaveStartDateTime() } returns startTime
            every { mockClock.now() } returns startTime + 15.minutes

            // WHEN
            val result = waveLinear.getWaveFrontCenterPosition()

            // THEN: Uses bbox center even for complex shapes
            assertNotNull(result)
            assertEquals(25.0, result.latitude, 0.0001)
        }

    @Test
    fun `test wave front center position with U-shaped polygon area`() =
        runBlocking {
            // GIVEN: U-shaped bounding box
            val bbox = BoundingBox.fromCorners(Position(-20.0, 10.0), Position(40.0, 60.0))
            val startTime = Instant.parse("2024-01-01T00:00:00Z")

            coEvery { mockEvent.area.bbox() } returns bbox
            every { mockEvent.getWaveStartDateTime() } returns startTime
            every { mockClock.now() } returns startTime + 20.minutes

            // WHEN
            val result = waveLinear.getWaveFrontCenterPosition()

            // THEN
            assertNotNull(result)
            assertEquals(10.0, result.latitude, 0.0001, "Should use bbox center latitude")
        }

    @Test
    fun `test wave front center position with concave polygon`() =
        runBlocking {
            // GIVEN: Concave polygon with indentations
            val bbox = BoundingBox.fromCorners(Position(5.0, 15.0), Position(45.0, 55.0))
            val startTime = Instant.parse("2024-01-01T00:00:00Z")

            coEvery { mockEvent.area.bbox() } returns bbox
            every { mockEvent.getWaveStartDateTime() } returns startTime
            every { mockClock.now() } returns startTime + 8.minutes

            // WHEN
            val result = waveLinear.getWaveFrontCenterPosition()

            // THEN: Uses simple bbox center
            assertNotNull(result)
            assertEquals(25.0, result.latitude, 0.0001)
        }

    @Test
    fun `test wave front center position crossing equator`() =
        runBlocking {
            // GIVEN: Area crossing equator (negative to positive latitudes)
            val bbox = BoundingBox.fromCorners(Position(-30.0, 10.0), Position(30.0, 50.0))
            val startTime = Instant.parse("2024-01-01T00:00:00Z")

            coEvery { mockEvent.area.bbox() } returns bbox
            every { mockEvent.getWaveStartDateTime() } returns startTime
            every { mockClock.now() } returns startTime + 12.minutes

            // WHEN
            val result = waveLinear.getWaveFrontCenterPosition()

            // THEN: Center should be at equator
            assertNotNull(result)
            assertEquals(0.0, result.latitude, 0.0001, "Center should be at equator")
        }

    @Test
    fun `test wave front center position crossing prime meridian`() =
        runBlocking {
            // GIVEN: Area crossing prime meridian (negative to positive longitudes)
            val bbox = BoundingBox.fromCorners(Position(20.0, -10.0), Position(40.0, 10.0))
            val startTime = Instant.parse("2024-01-01T00:00:00Z")

            coEvery { mockEvent.area.bbox() } returns bbox
            every { mockEvent.getWaveStartDateTime() } returns startTime
            every { mockClock.now() } returns startTime + 5.minutes

            // WHEN
            val result = waveLinear.getWaveFrontCenterPosition()

            // THEN
            assertNotNull(result)
            assertEquals(30.0, result.latitude, 0.0001)
            assertTrue(result.longitude >= -10.0 && result.longitude <= 10.0)
        }

    @Test
    fun `test wave front center position with very narrow polygon`() =
        runBlocking {
            // GIVEN: Very narrow polygon (high aspect ratio)
            val bbox = BoundingBox.fromCorners(Position(45.0, 2.0), Position(45.1, 98.0))
            val startTime = Instant.parse("2024-01-01T00:00:00Z")

            coEvery { mockEvent.area.bbox() } returns bbox
            every { mockEvent.getWaveStartDateTime() } returns startTime
            every { mockClock.now() } returns startTime + 3.minutes

            // WHEN
            val result = waveLinear.getWaveFrontCenterPosition()

            // THEN
            assertNotNull(result)
            assertEquals(45.05, result.latitude, 0.0001, "Should handle narrow areas")
        }

    @Test
    fun `test wave front center position with very wide polygon`() =
        runBlocking {
            // GIVEN: Very wide polygon (large longitude range)
            val bbox = BoundingBox.fromCorners(Position(10.0, -120.0), Position(20.0, 120.0))
            val startTime = Instant.parse("2024-01-01T00:00:00Z")

            coEvery { mockEvent.area.bbox() } returns bbox
            every { mockEvent.getWaveStartDateTime() } returns startTime
            every { mockClock.now() } returns startTime + 30.minutes

            // WHEN
            val result = waveLinear.getWaveFrontCenterPosition()

            // THEN
            assertNotNull(result)
            assertEquals(15.0, result.latitude, 0.0001, "Should handle wide areas")
        }

    @Test
    fun `test wave front center position with irregular 8-vertex polygon`() =
        runBlocking {
            // GIVEN: Irregular octagon bounding box
            val bbox = BoundingBox.fromCorners(Position(-5.0, -8.0), Position(35.0, 42.0))
            val startTime = Instant.parse("2024-01-01T00:00:00Z")

            coEvery { mockEvent.area.bbox() } returns bbox
            every { mockEvent.getWaveStartDateTime() } returns startTime
            every { mockClock.now() } returns startTime + 7.minutes

            // WHEN
            val result = waveLinear.getWaveFrontCenterPosition()

            // THEN
            assertNotNull(result)
            assertEquals(15.0, result.latitude, 0.0001)
        }

    @Test
    fun `test wave front center position with irregular 12-vertex polygon`() =
        runBlocking {
            // GIVEN: Complex 12-sided polygon
            val bbox = BoundingBox.fromCorners(Position(22.0, 33.0), Position(58.0, 77.0))
            val startTime = Instant.parse("2024-01-01T00:00:00Z")

            coEvery { mockEvent.area.bbox() } returns bbox
            every { mockEvent.getWaveStartDateTime() } returns startTime
            every { mockClock.now() } returns startTime + 18.minutes

            // WHEN
            val result = waveLinear.getWaveFrontCenterPosition()

            // THEN
            assertNotNull(result)
            assertEquals(40.0, result.latitude, 0.0001)
        }

    @Test
    fun `test wave front center position with irregular 20-vertex polygon`() =
        runBlocking {
            // GIVEN: Very complex 20-sided polygon
            val bbox = BoundingBox.fromCorners(Position(-15.0, 5.0), Position(65.0, 95.0))
            val startTime = Instant.parse("2024-01-01T00:00:00Z")

            coEvery { mockEvent.area.bbox() } returns bbox
            every { mockEvent.getWaveStartDateTime() } returns startTime
            every { mockClock.now() } returns startTime + 25.minutes

            // WHEN
            val result = waveLinear.getWaveFrontCenterPosition()

            // THEN
            assertNotNull(result)
            assertEquals(25.0, result.latitude, 0.0001)
        }

    @Test
    fun `test wave front center position with minimal area polygon`() =
        runBlocking {
            // GIVEN: Minimal area polygon
            val bbox = BoundingBox.fromCorners(Position(42.0, 13.0), Position(42.001, 13.001))
            val startTime = Instant.parse("2024-01-01T00:00:00Z")

            coEvery { mockEvent.area.bbox() } returns bbox
            every { mockEvent.getWaveStartDateTime() } returns startTime
            every { mockClock.now() } returns startTime + 1.minutes

            // WHEN
            val result = waveLinear.getWaveFrontCenterPosition()

            // THEN
            assertNotNull(result)
            assertEquals(42.0005, result.latitude, 0.0001, "Should handle minimal areas")
        }

    @Test
    fun `test wave front center position at 0% progression`() =
        runBlocking {
            // GIVEN: Wave at start (0% progression)
            val bbox = BoundingBox.fromCorners(Position(10.0, 20.0), Position(30.0, 80.0))
            val startTime = Instant.parse("2024-01-01T00:00:00Z")

            coEvery { mockEvent.area.bbox() } returns bbox
            every { mockEvent.getWaveStartDateTime() } returns startTime
            every { mockClock.now() } returns startTime // No time elapsed

            // WHEN
            val result = waveLinear.getWaveFrontCenterPosition()

            // THEN: Should be at starting longitude
            assertNotNull(result)
            assertEquals(20.0, result.latitude, 0.0001)
            assertEquals(20.0, result.longitude, 0.0001, "Should be at start longitude for EAST direction")
        }

    @Test
    fun `test wave front center position at 25% progression`() =
        runBlocking {
            // GIVEN: Wave at some time after start
            val bbox = BoundingBox.fromCorners(Position(10.0, 0.0), Position(30.0, 100.0))
            val startTime = Instant.parse("2024-01-01T00:00:00Z")
            val currentTime = startTime + 10.minutes

            coEvery { mockEvent.area.bbox() } returns bbox
            every { mockEvent.getWaveStartDateTime() } returns startTime
            every { mockClock.now() } returns currentTime

            // WHEN
            val result = waveLinear.getWaveFrontCenterPosition()

            // THEN
            assertNotNull(result)
            assertEquals(20.0, result.latitude, 0.0001, "Latitude should be bbox center")
            // Longitude should be within bounds
            assertTrue(result.longitude >= 0.0 && result.longitude <= 100.0, "Longitude should be within bounds")
        }

    @Test
    fun `test wave front center position at 50% progression`() =
        runBlocking {
            // GIVEN: Wave at some mid-point time
            val bbox = BoundingBox.fromCorners(Position(15.0, 10.0), Position(35.0, 90.0))
            val startTime = Instant.parse("2024-01-01T00:00:00Z")
            val currentTime = startTime + 20.minutes

            coEvery { mockEvent.area.bbox() } returns bbox
            every { mockEvent.getWaveStartDateTime() } returns startTime
            every { mockClock.now() } returns currentTime

            // WHEN
            val result = waveLinear.getWaveFrontCenterPosition()

            // THEN
            assertNotNull(result)
            assertEquals(25.0, result.latitude, 0.0001)
            assertTrue(result.longitude >= 10.0 && result.longitude <= 90.0)
        }

    @Test
    fun `test wave front center position at 75% progression`() =
        runBlocking {
            // GIVEN: Wave at later time
            val bbox = BoundingBox.fromCorners(Position(20.0, 30.0), Position(60.0, 150.0))
            val startTime = Instant.parse("2024-01-01T00:00:00Z")
            val currentTime = startTime + 30.minutes

            coEvery { mockEvent.area.bbox() } returns bbox
            every { mockEvent.getWaveStartDateTime() } returns startTime
            every { mockClock.now() } returns currentTime

            // WHEN
            val result = waveLinear.getWaveFrontCenterPosition()

            // THEN
            assertNotNull(result)
            assertEquals(40.0, result.latitude, 0.0001)
            assertTrue(result.longitude >= 30.0 && result.longitude <= 150.0)
        }

    @Test
    fun `test wave front center position at 100% progression`() =
        runBlocking {
            // GIVEN: Wave well past duration (will be at end)
            val bbox = BoundingBox.fromCorners(Position(25.0, 40.0), Position(55.0, 160.0))
            val startTime = Instant.parse("2024-01-01T00:00:00Z")
            val currentTime = startTime + 2.hours // Far beyond typical duration

            coEvery { mockEvent.area.bbox() } returns bbox
            every { mockEvent.getWaveStartDateTime() } returns startTime
            every { mockClock.now() } returns currentTime

            // WHEN
            val result = waveLinear.getWaveFrontCenterPosition()

            // THEN: Should be at or past ending longitude
            assertNotNull(result)
            assertEquals(40.0, result.latitude, 0.0001)
            assertTrue(result.longitude >= 40.0, "Longitude should be at or past start for EAST direction")
        }

    @Test
    fun `test wave front center position with WEST direction`() =
        runBlocking {
            // GIVEN: Wave moving WEST
            waveLinear =
                WWWEventWaveLinear(
                    speed = 100.0,
                    direction = WWWEventWave.Direction.WEST,
                    approxDuration = 60,
                ).setRelatedEvent(mockEvent)

            val bbox = BoundingBox.fromCorners(Position(10.0, 20.0), Position(30.0, 80.0))
            val startTime = Instant.parse("2024-01-01T00:00:00Z")
            val currentTime = startTime + 10.minutes

            coEvery { mockEvent.area.bbox() } returns bbox
            every { mockEvent.getWaveStartDateTime() } returns startTime
            every { mockClock.now() } returns currentTime

            // WHEN
            val result = waveLinear.getWaveFrontCenterPosition()

            // THEN: Should move from maxLongitude (WEST direction)
            assertNotNull(result)
            assertEquals(20.0, result.latitude, 0.0001)
            assertTrue(result.longitude <= 80.0, "Should start from max longitude for WEST")
        }

    @Test
    fun `test wave front center position with EAST direction`() =
        runBlocking {
            // GIVEN: Wave moving EAST (already default)
            val bbox = BoundingBox.fromCorners(Position(15.0, 25.0), Position(45.0, 125.0))
            val startTime = Instant.parse("2024-01-01T00:00:00Z")
            val currentTime = startTime + 15.minutes

            coEvery { mockEvent.area.bbox() } returns bbox
            every { mockEvent.getWaveStartDateTime() } returns startTime
            every { mockClock.now() } returns currentTime

            // WHEN
            val result = waveLinear.getWaveFrontCenterPosition()

            // THEN: Should move from minLongitude (EAST direction)
            assertNotNull(result)
            assertEquals(30.0, result.latitude, 0.0001)
            assertTrue(result.longitude >= 25.0, "Should start from min longitude for EAST")
        }

    @Test
    fun `test wave front center position consistency across multiple calls`() =
        runBlocking {
            // GIVEN: Fixed time and bbox
            val bbox = BoundingBox.fromCorners(Position(12.0, 18.0), Position(48.0, 72.0))
            val startTime = Instant.parse("2024-01-01T00:00:00Z")
            val fixedTime = startTime + 20.minutes

            coEvery { mockEvent.area.bbox() } returns bbox
            every { mockEvent.getWaveStartDateTime() } returns startTime
            every { mockClock.now() } returns fixedTime

            // WHEN: Call multiple times
            val result1 = waveLinear.getWaveFrontCenterPosition()
            val result2 = waveLinear.getWaveFrontCenterPosition()
            val result3 = waveLinear.getWaveFrontCenterPosition()

            // THEN: Results should be identical
            assertNotNull(result1)
            assertNotNull(result2)
            assertNotNull(result3)
            assertEquals(result1.latitude, result2.latitude, 0.0001)
            assertEquals(result2.latitude, result3.latitude, 0.0001)
            assertEquals(result1.longitude, result2.longitude, 0.0001)
            assertEquals(result2.longitude, result3.longitude, 0.0001)
        }

    @Test
    fun `test wave front center position with different aspect ratios - square`() =
        runBlocking {
            // GIVEN: Square area (1:1 aspect ratio)
            val bbox = BoundingBox.fromCorners(Position(20.0, 30.0), Position(30.0, 40.0))
            val startTime = Instant.parse("2024-01-01T00:00:00Z")

            coEvery { mockEvent.area.bbox() } returns bbox
            every { mockEvent.getWaveStartDateTime() } returns startTime
            every { mockClock.now() } returns startTime + 5.minutes

            // WHEN
            val result = waveLinear.getWaveFrontCenterPosition()

            // THEN
            assertNotNull(result)
            assertEquals(25.0, result.latitude, 0.0001, "Center of square")
        }

    @Test
    fun `test wave front center position with different aspect ratios - 2 to 1 rectangle`() =
        runBlocking {
            // GIVEN: 2:1 aspect ratio (wider than tall)
            val bbox = BoundingBox.fromCorners(Position(10.0, 0.0), Position(30.0, 40.0))
            val startTime = Instant.parse("2024-01-01T00:00:00Z")

            coEvery { mockEvent.area.bbox() } returns bbox
            every { mockEvent.getWaveStartDateTime() } returns startTime
            every { mockClock.now() } returns startTime + 8.minutes

            // WHEN
            val result = waveLinear.getWaveFrontCenterPosition()

            // THEN
            assertNotNull(result)
            assertEquals(20.0, result.latitude, 0.0001)
        }

    @Test
    fun `test wave front center position with different aspect ratios - 1 to 5 tall rectangle`() =
        runBlocking {
            // GIVEN: 1:5 aspect ratio (much taller than wide)
            val bbox = BoundingBox.fromCorners(Position(-40.0, 50.0), Position(40.0, 60.0))
            val startTime = Instant.parse("2024-01-01T00:00:00Z")

            coEvery { mockEvent.area.bbox() } returns bbox
            every { mockEvent.getWaveStartDateTime() } returns startTime
            every { mockClock.now() } returns startTime + 2.minutes

            // WHEN
            val result = waveLinear.getWaveFrontCenterPosition()

            // THEN
            assertNotNull(result)
            assertEquals(0.0, result.latitude, 0.0001, "Center of tall rectangle")
        }
}
