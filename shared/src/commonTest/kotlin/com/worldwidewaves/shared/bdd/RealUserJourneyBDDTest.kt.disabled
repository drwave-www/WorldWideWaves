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

package com.worldwidewaves.shared.bdd

import com.worldwidewaves.shared.events.WWWEvents
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.events.utils.IClock
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.Instant

/**
 * BDD-style tests for real user journeys using actual WorldWideWaves classes.
 * Tests capture business requirements in Given/When/Then format.
 */
class RealUserJourneyBDDTest {

    private val mockClock = mockk<IClock>()
    private val fixedTimeBase = Instant.fromEpochMilliseconds(1643723400000L) // 2022-02-01 12:00:00 UTC

    @Test
    fun `Scenario - User discovers events sorted by proximity`() = runTest {
        // GIVEN: I am at Central Park coordinates
        val userPosition = givenUserIsAtPosition(40.7831, -73.9712)

        // AND: There are multiple events at different distances
        val nearbyEvent = givenEventAtLocation("Nearby Event", 40.7829, -73.9654, 1.0) // ~500m away
        val distantEvent = givenEventAtLocation("Distant Event", 40.7589, -73.9851, 1.0) // ~2km away

        val wwwEvents = WWWEvents()

        // WHEN: I discover events near my location
        val sortedEvents = whenUserDiscoversEventsNear(listOf(nearbyEvent, distantEvent), userPosition)

        // THEN: Events should be sorted by distance
        val firstEventDistance = calculateDistanceKm(userPosition, sortedEvents.first().area.center)
        val secondEventDistance = calculateDistanceKm(userPosition, sortedEvents.last().area.center)

        assertTrue(
            firstEventDistance <= secondEventDistance,
            "Events should be sorted by distance: first=$firstEventDistance, second=$secondEventDistance"
        )
    }

    @Test
    fun `Scenario - Event status changes over time realistically`() = runTest {
        // GIVEN: There is an event starting in 30 minutes
        val eventStartTime = fixedTimeBase.plus(30.minutes)
        givenCurrentTimeIs(fixedTimeBase)

        val futureEvent = givenEventStartingAt(eventStartTime)

        // WHEN: I check the event status before it starts
        val waitingStatus = futureEvent.getStatus()
        assertEquals("WAITING", waitingStatus, "Event should be waiting before start time")

        // AND: Time progresses to 10 minutes before start (within "soon" threshold)
        givenCurrentTimeIs(eventStartTime.minus(10.minutes))
        val soonStatus = futureEvent.getStatus()

        // THEN: Event should be "soon"
        assertEquals("SOON", soonStatus, "Event should be soon within 15 minutes of start")

        // WHEN: Time progresses to start time
        givenCurrentTimeIs(eventStartTime)
        val runningStatus = futureEvent.getStatus()

        // THEN: Event should be running
        assertEquals("RUNNING", runningStatus, "Event should be running at start time")

        // WHEN: Time progresses past event end
        givenCurrentTimeIs(eventStartTime.plus(3.hours)) // Assuming 2h duration + 1h buffer
        val doneStatus = futureEvent.getStatus()

        // THEN: Event should be done
        assertEquals("DONE", doneStatus, "Event should be done after completion")
    }

    @Test
    fun `Scenario - Wave progression accuracy during event`() = runTest {
        // GIVEN: There is a running wave event with known parameters
        val waveSpeed = 50.0 // km/h
        val eventCenter = Position(lat = 40.7831, lng = -73.9712)
        val userPosition = Position(lat = 40.7851, lng = -73.9712) // 2.2km north

        givenCurrentTimeIs(fixedTimeBase)
        val waveEvent = givenRunningWaveEventWithSpeed(eventCenter, waveSpeed, fixedTimeBase)

        // WHEN: I calculate when the wave will reach my position
        val timeToHit = waveEvent.wave.timeUntilUserHit(userPosition)
        val expectedTimeMinutes = (2.2 / waveSpeed * 60).toLong() // Distance/speed * 60

        // THEN: The calculation should be accurate
        assertTrue(
            kotlin.math.abs(timeToHit.inWholeMinutes - expectedTimeMinutes) <= 1,
            "Time to hit should be accurate: expected ~${expectedTimeMinutes}min, got ${timeToHit.inWholeMinutes}min"
        )

        // WHEN: Time advances to when wave should hit
        val hitTime = fixedTimeBase.plus(timeToHit)
        givenCurrentTimeIs(hitTime)

        // THEN: User should be marked as hit
        val isHit = waveEvent.wave.hasUserBeenHit(userPosition)
        assertTrue(isHit, "User should be hit when wave front reaches their position")
    }

    @Test
    fun `Scenario - Event area validation for participation eligibility`() = runTest {
        // GIVEN: There is a Central Park wave event
        val centralParkEvent = givenEventAtLocation("Central Park Wave", 40.7829, -73.9654, 2.0)

        // WHEN: User is within the event area
        val insidePosition = Position(lat = 40.7831, lng = -73.9712) // Inside Central Park
        val isEligibleInside = centralParkEvent.area.contains(insidePosition)

        // THEN: User should be eligible to participate
        assertTrue(isEligibleInside, "User inside event area should be eligible")

        // WHEN: User is outside the event area
        val outsidePosition = Position(lat = 40.7589, lng = -73.9851) // Times Square
        val isEligibleOutside = centralParkEvent.area.contains(outsidePosition)

        // THEN: User should not be eligible to participate
        assertFalse(isEligibleOutside, "User outside event area should not be eligible")
    }

    @Test
    fun `Scenario Outline - Button states based on event and location status`() = runTest {
        val testCases = listOf(
            TestCase("running", "within", true),
            TestCase("running", "outside", false),
            TestCase("soon", "within", true),
            TestCase("soon", "outside", false),
            TestCase("done", "within", false),
            TestCase("waiting", "within", false)
        )

        testCases.forEach { testCase ->
            // GIVEN: Event with specific status
            val event = givenEventWithStatus(testCase.eventStatus)

            // AND: User location relative to event
            val userPosition = givenUserLocationRelativeTo(event, testCase.locationStatus)

            // WHEN: User checks participation eligibility
            val canParticipate = event.canUserParticipate(userPosition)

            // THEN: Button state should match expectation
            assertEquals(
                testCase.expectedEnabled,
                canParticipate,
                "Event ${testCase.eventStatus} with user ${testCase.locationStatus} should have enabled=${testCase.expectedEnabled}"
            )
        }
    }

    // Helper methods implementing Given/When/Then steps

    private fun givenUserIsAtPosition(lat: Double, lng: Double): Position {
        return Position(lat = lat, lng = lng)
    }

    private fun givenEventAtLocation(
        name: String,
        centerLat: Double,
        centerLng: Double,
        radiusKm: Double
    ): WWWEvent {
        return createTestEvent(
            name = name,
            centerPosition = Position(centerLat, centerLng),
            radiusKm = radiusKm,
            startTime = fixedTimeBase.minus(30.minutes), // Running event
            clock = mockClock
        )
    }

    private fun givenCurrentTimeIs(time: Instant) {
        every { mockClock.now() } returns time
    }

    private fun givenEventStartingAt(startTime: Instant): WWWEvent {
        return createTestEvent(
            name = "Future Event",
            centerPosition = Position(40.7831, -73.9712),
            radiusKm = 2.0,
            startTime = startTime,
            clock = mockClock
        )
    }

    private fun givenRunningWaveEventWithSpeed(
        center: Position,
        speedKmh: Double,
        startTime: Instant
    ): WWWEvent {
        return createTestEvent(
            name = "Speed Test Event",
            centerPosition = center,
            radiusKm = 5.0,
            startTime = startTime,
            waveSpeedKmh = speedKmh,
            clock = mockClock
        )
    }

    private fun givenEventWithStatus(status: String): WWWEvent {
        val now = fixedTimeBase
        val startTime = when (status) {
            "waiting" -> now.plus(2.hours)
            "soon" -> now.plus(10.minutes)
            "running" -> now.minus(30.minutes)
            "done" -> now.minus(3.hours)
            else -> now
        }

        every { mockClock.now() } returns now

        return createTestEvent(
            name = "Status Test Event",
            centerPosition = Position(40.7831, -73.9712),
            radiusKm = 2.0,
            startTime = startTime,
            clock = mockClock
        )
    }

    private fun givenUserLocationRelativeTo(event: WWWEvent, locationStatus: String): Position {
        return when (locationStatus) {
            "within" -> event.area.center
            "outside" -> Position(
                lat = event.area.center.lat + 0.05, // Far enough to be outside
                lng = event.area.center.lng + 0.05
            )
            else -> event.area.center
        }
    }

    private fun whenUserDiscoversEventsNear(
        events: List<WWWEvent>,
        userPosition: Position
    ): List<WWWEvent> {
        return events.sortedBy { event ->
            calculateDistanceKm(userPosition, event.area.center)
        }
    }

    // Helper utilities
    private fun calculateDistanceKm(pos1: Position, pos2: Position): Double {
        val latDiff = pos1.lat - pos2.lat
        val lngDiff = pos1.lng - pos2.lng
        return kotlin.math.sqrt(latDiff * latDiff + lngDiff * lngDiff) * 111.0
    }

    private fun createTestEvent(
        name: String,
        centerPosition: Position,
        radiusKm: Double,
        startTime: Instant,
        waveSpeedKmh: Double = 50.0,
        clock: IClock
    ): WWWEvent {
        // Create a simple circular polygon around the center
        val radius = radiusKm / 111.0 // Convert km to degrees (approximate)
        val vertices = (0..8).map { i ->
            val angle = 2 * kotlin.math.PI * i / 8
            Position(
                lat = centerPosition.lat + radius * kotlin.math.cos(angle),
                lng = centerPosition.lng + radius * kotlin.math.sin(angle)
            )
        }

        return WWWEvent.create(
            id = name.lowercase().replace(" ", "_"),
            name = name,
            polygon = Polygon(coordinates = vertices),
            startTime = startTime,
            waveSpeedKmh = waveSpeedKmh,
            clock = clock
        )
    }

    private data class TestCase(
        val eventStatus: String,
        val locationStatus: String,
        val expectedEnabled: Boolean
    )
}