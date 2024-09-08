package com.worldwidewaves.shared.events

import com.worldwidewaves.shared.events.utils.BoundingBox
import com.worldwidewaves.shared.events.utils.GeoUtils
import com.worldwidewaves.shared.events.utils.GeoUtils.calculateDistance
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.events.utils.PolygonUtils
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
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
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
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/*
 * Copyright 2024 DrWave
 *
 * WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and countries,
 * culminating in a global wave. The project aims to transcend physical and cultural boundaries, fostering unity,
 * community, and shared human experience by leveraging real-time coordination and location-based services.
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
        val polygon = listOf<Position>()
        mockkObject(PolygonUtils)
        coEvery { event.area.getPolygons() } returns listOf(polygon)
        val splitResult = PolygonUtils.SplitPolygonResult(emptyList(), listOf(polygon))
        every { PolygonUtils.splitPolygonByLongitude(polygon, 30.0) } returns splitResult

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
        val bbox = mockk<BoundingBox>()
        every { bbox.maxLatitude } returns 40.0
        every { bbox.minLatitude } returns 20.0
        every { bbox.minLongitude } returns 10.0
        every { bbox.maxLongitude } returns 50.0
        coEvery { event.area.getBoundingBox() } returns bbox
        mockkObject(GeoUtils)
        every { GeoUtils.calculateDistance(10.0, 50.0, 30.0) } returns 4000.0

        // WHEN
        val result = wave.getWaveDuration()
        testScheduler.advanceUntilIdle()

        // THEN
        assertEquals(400.seconds, result)
        coVerify { event.area.getBoundingBox() }
    }

    @Test
    fun testHasUserBeenHit() = runTest {
        // GIVEN
        val userPosition = Position(1.0, 25.0)
        val bbox = mockk<BoundingBox>()
        coEvery { wave.getUserPosition() } returns userPosition
        coEvery { event.area.getBoundingBox() } returns bbox
        every { bbox.minLongitude } returns 20.0
        coEvery { wave.currentWaveLongitude(bbox) } returns 30.0

        // WHEN
        val result = wave.hasUserBeenHit()

        // THEN
        assertTrue(result)
        coVerify { wave.getUserPosition() }
    }

    @Test
    fun testCurrentWaveLongitude_EastDirection() {
        // GIVEN
        val bbox = BoundingBox(
            sw = Position(10.0, 20.0),
            ne = Position(15.0, 30.0)
        )
        val startTime = Instant.parse("2024-01-01T00:00:00Z")
        val currentTime = Instant.parse("2024-01-01T00:10:00Z") // 10 minutes later

        every { clock.now() } returns currentTime
        every { event.getStartDateTime() } returns startTime

        // WHEN
        val result = wave.currentWaveLongitude(bbox)

        // THEN
        val expectedLongitude = 20.0 + (10.0 * 600 / calculateDistance(20.0, 30.0, 12.5)) * 10.0
        assertEquals(expectedLongitude, result, 0.0001)
    }

    @Test
    fun testCurrentWaveLongitude_WestDirection() {
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

        // WHEN
        val result = wave.currentWaveLongitude(bbox)

        // THEN
        val expectedLongitude = 30.0 - (10.0 * 600 / calculateDistance(20.0, 30.0, 12.5)) * 10.0
        assertEquals(expectedLongitude, result, 0.0001)
    }

}