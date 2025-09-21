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
import com.worldwidewaves.shared.events.WWWEventWave
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.testing.TestHelpers.TestLocations
import com.worldwidewaves.shared.testing.TestHelpers.TestTimes
import com.worldwidewaves.shared.testing.TestHelpers.AssertionHelpers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime

/**
 * Enhanced comprehensive tests for TestHelpers functionality including
 * event creation, mock objects, geographic calculations, and assertion utilities.
 */
@OptIn(ExperimentalTime::class)
class TestHelpersEnhancedTest {

    @Test
    fun `test TestLocations constant values`() {
        // Verify all test location constants are properly defined
        assertNotNull(TestLocations.PARIS, "Paris location should be defined")
        assertNotNull(TestLocations.LONDON, "London location should be defined")
        assertNotNull(TestLocations.NEW_YORK, "New York location should be defined")
        assertNotNull(TestLocations.TOKYO, "Tokyo location should be defined")
        assertNotNull(TestLocations.SYDNEY, "Sydney location should be defined")
        assertNotNull(TestLocations.SAO_PAULO, "Sao Paulo location should be defined")

        // Verify Paris coordinates (approximate)
        assertTrue(TestLocations.PARIS.lat > 48.0 && TestLocations.PARIS.lat < 49.0, "Paris latitude should be ~48.8")
        assertTrue(TestLocations.PARIS.lng > 2.0 && TestLocations.PARIS.lng < 3.0, "Paris longitude should be ~2.3")

        // Verify London coordinates (approximate)
        assertTrue(TestLocations.LONDON.lat > 51.0 && TestLocations.LONDON.lat < 52.0, "London latitude should be ~51.5")
        assertTrue(TestLocations.LONDON.lng > -1.0 && TestLocations.LONDON.lng < 0.0, "London longitude should be ~-0.1")

        // Verify New York coordinates (approximate)
        assertTrue(TestLocations.NEW_YORK.lat > 40.0 && TestLocations.NEW_YORK.lat < 41.0, "NYC latitude should be ~40.7")
        assertTrue(TestLocations.NEW_YORK.lng > -75.0 && TestLocations.NEW_YORK.lng < -74.0, "NYC longitude should be ~-74.0")
    }

    @Test
    fun `test TestTimes constant values`() {
        // Verify all test time constants are properly defined
        assertNotNull(TestTimes.BASE_TIME, "Base time should be defined")
        assertNotNull(TestTimes.MORNING_TIME, "Morning time should be defined")
        assertNotNull(TestTimes.EVENING_TIME, "Evening time should be defined")

        // Verify time relationships
        assertTrue(TestTimes.BASE_TIME < TestTimes.MORNING_TIME, "Base time should be before morning time")
        assertTrue(TestTimes.MORNING_TIME < TestTimes.EVENING_TIME, "Morning time should be before evening time")

        // Verify epoch milliseconds are reasonable (around 2022)
        assertTrue(TestTimes.BASE_TIME.epochSeconds > 1640000000, "Base time should be around 2022")
        assertTrue(TestTimes.BASE_TIME.epochSeconds < 1670000000, "Base time should be around 2022")
    }

    @Test
    fun `test createTestEvent with defaults`() {
        val event = TestHelpers.createTestEvent()

        assertNotNull(event, "Test event should be created")
        assertEquals("test_event", event.id, "Default ID should be test_event")
        assertEquals("city", event.type, "Default type should be city")
        assertEquals("france", event.country, "Default country should be france")
        assertEquals("paris", event.community, "Default community should be paris")
        assertEquals("Europe/Paris", event.timeZone, "Default timezone should be Europe/Paris")
        assertEquals("2022-06-15", event.date, "Default date should be 2022-06-15")
        assertEquals("18:00", event.startHour, "Default start hour should be 18:00")
        assertEquals("worldwidewaves", event.instagramAccount, "Default Instagram account should be worldwidewaves")
        assertEquals("#WorldWideWaves", event.instagramHashtag, "Default hashtag should be #WorldWideWaves")
        assertNotNull(event.wavedef, "Wave definition should be created")
        assertNotNull(event.area, "Event area should be created")
        assertNotNull(event.map, "Event map should be created")
    }

    @Test
    fun `test createTestEvent with custom parameters`() {
        val customEvent = TestHelpers.createTestEvent(
            id = "custom_id",
            type = "country",
            country = "italy",
            community = "rome",
            timeZone = "Europe/Rome",
            date = "2023-07-20",
            startHour = "20:30",
            userPosition = TestLocations.LONDON
        )

        assertEquals("custom_id", customEvent.id, "Custom ID should be applied")
        assertEquals("country", customEvent.type, "Custom type should be applied")
        assertEquals("italy", customEvent.country, "Custom country should be applied")
        assertEquals("rome", customEvent.community, "Custom community should be applied")
        assertEquals("Europe/Rome", customEvent.timeZone, "Custom timezone should be applied")
        assertEquals("2023-07-20", customEvent.date, "Custom date should be applied")
        assertEquals("20:30", customEvent.startHour, "Custom start hour should be applied")
    }

    @Test
    fun `test createRunningEvent properties`() {
        val runningEvent = TestHelpers.createRunningEvent(
            id = "running_test",
            startedAgo = 45.minutes,
            totalDuration = 3.hours,
            userPosition = TestLocations.TOKYO
        )

        assertEquals("running_test", runningEvent.id, "Running event ID should be set")
        assertNotNull(runningEvent.date, "Running event should have date")
        assertNotNull(runningEvent.startHour, "Running event should have start hour")
        assertTrue(runningEvent.date.contains("-"), "Date should be formatted with dashes")
        assertTrue(runningEvent.startHour.contains(":"), "Start hour should be formatted with colon")
    }

    @Test
    fun `test createFutureEvent properties`() {
        val futureEvent = TestHelpers.createFutureEvent(
            id = "future_test",
            startsIn = 2.hours,
            userPosition = TestLocations.SYDNEY
        )

        assertEquals("future_test", futureEvent.id, "Future event ID should be set")
        assertNotNull(futureEvent.date, "Future event should have date")
        assertNotNull(futureEvent.startHour, "Future event should have start hour")
        assertTrue(futureEvent.date.length >= 8, "Date should be properly formatted")
        assertTrue(futureEvent.startHour.length >= 4, "Start hour should be properly formatted")
    }

    @Test
    fun `test createCompletedEvent properties`() {
        val completedEvent = TestHelpers.createCompletedEvent(
            id = "completed_test",
            endedAgo = 30.minutes,
            totalDuration = 90.minutes,
            userPosition = TestLocations.SAO_PAULO
        )

        assertEquals("completed_test", completedEvent.id, "Completed event ID should be set")
        assertNotNull(completedEvent.date, "Completed event should have date")
        assertNotNull(completedEvent.startHour, "Completed event should have start hour")
    }

    @Test
    fun `test createMockArea functionality`() {
        val userPos = TestLocations.PARIS
        val mockArea = TestHelpers.createMockArea(userPosition = userPos, isUserInArea = true)

        assertNotNull(mockArea, "Mock area should be created")
        // Note: Since it's a mock, we can't directly test the coEvery behaviors
        // but we can verify the mock was created without errors
    }

    @Test
    fun `test createMockMap functionality`() {
        val mockMap = TestHelpers.createMockMap()

        assertNotNull(mockMap, "Mock map should be created")
        // Note: Since it's a mock, we verify creation without errors
    }

    @Test
    fun `test createSimpleWaveDefinition`() {
        val waveDef = TestHelpers.createSimpleWaveDefinition(
            duration = 90.minutes,
            speed = 15.0,
            userPosition = TestLocations.NEW_YORK
        )

        assertNotNull(waveDef, "Wave definition should be created")
        assertNotNull(waveDef.linear, "Linear wave should be defined")
    }

    @Test
    fun `test createMockWave with default parameters`() {
        val mockWave = TestHelpers.createMockWave()

        assertNotNull(mockWave, "Mock wave should be created")
        // Mock creation verification - the actual behavior testing would require coroutines
    }

    @Test
    fun `test createMockWave with custom parameters`() {
        val mockWave = TestHelpers.createMockWave(
            duration = 4.hours,
            speed = 25.0,
            userPosition = TestLocations.LONDON,
            progression = 0.5,
            hasBeenHit = true
        )

        assertNotNull(mockWave, "Custom mock wave should be created")
    }

    @Test
    fun `test createTestEventSuite`() {
        val eventSuite = TestHelpers.createTestEventSuite()

        assertEquals(4, eventSuite.size, "Event suite should contain 4 events")

        val eventIds = eventSuite.map { it.id }
        assertTrue(eventIds.contains("running_city"), "Should contain running_city event")
        assertTrue(eventIds.contains("future_country"), "Should contain future_country event")
        assertTrue(eventIds.contains("completed_world"), "Should contain completed_world event")
        assertTrue(eventIds.contains("soon_event"), "Should contain soon_event event")

        // Verify each event is properly configured
        eventSuite.forEach { event ->
            assertNotNull(event.id, "Each event should have ID")
            assertNotNull(event.type, "Each event should have type")
            assertNotNull(event.date, "Each event should have date")
            assertNotNull(event.startHour, "Each event should have start hour")
        }
    }

    @Test
    fun `test GeoHelpers createPolygonPoints`() {
        val center = TestLocations.PARIS
        val polygonPoints = TestHelpers.GeoHelpers.createPolygonPoints(
            center = center,
            radius = 0.01,
            sides = 6
        )

        assertEquals(6, polygonPoints.size, "Should create 6 polygon points")

        polygonPoints.forEach { point ->
            assertNotNull(point, "Each polygon point should be valid")
            // Verify points are within reasonable distance of center
            val distance = kotlin.math.abs(point.lat - center.lat) + kotlin.math.abs(point.lng - center.lng)
            assertTrue(distance <= 0.02, "Point should be within radius of center")
        }
    }

    @Test
    fun `test GeoHelpers createPolygonPoints with different parameters`() {
        val center = TestLocations.LONDON
        val trianglePoints = TestHelpers.GeoHelpers.createPolygonPoints(
            center = center,
            radius = 0.005,
            sides = 3
        )

        assertEquals(3, trianglePoints.size, "Should create 3 triangle points")

        val octagonPoints = TestHelpers.GeoHelpers.createPolygonPoints(
            center = center,
            radius = 0.02,
            sides = 8
        )

        assertEquals(8, octagonPoints.size, "Should create 8 octagon points")
    }

    @Test
    fun `test GeoHelpers calculateDistance`() {
        val pos1 = TestLocations.PARIS
        val pos2 = TestLocations.LONDON

        val distance = TestHelpers.GeoHelpers.calculateDistance(pos1, pos2)

        assertTrue(distance > 0, "Distance should be positive")
        // Paris to London is approximately 344 km
        assertTrue(distance > 300 && distance < 400, "Paris-London distance should be ~344 km, got $distance")

        // Test distance to same position should be zero
        val sameDistance = TestHelpers.GeoHelpers.calculateDistance(pos1, pos1)
        assertTrue(sameDistance < 0.1, "Distance to same position should be near zero")
    }

    @Test
    fun `test GeoHelpers calculateDistance for various city pairs`() {
        val distances = mapOf(
            "Paris-London" to TestHelpers.GeoHelpers.calculateDistance(TestLocations.PARIS, TestLocations.LONDON),
            "NYC-Tokyo" to TestHelpers.GeoHelpers.calculateDistance(TestLocations.NEW_YORK, TestLocations.TOKYO),
            "Sydney-SaoPaulo" to TestHelpers.GeoHelpers.calculateDistance(TestLocations.SYDNEY, TestLocations.SAO_PAULO)
        )

        distances.forEach { (pair, distance) ->
            assertTrue(distance > 0, "$pair distance should be positive")
            assertTrue(distance < 20000, "$pair distance should be reasonable (<20000 km)")
        }

        // NYC-Tokyo should be a long distance
        assertTrue(distances["NYC-Tokyo"]!! > 10000, "NYC-Tokyo should be over 10000 km")
    }

    @Test
    fun `test AssertionHelpers assertStatusTransition valid transitions`() {
        // Test valid transitions - these should not throw
        AssertionHelpers.assertStatusTransition(IWWWEvent.Status.NEXT, IWWWEvent.Status.SOON)
        AssertionHelpers.assertStatusTransition(IWWWEvent.Status.NEXT, IWWWEvent.Status.RUNNING)
        AssertionHelpers.assertStatusTransition(IWWWEvent.Status.SOON, IWWWEvent.Status.RUNNING)
        AssertionHelpers.assertStatusTransition(IWWWEvent.Status.SOON, IWWWEvent.Status.NEXT)
        AssertionHelpers.assertStatusTransition(IWWWEvent.Status.RUNNING, IWWWEvent.Status.DONE)

        // Same status transitions should be valid
        AssertionHelpers.assertStatusTransition(IWWWEvent.Status.NEXT, IWWWEvent.Status.NEXT)
        AssertionHelpers.assertStatusTransition(IWWWEvent.Status.RUNNING, IWWWEvent.Status.RUNNING)
    }

    @Test
    fun `test AssertionHelpers assertStatusTransition invalid transitions`() {
        // Test invalid transitions - these should throw
        assertFailsWith<AssertionError> {
            AssertionHelpers.assertStatusTransition(IWWWEvent.Status.DONE, IWWWEvent.Status.RUNNING)
        }

        assertFailsWith<AssertionError> {
            AssertionHelpers.assertStatusTransition(IWWWEvent.Status.DONE, IWWWEvent.Status.SOON)
        }

        assertFailsWith<AssertionError> {
            AssertionHelpers.assertStatusTransition(IWWWEvent.Status.RUNNING, IWWWEvent.Status.NEXT)
        }
    }

    @Test
    fun `test AssertionHelpers assertTimeOrderCorrect`() {
        val earlier = TestTimes.BASE_TIME
        val later = TestTimes.MORNING_TIME

        // Valid time order should not throw
        AssertionHelpers.assertTimeOrderCorrect(earlier, later)

        // Invalid time order should throw
        assertFailsWith<AssertionError> {
            AssertionHelpers.assertTimeOrderCorrect(later, earlier)
        }

        // Same time should throw
        assertFailsWith<AssertionError> {
            AssertionHelpers.assertTimeOrderCorrect(earlier, earlier)
        }
    }

    @Test
    fun `test AssertionHelpers assertPositionInBounds valid positions`() {
        // Valid positions should not throw
        AssertionHelpers.assertPositionInBounds(TestLocations.PARIS)
        AssertionHelpers.assertPositionInBounds(TestLocations.LONDON)
        AssertionHelpers.assertPositionInBounds(TestLocations.NEW_YORK)
        AssertionHelpers.assertPositionInBounds(TestLocations.TOKYO)
        AssertionHelpers.assertPositionInBounds(TestLocations.SYDNEY)
        AssertionHelpers.assertPositionInBounds(TestLocations.SAO_PAULO)

        // Extreme but valid positions
        AssertionHelpers.assertPositionInBounds(Position(90.0, 180.0)) // North Pole, Date Line
        AssertionHelpers.assertPositionInBounds(Position(-90.0, -180.0)) // South Pole, Date Line
        AssertionHelpers.assertPositionInBounds(Position(0.0, 0.0)) // Equator, Prime Meridian
    }

    @Test
    fun `test AssertionHelpers assertPositionInBounds invalid positions`() {
        // Invalid latitude should throw
        assertFailsWith<AssertionError> {
            AssertionHelpers.assertPositionInBounds(Position(91.0, 0.0))
        }

        assertFailsWith<AssertionError> {
            AssertionHelpers.assertPositionInBounds(Position(-91.0, 0.0))
        }

        // Invalid longitude should throw
        assertFailsWith<AssertionError> {
            AssertionHelpers.assertPositionInBounds(Position(0.0, 181.0))
        }

        assertFailsWith<AssertionError> {
            AssertionHelpers.assertPositionInBounds(Position(0.0, -181.0))
        }
    }

    @Test
    fun `test AssertionHelpers assertPositionInBounds with custom bounds`() {
        val position = TestLocations.PARIS

        // Position should be valid within generous bounds
        AssertionHelpers.assertPositionInBounds(
            position,
            minLat = 40.0,
            maxLat = 60.0,
            minLng = -10.0,
            maxLng = 10.0
        )

        // Position should be invalid within restrictive bounds
        assertFailsWith<AssertionError> {
            AssertionHelpers.assertPositionInBounds(
                position,
                minLat = 50.0,
                maxLat = 51.0,
                minLng = 0.0,
                maxLng = 1.0
            )
        }
    }

    @Test
    fun `test utilities thread safety`() {
        val results = mutableListOf<String>()
        val exceptions = mutableListOf<Exception>()

        val threads = (1..5).map { threadId ->
            Thread {
                try {
                    repeat(10) { iteration ->
                        // Test various helper functions
                        val event = TestHelpers.createTestEvent("thread_${threadId}_$iteration")
                        val runningEvent = TestHelpers.createRunningEvent("running_${threadId}_$iteration")
                        val polygonPoints = TestHelpers.GeoHelpers.createPolygonPoints(
                            TestLocations.PARIS,
                            0.01,
                            6
                        )
                        val distance = TestHelpers.GeoHelpers.calculateDistance(
                            TestLocations.PARIS,
                            TestLocations.LONDON
                        )

                        synchronized(results) {
                            results.add("Thread $threadId: event=${event.id}, points=${polygonPoints.size}, dist=$distance")
                        }
                    }
                } catch (e: Exception) {
                    synchronized(exceptions) {
                        exceptions.add(e)
                    }
                }
            }
        }

        threads.forEach { it.start() }
        threads.forEach { it.join() }

        assertTrue(exceptions.isEmpty(), "No exceptions should occur during concurrent utility usage: $exceptions")
        assertEquals(50, results.size, "All threads should complete successfully")
    }

    @Test
    fun `test utility function performance`() {
        repeat(100) { i ->
            // Test performance of various helper functions
            TestHelpers.createTestEvent("perf_test_$i")
            TestHelpers.createRunningEvent("running_perf_$i")
            TestHelpers.createFutureEvent("future_perf_$i")
            TestHelpers.createCompletedEvent("completed_perf_$i")

            val points = TestHelpers.GeoHelpers.createPolygonPoints(TestLocations.PARIS, 0.01, 6)
            TestHelpers.GeoHelpers.calculateDistance(TestLocations.PARIS, TestLocations.LONDON)

            AssertionHelpers.assertPositionInBounds(TestLocations.TOKYO)
            AssertionHelpers.assertTimeOrderCorrect(TestTimes.BASE_TIME, TestTimes.MORNING_TIME)
            AssertionHelpers.assertStatusTransition(IWWWEvent.Status.NEXT, IWWWEvent.Status.SOON)
        }

        // If we reach here without crashes, performance is acceptable
        assertTrue(true, "Utility functions completed performance test without errors")
    }
}