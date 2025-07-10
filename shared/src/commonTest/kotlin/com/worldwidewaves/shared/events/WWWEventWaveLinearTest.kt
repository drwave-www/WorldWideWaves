package com.worldwidewaves.shared.events

/*
 * Copyright 2024 DrWave
 *
 * WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and
 * countries, culminating in a global wave. The project aims to transcend physical and cultural
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
import kotlinx.datetime.Instant
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

class WWWEventWaveLinearTest : KoinTest {

    private val dispatcher = StandardTestDispatcher() // Create dispatcher instance

    private var mockClock = mockk<IClock>()
    private var mockEvent = mockk<IWWWEvent>(relaxed = true)
    private lateinit var waveLinear: WWWEventWaveLinear
    private lateinit var mockArea: WWWEventArea
    private lateinit var mockWarming: WWWEventWaveWarming

    // ---------------------------

    init {
        Napier.base(object : Antilog() {
            override fun performLog(priority: LogLevel, tag: String?, throwable: Throwable?, message: String?) {
                println(message)
            }
        })
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        startKoin { modules(module { single { mockClock } }) }

        mockEvent = mockk()
        mockClock = mockk()
        mockArea = mockk()
        mockWarming = mockk()

        waveLinear = WWWEventWaveLinear(
            speed = 100.0, // m/s
            direction = WWWEventWave.Direction.EAST,
            approxDuration = 60
        )
        waveLinear.setRelatedEvent<WWWEventWaveLinear>(mockEvent)

        every { mockEvent.area } returns mockArea
        coEvery { mockEvent.isRunning() } returns true
        coEvery { mockEvent.getWaveStartDateTime() } returns mockk()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterTest
    fun tearDown() {
        stopKoin()
        Dispatchers.resetMain()
    }

    // ---------------------------

    @Test
    fun testGetWaveDuration() = runBlocking {
        // GIVEN
        val bbox = BoundingBox.fromCorners(
            sw = Position(20.0, 10.0),
            ne = Position(40.0, 50.0)
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
    private fun testHasUserBeenHit(isWithin : Boolean = true) = runBlocking {
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
    fun testCurrentWaveLongitude_EastDirection() = runBlocking {
        // GIVEN
        val bbox = BoundingBox.fromCorners(
            sw = Position(10.0, 20.0),
            ne = Position(15.0, 30.0)
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
    fun testCurrentWaveLongitude_WestDirection_isUserWithin() = runBlocking {
        // GIVEN
        waveLinear = waveLinear.copy(direction = WWWEventWave.Direction.WEST).setRelatedEvent(mockEvent)
        val bbox = BoundingBox.fromCorners(
            sw = Position(10.0, 20.0),
            ne = Position(15.0, 30.0)
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
    fun testTimeBeforeUserHit_UserPositionAvailable() = runBlocking {
        // GIVEN
        val bbox = BoundingBox.fromCorners(
            sw = Position(10.0, 20.0),
            ne = Position(15.0, 30.0)
        )
        val userPosition = Position(12.5, 25.0)
        val startTime = Instant.parse("2024-01-01T00:00:00Z")
        val currentTime = Instant.parse("2024-01-01T00:10:00Z") // 10 minutes later

        every { mockClock.now() } returns currentTime
        every { mockEvent.getWaveStartDateTime() } returns startTime
        coEvery { mockEvent.area.bbox() } returns bbox
        waveLinear.setPositionRequester { userPosition }
        coEvery { mockEvent.area.isPositionWithin(userPosition) } returns true

        // WHEN
        val result = waveLinear.timeBeforeUserHit()

        // THEN
        val waveCurrentLongitude = waveLinear.closestWaveLongitude(12.5)
        val distanceToUser = calculateDistance(waveCurrentLongitude, userPosition.lng, userPosition.lat)
        val expectedTime = (distanceToUser / waveLinear.speed).seconds
        assertEquals(expectedTime, result)
    }

    @Test
    fun testTimeBeforeUserHit_UserPositionNull() = runBlocking {
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
    fun `getWavePolygons with no last wave state and no progression`() = runBlocking {
        // GIVEN
        val mockBoundingBox = BoundingBox.fromCorners(Position(1.0, 1.0), Position(2.0, 2.0))
        val mockPolygons = listOf(Polygon.fromPositions(listOf(Position(1.0, 1.0), Position(2.0, 2.0), Position(1.0, 2.0))))

        val startTime = Instant.parse("2023-06-15T10:15:30.00Z")
        every { mockEvent.getWaveStartDateTime() } returns startTime

        coEvery { mockArea.getPolygons() } returns mockPolygons
        coEvery { mockArea.bbox() } returns mockBoundingBox
        every { mockClock.now() } returns startTime

        // WHEN
        val result = waveLinear.getWavePolygons(null, WWWEventWave.WaveMode.ADD)

        // THEN
        assertNull(result)
    }

    @Test
    fun `getWavePolygons with no last wave state`() = runBlocking {
        // GIVEN
        val mockBoundingBox = BoundingBox.fromCorners(Position(1.0, 1.0), Position(2.0, 2.0))
        val mockPolygons = listOf(Polygon.fromPositions(listOf(Position(1.0, 1.0), Position(2.0, 2.0), Position(1.0, 2.0))))

        val startTime = Instant.parse("2023-06-15T10:15:30.00Z")
        every { mockEvent.getWaveStartDateTime() } returns startTime
        coEvery { mockArea.getPolygons() } returns mockPolygons
        coEvery { mockArea.bbox() } returns mockBoundingBox
        every { mockClock.now() } returns startTime + 1.hours

        // WHEN
        val result = waveLinear.getWavePolygons(null, WWWEventWave.WaveMode.ADD)

        // THEN
        assertNotNull(result)
        assertTrue(result.traversedPolygons.isNotEmpty() || result.remainingPolygons.isNotEmpty())
    }

    @Test
    fun `getWavePolygons with last wave state and ADD mode`() = runBlocking {
        // GIVEN
        val mockBoundingBox = BoundingBox.fromCorners(Position(0.0, 0.0), Position(10.0, 10.0))
        val mockPolygons = listOf(Polygon.fromPositions(listOf(Position(1.0, 1.0), Position(2.0, 2.0), Position(1.0, 2.0))))
        val startTime = Instant.parse("2023-06-15T10:15:30.00Z")
        val lastWaveState = WWWEventWave.WavePolygons(
            timestamp = startTime,
            traversedPolygons = listOf(Polygon.fromPositions(listOf(Position(0.0, 0.0), Position(1.0, 1.0), Position(0.0, 1.0)))),
            remainingPolygons = mockPolygons
        )

        every { mockEvent.getWaveStartDateTime() } returns startTime

        coEvery { mockArea.getPolygons() } returns mockPolygons
        coEvery { mockArea.bbox() } returns mockBoundingBox
        every { mockClock.now() } returns startTime + 2.hours

        // WHEN
        val result = waveLinear.getWavePolygons(lastWaveState, WWWEventWave.WaveMode.ADD)

        // THEN
        assertNotNull(result)
        assertTrue(result.traversedPolygons.size >= lastWaveState.traversedPolygons.size)
    }

    @Test
    fun `getWavePolygons with last wave state and RECOMPOSE mode`() = runBlocking {
        // GIVEN
        val mockPolygons = listOf(Polygon.fromPositions(listOf(Position(10.0, -10.0), Position(10.0, 10.0), Position(-10.0, 10.0))))
        val startTime = Instant.parse("2023-06-15T10:15:30.00Z")
        val cutPosition1Left = Position(-10.0, -10.0).init()
        val cutPosition1Right = Position(-10.0, 10.0).init()
        val cutPosition2Left = Position(10.0, -10.0).init()
        val cutPosition2Right = Position(10.0, 10.0).init()
        val lastWaveState = WWWEventWave.WavePolygons(
            timestamp = startTime,
            traversedPolygons = listOf(Polygon.fromPositions(listOf(
                CutPosition(-10.0, 0.0, 42, cutPosition1Left, cutPosition1Right),
                CutPosition(10.0, 0.0, 42, cutPosition1Left, cutPosition1Right),
                Position(10.0, -10.0),
                Position(-10.0, -10.0)
            ))),
            remainingPolygons = listOf(Polygon.fromPositions(listOf(
                CutPosition(10.0, 0.0, 42, cutPosition2Left, cutPosition2Right),
                Position(10.0, 10.0),
                Position(-10.0, 10.0),
                CutPosition(-10.0, 0.0, 42, cutPosition2Left, cutPosition2Right)
            )))
        )
        val mockBoundingBox = mockPolygons.first().bbox()

        coEvery { mockArea.getPolygons() } returns mockPolygons
        coEvery { mockArea.bbox() } returns mockBoundingBox
        every { mockClock.now() } returns startTime + 4.hours
        every { mockEvent.getWaveStartDateTime() } returns startTime

        // WHEN
        val result = waveLinear.getWavePolygons(lastWaveState, WWWEventWave.WaveMode.RECOMPOSE)

        // THEN
        assertNotNull(result)
        assertTrue(result.traversedPolygons.isNotEmpty())
        assertTrue(result.remainingPolygons.isNotEmpty())
    }

    @Test
    fun `getWavePolygons with empty area polygons`() = runBlocking {
        // GIVEN
        val mockBoundingBox = BoundingBox.fromCorners(Position(0.0, 0.0), Position(10.0, 10.0))

        val startTime = Instant.parse("2023-06-15T10:15:30.00Z")
        every { mockEvent.getWaveStartDateTime() } returns startTime

        coEvery { mockArea.getPolygons() } returns emptyList()
        coEvery { mockArea.bbox() } returns mockBoundingBox
        every { mockClock.now() } returns startTime

        // WHEN
        val result = waveLinear.getWavePolygons(null, WWWEventWave.WaveMode.ADD)

        // THEN
        assertNull(result)
    }

    @Test
    fun `getWavePolygons with WEST direction`() = runBlocking {
        // GIVEN
        waveLinear = WWWEventWaveLinear(
            speed = 100.0,
            direction = WWWEventWave.Direction.WEST,
            approxDuration = 60
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
        val result = waveLinear.getWavePolygons(null, WWWEventWave.WaveMode.ADD)

        // THEN
        assertNotNull(result)
        assertTrue(result.traversedPolygons.isNotEmpty() || result.remainingPolygons.isNotEmpty())
    }




}