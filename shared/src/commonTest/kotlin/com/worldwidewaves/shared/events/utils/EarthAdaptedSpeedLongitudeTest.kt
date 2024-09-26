package com.worldwidewaves.shared.events.utils

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

import com.worldwidewaves.shared.WWWGlobals.Companion.WAVE_LINEAR_METERS_REFRESH
import com.worldwidewaves.shared.events.WWWEventWave.Direction
import com.worldwidewaves.shared.events.utils.GeoUtils.EARTH_RADIUS
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

class EarthAdaptedSpeedLongitudeTest {

    private val epsilon = 1e-6 // For floating-point comparisons

    @Test
    fun `test withProgression - no elapsed time`() {
        val bbox = BoundingBox(Position(0.0, 0.0), Position(10.0, 10.0))
        val speed = 100.0 // m/s
        val direction = Direction.EAST

        val longitude = EarthAdaptedSpeedLongitude(bbox, speed, direction)
        val progressed = longitude.withProgression(Duration.ZERO)

        assertTrue(progressed.size() > 1)
        progressed.forEach {
            assertTrue(it.lng in 0.0..10.0)
            assertTrue(it.lat in 0.0..10.0)
        }
    }

    @Test
    fun `test withProgression - positive elapsed time`() {
        val bbox = BoundingBox(Position(0.0, 0.0), Position(10.0, 10.0))
        val speed = 100.0 // m/s
        val direction = Direction.EAST

        val longitude = EarthAdaptedSpeedLongitude(bbox, speed, direction)
        val progressed = longitude.withProgression(1.hours)

        assertTrue(progressed.size() > 1)
        progressed.forEach {
            assertTrue(it.lng > 0.0)
            assertTrue(it.lat in 0.0..10.0)
        }
    }

    @Test
    fun `test withProgression - direction West`() {
        val bbox = BoundingBox(Position(0.0, 0.0), Position(10.0, 10.0))
        val speed = 100.0 // m/s
        val direction = Direction.WEST

        val longitude = EarthAdaptedSpeedLongitude(bbox, speed, direction)
        val progressed = longitude.withProgression(1.hours)

        assertTrue(progressed.size() > 1)
        progressed.forEach {
            assertTrue(it.lng < 10.0)
            assertTrue(it.lat in 0.0..10.0)
        }
    }


    @Test
    fun `test calculateLonBandWidthAtMiddleLatitude`() {
        val bbox = BoundingBox(Position(0.0, 0.0), Position(10.0, 10.0))
        val speed = 100.0 // m/s
        val direction = Direction.EAST

        val longitude = EarthAdaptedSpeedLongitude(bbox, speed, direction)
        val width = longitude.calculateLonBandWidthAtMiddleLatitude(0.0)

        // Expected width at equator: (100 * bandDuration.inWholeMilliseconds / 1000) / (EARTH_RADIUS * 1) * (180 / PI)
        val bandDuration = (WAVE_LINEAR_METERS_REFRESH / speed).seconds
        val expected = (speed * bandDuration.inWholeMilliseconds / 1000) / EARTH_RADIUS * (180 / PI)
        val epsilon = 1e-6

        assertEquals(expected, width, epsilon)
    }

    @Test
    fun `test calculateOptimalLatBandWidth`() {
        val bbox = BoundingBox(Position(0.0, 0.0), Position(10.0, 10.0))
        val speed = 100.0 // m/s
        val direction = Direction.EAST

        val longitude = EarthAdaptedSpeedLongitude(bbox, speed, direction)
        val width = longitude.calculateOptimalLatBandWidth(45.0, 1.0)

        assertTrue(width > 0)
    }

    @Test
    fun `test adjustLongitudeWidthAtLatitude`() {
        val bbox = BoundingBox(Position(0.0, 0.0), Position(10.0, 10.0))
        val speed = 100.0 // m/s
        val direction = Direction.EAST

        val longitude = EarthAdaptedSpeedLongitude(bbox, speed, direction)
        val adjustedWidth = longitude.adjustLongitudeWidthAtLatitude(45.0, 1.0)

        assertTrue(adjustedWidth > 1.0)
    }

    @Test
    fun `test calculateWaveBands`() {
        val bbox = BoundingBox(Position(0.0, 0.0), Position(10.0, 10.0))
        val speed = 100.0 // m/s
        val direction = Direction.EAST

        val longitude = EarthAdaptedSpeedLongitude(bbox, speed, direction)
        val bands = longitude.calculateWaveBands()

        assertTrue(bands.isNotEmpty())
        bands.forEach {
            assertTrue(it.latitude in 0.0..10.0)
            assertTrue(it.latWidth > 0)
            assertTrue(it.lngWidth > 0)
        }
    }

    @Test
    fun `test invalid speed`() {
        val bbox = BoundingBox(Position(0.0, 0.0), Position(10.0, 10.0))
        val speed = -100.0 // Invalid negative speed
        val direction = Direction.EAST

        assertFailsWith<IllegalArgumentException> {
            EarthAdaptedSpeedLongitude(bbox, speed, direction)
        }
    }

    @Test
    fun `test progression near poles`() {
        val bbox = BoundingBox(Position(85.0, 0.0), Position(90.0, 10.0))
        val speed = 100.0 // m/s
        val direction = Direction.EAST

        val longitude = EarthAdaptedSpeedLongitude(bbox, speed, direction)
        val progressed = longitude.withProgression(1.hours)

        assertTrue(progressed.size() > 1)
        progressed.forEach {
            assertTrue(it.lng > 0.0)
            assertTrue(it.lat in 85.0..90.0)
        }
    }
}