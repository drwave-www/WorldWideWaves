package com.worldwidewaves.shared.testing

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

import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.WWWEvent
import com.worldwidewaves.shared.events.WWWEventArea
import com.worldwidewaves.shared.events.WWWEventMap
import com.worldwidewaves.shared.events.WWWEventWave
import com.worldwidewaves.shared.events.WWWEventWaveLinear
import com.worldwidewaves.shared.utils.GeoPosition
import io.mockk.every
import io.mockk.mockk
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Comprehensive test helpers for creating consistent test data and assertions.
 *
 * This object provides factory methods for creating test events, mock implementations,
 * and assertion utilities specifically designed for WorldWideWaves testing.
 */
@OptIn(ExperimentalTime::class)
object TestHelpers {

    // Standard test locations
    object TestLocations {
        val PARIS = GeoPosition(48.8566, 2.3522)
        val LONDON = GeoPosition(51.5074, -0.1278)
        val NEW_YORK = GeoPosition(40.7128, -74.0060)
        val TOKYO = GeoPosition(35.6762, 139.6503)
        val SYDNEY = GeoPosition(-33.8688, 151.2093)
        val SAO_PAULO = GeoPosition(-23.5505, -46.6333)
    }

    // Standard test times
    object TestTimes {
        val BASE_TIME = Instant.fromEpochMilliseconds(1640995200000L) // 2022-01-01 00:00:00 UTC
        val MORNING_TIME = Instant.fromEpochMilliseconds(1641020400000L) // 2022-01-01 07:00:00 UTC
        val EVENING_TIME = Instant.fromEpochMilliseconds(1641063600000L) // 2022-01-01 18:00:00 UTC
    }

    /**
     * Creates a fully configured test event with reasonable defaults.
     * All parameters can be overridden for specific test scenarios.
     */
    fun createTestEvent(
        id: String = "test_event",
        type: String = "city",
        country: String? = "france",
        community: String? = "paris",
        timeZone: String = "Europe/Paris",
        date: String = "2022-06-15",
        startHour: String = "18:00",
        instagramAccount: String = "worldwidewaves",
        instagramHashtag: String = "#WorldWideWaves",
        userPosition: GeoPosition? = TestLocations.PARIS,
        waveDuration: Duration = 2.hours,
        waveSpeed: Double = 50.0, // km/h
        area: WWWEventArea? = null,
        map: WWWEventMap? = null,
        configureWave: (WWWEventWave) -> Unit = {}
    ): WWWEvent {
        val mockArea = area ?: createMockArea(userPosition = userPosition)
        val mockMap = map ?: createMockMap()
        val waveDefinition = createMockWaveDefinition(
            duration = waveDuration,
            speed = waveSpeed,
            userPosition = userPosition,
            configureWave = configureWave
        )

        return WWWEvent(
            id = id,
            type = type,
            country = country,
            community = community,
            timeZone = timeZone,
            date = date,
            startHour = startHour,
            instagramAccount = instagramAccount,
            instagramHashtag = instagramHashtag,
            wavedef = waveDefinition,
            area = mockArea,
            map = mockMap,
        )
    }

    /**
     * Creates a test event that is currently running.
     */
    fun createRunningEvent(
        id: String = "running_event",
        startedAgo: Duration = 30.minutes,
        totalDuration: Duration = 2.hours,
        userPosition: GeoPosition? = TestLocations.PARIS
    ): WWWEvent {
        val now = TestTimes.BASE_TIME
        val startTime = now - startedAgo
        val startDateTime = startTime.toString().substring(0, 10) // Extract date part
        val startHour = startTime.toString().substring(11, 16) // Extract hour:minute part

        return createTestEvent(
            id = id,
            date = startDateTime,
            startHour = startHour,
            userPosition = userPosition,
            waveDuration = totalDuration
        )
    }

    /**
     * Creates a test event that is in the future (NEXT or SOON status).
     */
    fun createFutureEvent(
        id: String = "future_event",
        startsIn: Duration = 5.hours,
        userPosition: GeoPosition? = TestLocations.PARIS
    ): WWWEvent {
        val now = TestTimes.BASE_TIME
        val startTime = now + startsIn
        val startDateTime = startTime.toString().substring(0, 10)
        val startHour = startTime.toString().substring(11, 16)

        return createTestEvent(
            id = id,
            date = startDateTime,
            startHour = startHour,
            userPosition = userPosition
        )
    }

    /**
     * Creates a test event that has already finished (DONE status).
     */
    fun createCompletedEvent(
        id: String = "completed_event",
        endedAgo: Duration = 1.hours,
        totalDuration: Duration = 30.minutes,
        userPosition: GeoPosition? = TestLocations.PARIS
    ): WWWEvent {
        val now = TestTimes.BASE_TIME
        val endTime = now - endedAgo
        val startTime = endTime - totalDuration
        val startDateTime = startTime.toString().substring(0, 10)
        val startHour = startTime.toString().substring(11, 16)

        return createTestEvent(
            id = id,
            date = startDateTime,
            startHour = startHour,
            userPosition = userPosition,
            waveDuration = totalDuration
        )
    }

    /**
     * Creates a mock event area with configurable position containment.
     */
    fun createMockArea(
        userPosition: GeoPosition? = null,
        isUserInArea: Boolean = true
    ): WWWEventArea {
        val mockArea = mockk<WWWEventArea>(relaxed = true)
        every { mockArea.validationErrors() } returns null
        every { mockArea.setRelatedEvent(any()) } returns Unit

        if (userPosition != null) {
            every { mockArea.isPositionWithin(userPosition) } returns isUserInArea
        }
        every { mockArea.isPositionWithin(any()) } returns isUserInArea

        return mockArea
    }

    /**
     * Creates a mock event map.
     */
    fun createMockMap(): WWWEventMap {
        val mockMap = mockk<WWWEventMap>(relaxed = true)
        every { mockMap.validationErrors() } returns null
        every { mockMap.setRelatedEvent(any()) } returns Unit
        return mockMap
    }

    /**
     * Creates a mock wave definition with configurable behavior.
     */
    fun createMockWaveDefinition(
        duration: Duration = 2.hours,
        speed: Double = 50.0,
        userPosition: GeoPosition? = null,
        progression: Double = 0.0,
        configureWave: (WWWEventWave) -> Unit = {}
    ): WWWEvent.WWWWaveDefinition {
        val mockWave = createMockWave(
            duration = duration,
            speed = speed,
            userPosition = userPosition,
            progression = progression,
            configureWave = configureWave
        )

        val mockLinear = mockk<WWWEventWaveLinear>(relaxed = true)
        every { mockLinear.validationErrors() } returns null
        every { mockLinear.setRelatedEvent<WWWEventWave>(any()) } answers {
            // Return the mock wave instead of the linear wave
            mockWave
        }

        return WWWEvent.WWWWaveDefinition(linear = mockLinear)
    }

    /**
     * Creates a mock wave with comprehensive behavior configuration.
     */
    fun createMockWave(
        duration: Duration = 2.hours,
        speed: Double = 50.0,
        userPosition: GeoPosition? = null,
        progression: Double = 0.0,
        timeBeforeHit: Duration? = null,
        hasBeenHit: Boolean = false,
        userPositionRatio: Double? = null,
        hitDateTime: Instant? = null,
        configureWave: (WWWEventWave) -> Unit = {}
    ): WWWEventWave {
        val mockWave = mockk<WWWEventWave>(relaxed = true)

        every { mockWave.getWaveDuration() } returns duration
        every { mockWave.getApproxDuration() } returns duration
        every { mockWave.getLiteralSpeed() } returns "${speed.toInt()} km/h"
        every { mockWave.getProgression() } returns progression
        every { mockWave.getUserPosition() } returns userPosition
        every { mockWave.timeBeforeUserHit() } returns timeBeforeHit
        every { mockWave.hasUserBeenHitInCurrentPosition() } returns hasBeenHit
        every { mockWave.userPositionToWaveRatio() } returns userPositionRatio
        every { mockWave.userHitDateTime() } returns hitDateTime
        every { mockWave.setRelatedEvent<WWWEventWave>(any()) } returns Unit

        configureWave(mockWave)
        return mockWave
    }

    /**
     * Creates a collection of test events with different statuses for comprehensive testing.
     */
    fun createTestEventSuite(): List<WWWEvent> = listOf(
        createRunningEvent("running_city", userPosition = TestLocations.PARIS),
        createFutureEvent("future_country", startsIn = 5.hours, userPosition = TestLocations.LONDON),
        createCompletedEvent("completed_world", endedAgo = 1.hours, userPosition = TestLocations.NEW_YORK),
        createTestEvent("soon_event", date = "2022-01-02", userPosition = TestLocations.TOKYO), // Soon event
    )

    /**
     * Performance testing utilities for measuring execution time.
     */
    object PerformanceHelpers {
        inline fun <T> measureTime(operation: () -> T): Pair<T, Duration> {
            val start = TestTimes.BASE_TIME
            val result = operation()
            val end = TestTimes.BASE_TIME + 100.minutes // Simulated end time
            return Pair(result, end - start)
        }

        fun simulateHeavyLoad(events: List<IWWWEvent>): List<IWWWEvent> {
            // Simulate processing load for performance testing
            return events.map { event ->
                // Simulate heavy computation
                repeat(1000) {
                    event.id.hashCode()
                }
                event
            }
        }
    }

    /**
     * Geographic calculation testing utilities.
     */
    object GeoHelpers {
        fun createPolygonPoints(
            center: GeoPosition,
            radius: Double = 0.01, // Degrees
            sides: Int = 6
        ): List<GeoPosition> {
            val points = mutableListOf<GeoPosition>()
            val angleStep = 360.0 / sides

            for (i in 0 until sides) {
                val angle = Math.toRadians(i * angleStep)
                val lat = center.latitude + radius * kotlin.math.cos(angle)
                val lng = center.longitude + radius * kotlin.math.sin(angle)
                points.add(GeoPosition(lat, lng))
            }

            return points
        }

        fun calculateDistance(pos1: GeoPosition, pos2: GeoPosition): Double {
            // Simplified Haversine formula for testing
            val dLat = Math.toRadians(pos2.latitude - pos1.latitude)
            val dLng = Math.toRadians(pos2.longitude - pos1.longitude)
            val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
                    kotlin.math.cos(Math.toRadians(pos1.latitude)) *
                    kotlin.math.cos(Math.toRadians(pos2.latitude)) *
                    kotlin.math.sin(dLng / 2) * kotlin.math.sin(dLng / 2)
            val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
            return 6371 * c // Earth radius in kilometers
        }
    }

    /**
     * Assertion helpers for common test scenarios.
     */
    object AssertionHelpers {
        fun assertEventIsValid(event: IWWWEvent) {
            val errors = event.validationErrors()
            if (errors != null) {
                throw AssertionError("Event validation failed: ${errors.joinToString(", ")}")
            }
        }

        fun assertStatusTransition(
            from: IWWWEvent.Status,
            to: IWWWEvent.Status,
            message: String = "Invalid status transition"
        ) {
            val validTransitions = mapOf(
                IWWWEvent.Status.NEXT to setOf(IWWWEvent.Status.SOON, IWWWEvent.Status.RUNNING),
                IWWWEvent.Status.SOON to setOf(IWWWEvent.Status.RUNNING, IWWWEvent.Status.NEXT),
                IWWWEvent.Status.RUNNING to setOf(IWWWEvent.Status.DONE),
                IWWWEvent.Status.DONE to emptySet()
            )

            val allowedTransitions = validTransitions[from] ?: emptySet()
            if (to !in allowedTransitions && from != to) {
                throw AssertionError("$message: $from -> $to is not a valid transition")
            }
        }

        fun assertTimeOrderCorrect(start: Instant, end: Instant) {
            if (start >= end) {
                throw AssertionError("Start time ($start) must be before end time ($end)")
            }
        }

        fun assertPositionInBounds(
            position: GeoPosition,
            minLat: Double = -90.0,
            maxLat: Double = 90.0,
            minLng: Double = -180.0,
            maxLng: Double = 180.0
        ) {
            if (position.latitude < minLat || position.latitude > maxLat) {
                throw AssertionError("Latitude ${position.latitude} is out of bounds [$minLat, $maxLat]")
            }
            if (position.longitude < minLng || position.longitude > maxLng) {
                throw AssertionError("Longitude ${position.longitude} is out of bounds [$minLng, $maxLng]")
            }
        }
    }
}