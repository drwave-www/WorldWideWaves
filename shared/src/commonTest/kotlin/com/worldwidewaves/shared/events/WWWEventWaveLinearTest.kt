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
import com.worldwidewaves.shared.events.utils.GeoUtils
import com.worldwidewaves.shared.events.utils.GeoUtils.calculateDistance
import com.worldwidewaves.shared.events.utils.GeoUtils.toRadians
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.events.utils.PolygonUtils
import com.worldwidewaves.shared.events.utils.PolygonUtils.RightCutPolygon
import com.worldwidewaves.shared.events.utils.PolygonUtils.splitByLongitude
import com.worldwidewaves.shared.events.utils.Position
import io.github.aakira.napier.Antilog
import io.github.aakira.napier.LogLevel
import io.github.aakira.napier.Napier
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Instant
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import kotlin.math.PI
import kotlin.math.cos
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.seconds

class WWWEventWaveLinearTest : KoinTest {

    private val dispatcher = StandardTestDispatcher() // Create dispatcher instance

    private var clock = mockk<IClock>()
    private var event = mockk<IWWWEvent>(relaxed = true)
    private lateinit var wave: WWWEventWaveLinear

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
        startKoin { modules(module { single { clock } }) }

        wave = spyk(WWWEventWaveLinear(
            speed = 10.0,
            direction = WWWEventWave.Direction.EAST,
            warming = WWWEventWaveWarming(type = WWWEventWaveWarming.Type.LONGITUDE_CUT, longitude = 30.0)
        )).setRelatedEvent(event)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterTest
    fun tearDown() {
        stopKoin()
        Dispatchers.resetMain()
    }

    // ---------------------------

    @Test
    fun testGetWarmingPolygons() = runTest {
        // WHEN
        val polygon = RightCutPolygon(42)
        mockkObject(PolygonUtils)
        coEvery { event.area.getPolygons() } returns listOf(polygon)
        val splitResult = PolygonUtils.PolygonSplitResult(emptyList(), listOf(polygon))
        every { polygon.splitByLongitude(30.0) } returns splitResult

        // WHEN
        val result = wave.getWarmingPolygons()
        testScheduler.advanceUntilIdle()

        // THEN
        assertEquals(listOf(polygon), result)
        coVerify { event.area.getPolygons() }
    }

    @Test
    fun testGetWaveDuration() = runTest {
        // GIVEN
        val bbox = BoundingBox(
            sw = Position(20.0, 10.0),
            ne = Position(40.0, 50.0)
        )
        coEvery { event.area.getBoundingBox() } returns bbox
        mockkObject(GeoUtils)
        every { calculateDistance(any(), any(), any()) } returns 4000.0

        // WHEN
        val result = wave.getWaveDuration()
        testScheduler.advanceUntilIdle()

        // THEN
        assertEquals(400.seconds, result)
        coVerify { event.area.getBoundingBox() }
    }

    @Test fun testHasUserBeenHit_isWithin() = testHasUserBeenHit(isWithin = true)
    @Test fun testHasUserBeenHit_isNotWithin() = testHasUserBeenHit(isWithin = false)
    private fun testHasUserBeenHit(isWithin : Boolean = true) = runTest {
        // GIVEN
        val userPosition = Position(1.0, 25.0)
        val bbox = mockk<BoundingBox>()
        coEvery { wave.getUserPosition() } returns userPosition
        every { bbox.minLongitude } returns 20.0
        every { bbox.maxLongitude } returns 30.0

        val startTime = Instant.parse("2024-01-01T00:00:00Z")
        val currentTime = Instant.parse("2024-01-01T00:10:00Z") // 10 minutes later
        every { clock.now() } returns currentTime
        every { event.getStartDateTime() } returns startTime

        coEvery { wave.currentWaveLongitude(1.0) } returns 30.0
        coEvery { event.area.isPositionWithin(userPosition) } returns isWithin

        // WHEN
        val result = wave.hasUserBeenHitInCurrentPosition()
        testScheduler.advanceUntilIdle()

        // THEN
        assertEquals(isWithin, result)
        coVerify { wave.getUserPosition() }
    }

    @Test
    fun testCurrentWaveLongitude_EastDirection() = runBlocking {
        // GIVEN
        val bbox = BoundingBox(
            sw = Position(10.0, 20.0),
            ne = Position(15.0, 30.0)
        )
        val startTime = Instant.parse("2024-01-01T00:00:00Z")
        val currentTime = Instant.parse("2024-01-01T00:10:00Z") // 10 minutes later

        every { clock.now() } returns currentTime
        every { event.getStartDateTime() } returns startTime
        coEvery { event.area.getBoundingBox() } returns bbox

        // WHEN
        val result = wave.currentWaveLongitude(12.5)

        // THEN
        val expectedLongitude = 20.0 + (10.0 * 600 / calculateDistance(20.0, 30.0, 12.5)) * 10.0
        assertEquals(expectedLongitude, result, 0.0001)
    }

    @Test
    fun testCurrentWaveLongitude_WestDirection_isUserWithin() = runBlocking {
        // GIVEN
        wave = wave.copy(direction = WWWEventWave.Direction.WEST).setRelatedEvent(event)
        val bbox = BoundingBox(
            sw = Position(10.0, 20.0),
            ne = Position(15.0, 30.0)
        )
        val startTime = Instant.parse("2024-01-01T00:00:00Z")
        val currentTime = Instant.parse("2024-01-01T00:10:00Z") // 10 minutes later

        every { clock.now() } returns currentTime
        every { event.getStartDateTime() } returns startTime
        coEvery { event.area.getBoundingBox() } returns bbox
        coEvery { event.area.isPositionWithin(any()) } returns true

        // WHEN
        val result = wave.currentWaveLongitude(12.5)

        // THEN
        val expectedLongitude = 30.0 - (10.0 * 600 / calculateDistance(20.0, 30.0, 12.5)) * 10.0
        assertEquals(expectedLongitude, result, 0.0001)
    }


    @Test
    fun testTimeBeforeHit_UserPositionAvailable() = runTest {
        // GIVEN
        val bbox = BoundingBox(
            sw = Position(10.0, 20.0),
            ne = Position(15.0, 30.0)
        )
        val userPosition = Position(12.5, 25.0)
        val startTime = Instant.parse("2024-01-01T00:00:00Z")
        val currentTime = Instant.parse("2024-01-01T00:10:00Z") // 10 minutes later

        every { clock.now() } returns currentTime
        every { event.getStartDateTime() } returns startTime
        coEvery { event.area.getBoundingBox() } returns bbox
        every { wave.getUserPosition() } returns userPosition
        coEvery { event.area.isPositionWithin(userPosition) } returns true

        // WHEN
        val result = wave.timeBeforeHit()
        testScheduler.advanceUntilIdle()

        // THEN
        val waveCurrentLongitude = wave.currentWaveLongitude(12.5)
        val distanceToUser = calculateDistance(waveCurrentLongitude, userPosition.lng, userPosition.lat)
        val expectedTime = (distanceToUser / wave.speed).seconds
        assertEquals(expectedTime, result)
    }

    @Test
    fun testTimeBeforeHit_UserPositionNull() = runTest {
        // GIVEN
        every { wave.getUserPosition() } returns null

        // WHEN
        val result = wave.timeBeforeHit()
        testScheduler.advanceUntilIdle()

        // THEN
        assertNull(result)
    }

    // ---------------------------

    @Test
    fun testCalculateLatLonBandsWithSpeed() = runBlocking {
        // GIVEN
        val sw = Position(48.8156, 2.2242) // Paris city
        val ne = Position(48.9021, 2.4699)
        coEvery { event.area.getBoundingBox() } returns BoundingBox(sw, ne)

        // WHEN
        val bands = wave.calculateWaveBands(30.seconds)

        // THEN
        assertEquals(3, bands.size)
        assertEquals(48.8156, bands[0].latitude)
        assertEquals(0.03333333333333333, bands[0].latWidth)
        assertEquals(0.006222215814700397, bands[0].lngWidth)
    }

    @Test
    fun testCalculateLonBandWidthAtMiddleLatitude() {
        // GIVEN
        val middleLatitude = 45.0
        val expectedWidth = (10.0 / (6371000 * cos(45.0.toRadians()))) * (180 / PI)

        // WHEN
        val lonBandWidth = wave.calculateLonBandWidthAtMiddleLatitude(middleLatitude)

        // THEN
        assertEquals(expectedWidth, lonBandWidth)
    }

    @Test
    fun testAdjustLongitudeWidthAtLatitude() {
        // GIVEN
        val latitude = 45.0
        val lonWidth = 10.0
        val expectedWidth = lonWidth / cos(45.0.toRadians())

        // WHEN
        val adjustedWidth = wave.adjustLongitudeWidthAtLatitude(latitude, lonWidth)

        // THEN
        assertEquals(expectedWidth, adjustedWidth)
    }

    @Test
    fun testCalculateOptimalLatBandWidth() {
        // GIVEN
        val latitude = 45.0
        val lonBandWidthAtEquator = 10.0
        val minPerceptibleDifference = 10.0 // Minimum perceptible difference in meters
        val earthRadius = 6371000.0 // Earth's radius in meters

        // Calculate the expected longitudinal distance at this latitude
        val latitudeInRadians = latitude.toRadians()
        val lonDistanceAtThisLat = earthRadius * cos(latitudeInRadians) * lonBandWidthAtEquator * (PI / 180)

        // Calculate how much latitude difference is needed for the minimum perceptible difference
        val expectedWidth = minPerceptibleDifference / lonDistanceAtThisLat

        // WHEN
        val latBandWidth = wave.calculateOptimalLatBandWidth(latitude, lonBandWidthAtEquator)

        // THEN
        assertEquals(expectedWidth, latBandWidth, 0.0001) // Allow a small tolerance for floating-point precision
    }

}