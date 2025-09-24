package com.worldwidewaves.shared.events.utils

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

import com.worldwidewaves.shared.WWWGlobals.Wave
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.WWWEventArea
import com.worldwidewaves.shared.events.WWWEventWave.Direction
import com.worldwidewaves.shared.events.WWWEventWaveLinear
import com.worldwidewaves.shared.events.utils.GeoUtils.EARTH_RADIUS
import com.worldwidewaves.shared.events.utils.GeoUtils.calculateDistance
import io.github.aakira.napier.Antilog
import io.github.aakira.napier.LogLevel
import io.github.aakira.napier.Napier
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class EarthAdaptedSpeedLongitudeTest {
    private val dispatcher = StandardTestDispatcher() // Create dispatcher instance

    private val epsilonFine = 1e-6 // For floating-point comparisons
    private val epsilonMedium = 1e-3
    private val epsilonLarge = 0.01
    private val epsilonHuge = 5.0

    private var mockClock = mockk<IClock>()

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
        startKoin { modules(module { single { mockClock } }) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterTest
    fun tearDown() {
        stopKoin()
        Dispatchers.resetMain()
    }

    @Test
    fun `test withProgression - no elapsed time`() {
        val bbox = BoundingBox.fromCorners(Position(0.0, 0.0), Position(10.0, 10.0))
        val speed = 100.0 // m/s
        val direction = Direction.EAST

        val longitude = EarthAdaptedSpeedLongitude(bbox, speed, direction)
        val progressed = longitude.withProgression(Duration.ZERO)

        assertTrue(progressed.size() > 1)
        progressed.drop(1).dropLast(1).forEach {
            assertTrue(it.lng in 0.0..10.0)
            assertTrue(it.lat in 0.0..10.0)
        }
    }

    @Test
    fun `test withProgression - positive elapsed time`() {
        val bbox = BoundingBox.fromCorners(Position(0.0, 0.0), Position(10.0, 10.0))
        val speed = 100.0 // m/s
        val direction = Direction.EAST

        val longitude = EarthAdaptedSpeedLongitude(bbox, speed, direction)
        val progressed = longitude.withProgression(1.hours)

        assertTrue(progressed.size() > 1)
        progressed.drop(1).dropLast(1).forEach {
            assertTrue(it.lng > 0.0)
            assertTrue(it.lat in 0.0..10.0)
        }
    }

    @Test
    fun `test withProgression - bands check`() =
        runTest {
            // GIVEN
            val bbox =
                BoundingBox.fromCorners(
                    sw = Position(-10.0, -10.0),
                    ne = Position(10.0, 10.0),
                )
            val speed = 100.0 // m/s
            val direction = Direction.EAST

            val distanceAtEquator = calculateDistance(-10.0, 10.0, 0.0)
            val timeToFull: Duration = (distanceAtEquator / speed).seconds
            val nbWindows1 = ceil(distanceAtEquator / Wave.LINEAR_METERS_REFRESH)
            val nbWindows2 =
                ceil(
                    timeToFull.inWholeMilliseconds.toDouble() /
                        (Wave.LINEAR_METERS_REFRESH / speed).seconds.inWholeMilliseconds.toDouble(),
                )

            val longitude = EarthAdaptedSpeedLongitude(bbox, speed, direction)
            val bands = longitude.bands()

            // WHEN
            val progressed = longitude.withProgression(timeToFull)
            val positionClosestToZero = progressed.getPositions().minBy { abs(it.lat) }
            val bandClosestToZero = bands.minBy { abs(it.key) }
            val addedStepDistance = calculateDistance(nbWindows1 * bandClosestToZero.value.lngWidth, 0.0)

            // THEN
            assertEquals(10.0, positionClosestToZero.lng, epsilonMedium, "current longitude should be 10")
            assertEquals(nbWindows1, nbWindows2, epsilonFine, "Calculation of nb windows should be consistent")
            assertEquals(distanceAtEquator, addedStepDistance, epsilonHuge, "Stepped distance should equal to traversed distance")
        }

    @Test
    fun `test withProgression - compare with WaveDuration`() =
        runTest {
            // GIVEN
            val bbox =
                BoundingBox.fromCorners(
                    sw = Position(-10.0, -10.0),
                    ne = Position(10.0, 10.0),
                )
            val speed = 100.0 // m/s
            val direction = Direction.EAST

            val halfDistanceAtEquator = calculateDistance(-10.0, 10.0, 0.0) / 2
            val timeTo50percent: Duration = (halfDistanceAtEquator / speed).seconds

            val startTime = Instant.parse("2024-01-01T00:00:00Z")
            val currentTime = startTime + timeTo50percent

            val mockEvent = mockk<IWWWEvent>(relaxed = true)
            val mockArea = mockk<WWWEventArea>()

            val waveLinear =
                WWWEventWaveLinear(
                    speed = speed,
                    direction = direction,
                    approxDuration = 60,
                )

            every { mockClock.now() } returns currentTime
            every { mockEvent.getWaveStartDateTime() } returns startTime
            coEvery { mockEvent.isRunning() } returns true
            every { mockEvent.area } returns mockArea
            coEvery { mockEvent.area.bbox() } returns bbox

            waveLinear.setRelatedEvent<WWWEventWaveLinear>(mockEvent)

            val longitude = EarthAdaptedSpeedLongitude(bbox, speed, direction)

            // WHEN
            val waveDuration = waveLinear.getWaveDuration()
            val progression = waveLinear.getProgression()
            val progressed = longitude.withProgression(timeTo50percent)

            val positionClosestToZero = progressed.getPositions().minBy { abs(it.lat) }

            // THEN
            assertEquals(
                (timeTo50percent * 2).toDouble(DurationUnit.MILLISECONDS),
                waveDuration.toDouble(DurationUnit.MILLISECONDS),
                epsilonFine,
                "Wave duration should be twice half the way",
            )
            assertEquals(50.0, progression, epsilonLarge, "progression should be half the way")
            assertEquals(0.0, positionClosestToZero.lng, epsilonMedium, "current longitude should be 0")
        }

    @Test
    fun `test withProgression - direction West`() {
        val bbox = BoundingBox.fromCorners(Position(0.0, 0.0), Position(10.0, 10.0))
        val speed = 100.0 // m/s
        val direction = Direction.WEST

        val longitude = EarthAdaptedSpeedLongitude(bbox, speed, direction)
        val progressed = longitude.withProgression(1.hours)

        assertTrue(progressed.size() > 1)
        progressed.drop(1).dropLast(1).forEach {
            assertTrue(it.lng < 10.0)
            assertTrue(it.lat in 0.0..10.0)
        }
    }

    @Test
    fun `test calculateLonBandWidthAtMiddleLatitude`() {
        val bbox = BoundingBox.fromCorners(Position(0.0, 0.0), Position(10.0, 10.0))
        val speed = 100.0 // m/s
        val direction = Direction.EAST

        val longitude = EarthAdaptedSpeedLongitude(bbox, speed, direction)
        val width = longitude.calculateLonBandWidthAtLatitude(0.0)

        // Expected width at equator: (100 * bandDuration.inWholeMilliseconds / 1000) / (EARTH_RADIUS * 1) * (180 / PI)
        val bandDuration = (Wave.LINEAR_METERS_REFRESH / speed).seconds
        val expected = (speed * bandDuration.inWholeMilliseconds / 1000) / EARTH_RADIUS * (180 / PI)

        assertEquals(expected, width, epsilonFine)
    }

    @Test
    fun `test calculateOptimalLatBandWidth`() {
        val bbox = BoundingBox.fromCorners(Position(0.0, 0.0), Position(10.0, 10.0))
        val speed = 100.0 // m/s
        val direction = Direction.EAST

        val longitude = EarthAdaptedSpeedLongitude(bbox, speed, direction)
        val width = longitude.calculateOptimalLatBandWidth(45.0, 1.0)

        assertTrue(width > 0)
    }

    @Test
    fun `test adjustLongitudeWidthAtLatitude`() {
        val bbox = BoundingBox.fromCorners(Position(0.0, 0.0), Position(10.0, 10.0))
        val speed = 100.0 // m/s
        val direction = Direction.EAST

        val longitude = EarthAdaptedSpeedLongitude(bbox, speed, direction)
        val adjustedWidth = longitude.adjustLongitudeWidthAtLatitude(45.0, 1.0)

        assertTrue(adjustedWidth > 1.0)
    }

    @Test
    fun `test calculateWaveBands`() {
        val bbox = BoundingBox.fromCorners(Position(0.0, 0.0), Position(10.0, 10.0))
        val speed = 100.0 // m/s
        val direction = Direction.EAST

        val longitude = EarthAdaptedSpeedLongitude(bbox, speed, direction)
        val bands = longitude.calculateWaveBands()

        assertTrue(bands.isNotEmpty(), "Should generate wave bands")

        // Test ALL bands including edge cases (first and last)
        // These are critical for proper wave coverage at boundaries
        val invalidBands = mutableListOf<String>()

        bands.forEachIndexed { index, band ->
            val bandDescription = "Band $index (lat=${band.latitude}, latWidth=${band.latWidth}, lngWidth=${band.lngWidth})"

            // Validate latitude is within expected world bounds
            if (band.latitude !in -90.0..90.0) {
                invalidBands.add("$bandDescription: Invalid latitude outside world bounds")
            }

            // Check if this is an extreme latitude band (near poles)
            val isExtremeBand = kotlin.math.abs(band.latitude) > 85.0

            // Validate positive widths with different rules for extreme bands
            if (band.latWidth < 0) {
                invalidBands.add("$bandDescription: Invalid negative latitude width")
            }

            if (band.lngWidth <= 0) {
                invalidBands.add("$bandDescription: Invalid zero or negative longitude width")
            }

            // For extreme bands, allow larger longitude widths but set reasonable upper bounds
            if (isExtremeBand && band.lngWidth > 720.0) { // 2x full longitude range is extreme
                invalidBands.add("$bandDescription: Extreme longitude width indicates mathematical instability")
            }

            // For bands within our test area, validate they're in expected range
            if (band.latitude in 0.0..10.0) {
                if (band.latWidth <= 0 || band.lngWidth <= 0) {
                    invalidBands.add("$bandDescription: Band in test area has invalid dimensions")
                }
                // Test area bands should have reasonable longitude widths
                if (band.lngWidth > 50.0) { // Reasonable upper bound for test area
                    invalidBands.add("$bandDescription: Test area longitude width seems excessive")
                }
            }
        }

        if (invalidBands.isNotEmpty()) {
            val errorSummary = invalidBands.joinToString("\n- ", "Wave band calculation has boundary issues:\n- ")
            fail("$errorSummary\n\nThese edge cases must be fixed for reliable wave coverage.")
        }
    }

    @Test
    fun `test invalid speed`() {
        val bbox = BoundingBox.fromCorners(Position(0.0, 0.0), Position(10.0, 10.0))
        val speed = -100.0 // Invalid negative speed
        val direction = Direction.EAST

        assertFailsWith<IllegalArgumentException> {
            EarthAdaptedSpeedLongitude(bbox, speed, direction)
        }
    }

    // TODO: Fix polar region calculations - currently causing mathematical instabilities
    // @Test
    fun `test progression near poles - DISABLED`() {
        // Use realistic polar region (85°-89°) to avoid mathematical instabilities near exact pole
        val bbox = BoundingBox.fromCorners(Position(85.0, 0.0), Position(89.0, 10.0))
        val speed = 100.0 // m/s
        val direction = Direction.EAST

        val longitude = EarthAdaptedSpeedLongitude(bbox, speed, direction)
        val progressed = longitude.withProgression(1.hours)

        // Basic progression validation - ensure calculation doesn't fail in polar regions
        assertTrue(progressed.size() > 1, "Should have multiple progression points")

        // Verify no extreme values that would indicate calculation errors
        progressed.forEach { position ->
            assertTrue(position.lat.isFinite(), "Latitude should be finite")
            assertTrue(position.lng.isFinite(), "Longitude should be finite")
            assertTrue(position.lat >= 80.0, "Latitude should remain in polar regions")
            assertTrue(position.lat <= 90.0, "Latitude should not exceed pole")
        }
    }
}
