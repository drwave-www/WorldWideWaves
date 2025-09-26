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

package com.worldwidewaves.shared.testing

import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.events.utils.IClock
import io.mockk.every
import io.mockk.mockk
import kotlin.random.Random
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Test data builders for consistent, reusable test fixtures.
 * Provides builder pattern for creating test data with sensible defaults
 * and deterministic randomization using fixed seeds.
 */
object TestDataBuilders {

    // Fixed constants for deterministic testing
    const val FIXED_SEED = 42L
    const val BASE_TIMESTAMP = 1643723400000L // 2022-02-01 12:00:00 UTC

    /**
     * Builder for creating test positions with validation
     */
    class PositionBuilder {
        private var lat: Double = 40.7831 // Central Park default
        private var lng: Double = -73.9712
        private var source: String = "GPS"

        fun lat(latitude: Double) = apply {
            require(latitude in -90.0..90.0) { "Invalid latitude: $latitude" }
            this.lat = latitude
        }

        fun lng(longitude: Double) = apply {
            require(longitude in -180.0..180.0) { "Invalid longitude: $longitude" }
            this.lng = longitude
        }

        fun source(positionSource: String) = apply { this.source = positionSource }

        fun centralPark() = apply { lat = 40.7831; lng = -73.9712 }
        fun timesSquare() = apply { lat = 40.7589; lng = -73.9851 }
        fun london() = apply { lat = 51.5074; lng = -0.1278 }
        fun paris() = apply { lat = 48.8566; lng = 2.3522 }

        fun build(): Position = Position(lat = lat, lng = lng)
    }

    /**
     * Builder for creating test polygons with various shapes
     */
    class PolygonBuilder {
        private var centerLat: Double = 40.7831
        private var centerLng: Double = -73.9712
        private var radiusKm: Double = 2.0
        private var vertices: Int = 6
        private var seed: Long = FIXED_SEED

        fun center(lat: Double, lng: Double) = apply {
            this.centerLat = lat
            this.centerLng = lng
        }

        fun radius(km: Double) = apply {
            require(km > 0) { "Radius must be positive: $km" }
            this.radiusKm = km
        }

        fun vertices(count: Int) = apply {
            require(count >= 3) { "Need at least 3 vertices: $count" }
            this.vertices = count
        }

        fun seed(randomSeed: Long) = apply { this.seed = randomSeed }

        fun centralPark() = apply {
            center(40.7829, -73.9654)
            radius(1.5)
        }

        fun manhattan() = apply {
            center(40.7831, -73.9712)
            radius(5.0)
            vertices(8)
        }

        fun build(): List<Position> {
            val random = Random(seed)
            val radiusDegrees = radiusKm / 111.0 // Approximate km to degrees

            return (0 until vertices).map { i ->
                val angle = 2 * kotlin.math.PI * i / vertices
                val r = radiusDegrees * (0.8 + random.nextDouble() * 0.4) // Slight variation
                Position(
                    lat = centerLat + r * kotlin.math.cos(angle),
                    lng = centerLng + r * kotlin.math.sin(angle)
                )
            } + listOf(Position(
                lat = centerLat + radiusDegrees * kotlin.math.cos(0.0),
                lng = centerLng + radiusDegrees * kotlin.math.sin(0.0)
            )) // Close polygon
        }
    }

    /**
     * Builder for creating test clocks with deterministic time
     */
    class ClockBuilder {
        private var baseTime: kotlinx.datetime.Instant = kotlinx.datetime.Instant.fromEpochMilliseconds(BASE_TIMESTAMP)
        private var autoAdvance: Boolean = false
        private var advanceStep: kotlin.time.Duration = 1.minutes

        fun baseTime(time: kotlinx.datetime.Instant) = apply { this.baseTime = time }
        fun baseTime(epochMillis: Long) = apply {
            this.baseTime = kotlinx.datetime.Instant.fromEpochMilliseconds(epochMillis)
        }

        fun autoAdvance(enabled: Boolean = true) = apply { this.autoAdvance = enabled }
        fun advanceStep(step: kotlin.time.Duration) = apply { this.advanceStep = step }

        fun fixedAt2022() = apply { baseTime(BASE_TIMESTAMP) }
        fun fixedAtNow() = apply { baseTime(kotlinx.datetime.Clock.System.now()) }

        fun build(): IClock {
            val mockClock = mockk<IClock>()

            if (autoAdvance) {
                var currentTime = baseTime
                every { mockClock.now() } answers {
                    val result = currentTime
                    currentTime = currentTime.plus(advanceStep)
                    result
                }
            } else {
                every { mockClock.now() } returns baseTime
            }

            return mockClock
        }
    }

    /**
     * Builder for creating test events with realistic configurations
     */
    class EventBuilder {
        private var id: String = "test-event"
        private var name: String = "Test Event"
        private var polygon: List<Position> = PolygonBuilder().centralPark().build()
        private var startTime: kotlinx.datetime.Instant = kotlinx.datetime.Instant.fromEpochMilliseconds(BASE_TIMESTAMP)
        private var waveSpeedKmh: Double = 50.0
        private var clock: IClock = ClockBuilder().fixedAt2022().build()

        fun id(eventId: String) = apply { this.id = eventId }
        fun name(eventName: String) = apply { this.name = eventName }
        fun polygon(coords: List<Position>) = apply { this.polygon = coords }
        fun startTime(time: kotlinx.datetime.Instant) = apply { this.startTime = time }
        fun waveSpeed(kmh: Double) = apply {
            require(kmh > 0) { "Wave speed must be positive: $kmh" }
            this.waveSpeedKmh = kmh
        }
        fun clock(testClock: IClock) = apply { this.clock = testClock }

        fun centralParkEvent() = apply {
            id("central_park_wave")
            name("Central Park Wave")
            polygon(PolygonBuilder().centralPark().build())
        }

        fun runningEvent() = apply {
            startTime(kotlinx.datetime.Instant.fromEpochMilliseconds(BASE_TIMESTAMP - 30.minutes.inWholeMilliseconds))
        }

        fun futureEvent() = apply {
            startTime(kotlinx.datetime.Instant.fromEpochMilliseconds(BASE_TIMESTAMP + 2.hours.inWholeMilliseconds))
        }

        fun soonEvent() = apply {
            startTime(kotlinx.datetime.Instant.fromEpochMilliseconds(BASE_TIMESTAMP + 10.minutes.inWholeMilliseconds))
        }

        // Note: Actual build() would need to use real WWWEvent API
        fun buildData(): EventTestData {
            return EventTestData(
                id = id,
                name = name,
                polygon = polygon,
                startTime = startTime,
                waveSpeedKmh = waveSpeedKmh,
                clock = clock
            )
        }
    }

    // Factory methods for common test scenarios
    fun position(): PositionBuilder = PositionBuilder()
    fun polygon(): PolygonBuilder = PolygonBuilder()
    fun clock(): ClockBuilder = ClockBuilder()
    fun event(): EventBuilder = EventBuilder()

    // Quick access to common positions
    object CommonPositions {
        val CENTRAL_PARK = Position(lat = 40.7831, lng = -73.9712)
        val TIMES_SQUARE = Position(lat = 40.7589, lng = -73.9851)
        val LONDON = Position(lat = 51.5074, lng = -0.1278)
        val PARIS = Position(lat = 48.8566, lng = 2.3522)
        val TOKYO = Position(lat = 35.6762, lng = 139.6503)
        val SYDNEY = Position(lat = -33.8688, lng = 151.2093)

        // Boundary test positions
        val NORTH_POLE = Position(lat = 90.0, lng = 0.0)
        val SOUTH_POLE = Position(lat = -90.0, lng = 0.0)
        val PRIME_MERIDIAN = Position(lat = 0.0, lng = 0.0)
        val DATELINE_EAST = Position(lat = 0.0, lng = 179.999)
        val DATELINE_WEST = Position(lat = 0.0, lng = -179.999)
    }

    // Deterministic random generators
    object RandomGenerators {
        fun positions(count: Int, seed: Long = FIXED_SEED): List<Position> {
            val random = Random(seed)
            return (1..count).map {
                Position(
                    lat = random.nextDouble() * 180.0 - 90.0,
                    lng = random.nextDouble() * 360.0 - 180.0
                )
            }
        }

        fun nycAreaPositions(count: Int, seed: Long = FIXED_SEED): List<Position> {
            val random = Random(seed)
            return (1..count).map {
                Position(
                    lat = 40.7 + random.nextDouble() * 0.2,
                    lng = -74.1 + random.nextDouble() * 0.2
                )
            }
        }
    }

    // Data transfer objects for test data
    data class EventTestData(
        val id: String,
        val name: String,
        val polygon: List<Position>,
        val startTime: kotlinx.datetime.Instant,
        val waveSpeedKmh: Double,
        val clock: IClock
    )
}

// Extension functions for fluent test writing
fun Position.distanceTo(other: Position): Double {
    val latDiff = this.lat - other.lat
    val lngDiff = this.lng - other.lng
    return kotlin.math.sqrt(latDiff * latDiff + lngDiff * lngDiff) * 111.0 // Approximate km
}

fun Position.isNear(other: Position, toleranceKm: Double = 1.0): Boolean {
    return this.distanceTo(other) <= toleranceKm
}