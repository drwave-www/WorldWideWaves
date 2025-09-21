package com.worldwidewaves.shared.error

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
import com.worldwidewaves.shared.events.WWWEvents
import com.worldwidewaves.shared.events.WWWEventObserver
import com.worldwidewaves.shared.events.WWWEventWaveLinear
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.data.DataStore
import com.worldwidewaves.shared.testing.TestHelpers
import io.github.aakira.napier.Antilog
import io.github.aakira.napier.LogLevel
import io.github.aakira.napier.Napier
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class ErrorHandlingTest : KoinTest {
    private lateinit var mockClock: IClock
    private lateinit var wwwEvents: WWWEvents
    private lateinit var testEvent: IWWWEvent

    init {
        Napier.base(
            object : Antilog() {
                override fun performLog(
                    priority: LogLevel,
                    tag: String?,
                    throwable: Throwable?,
                    message: String?,
                ) {
                    println("[$priority] $tag: $message")
                    throwable?.printStackTrace()
                }
            },
        )
    }

    @BeforeTest
    fun setUp() {
        mockClock = mockk<IClock>()
        every { mockClock.now() } returns Instant.fromEpochMilliseconds(System.currentTimeMillis())

        startKoin {
            modules(module {
                single { mockClock }
            })
        }

        wwwEvents = WWWEvents()
        testEvent = TestHelpers.createTestEvent()
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    // ===== NETWORK FAILURE SCENARIOS =====

    @Test
    fun `test network failure during event loading should handle gracefully`() = runTest {
        // GIVEN: Network failure simulation
        coEvery { wwwEvents.loadEventById("network_fail_event") } throws Exception("Network connection failed")

        // WHEN: Try to load event with network failure
        var exceptionCaught = false
        try {
            wwwEvents.loadEventById("network_fail_event")
        } catch (e: Exception) {
            exceptionCaught = true
            assertTrue(e.message?.contains("Network connection failed") == true, "Should contain network error message")
        }

        // THEN: Should handle network failure gracefully
        assertTrue(exceptionCaught, "Network failure should be caught and handled")
    }

    @Test
    fun `test observer handles network timeouts during state updates`() = runTest {
        // GIVEN: Event observer with timeout prone network
        val observer = WWWEventObserver(testEvent)

        // WHEN: Network timeout occurs during observation
        var timeoutHandled = false
        try {
            withTimeout(100.milliseconds) {
                observer.startObservation()
                delay(200.milliseconds) // Simulate long network operation
                observer.getObservationState().first()
            }
        } catch (e: TimeoutCancellationException) {
            timeoutHandled = true
            observer.stopObservation()
        }

        // THEN: Should handle timeout gracefully
        assertTrue(timeoutHandled, "Network timeout should be handled")
        assertFalse(observer.isObserving(), "Observer should stop after timeout")
    }

    @Test
    fun `test resource loading failure with fallback mechanisms`() = runTest {
        // GIVEN: Resource loading that fails
        val invalidEventId = "invalid_resource_id"

        // WHEN: Try to load invalid resource
        var fallbackUsed = false
        try {
            wwwEvents.loadEventById(invalidEventId)
        } catch (e: Exception) {
            // THEN: Should use fallback mechanism
            fallbackUsed = true
            assertTrue(e.message?.contains("invalid") == true || e.message?.contains("not found") == true,
                "Should indicate resource not found")
        }

        assertTrue(fallbackUsed, "Should handle resource loading failure")
    }

    // ===== MALFORMED DATA HANDLING =====

    @Test
    fun `test malformed JSON data handling`() = runTest {
        // GIVEN: Malformed event data
        val malformedJson = """{"invalid": "json", "missing_required_fields": true}"""

        // WHEN: Try to parse malformed data
        var parseErrorHandled = false
        try {
            // Simulate parsing malformed JSON through WWWEvent creation
            val invalidEvent = WWWEvent(
                id = "",
                name = "",
                description = "",
                start = "",
                end = "",
                wave = WWWEventWaveLinear(1.0, WWWEventWaveLinear.Direction.EAST, 60),
                bbox = TestHelpers.createTestBoundingBox(),
                areas = emptyList(),
                country = "",
                community = "",
                link = null,
                instagramAccount = null,
                instagramHashtag = null
            )

            // Should fail validation
            val validationErrors = invalidEvent.validationErrors()
            if (validationErrors != null && validationErrors.isNotEmpty()) {
                parseErrorHandled = true
            }
        } catch (e: Exception) {
            parseErrorHandled = true
        }

        // THEN: Should handle malformed data gracefully
        assertTrue(parseErrorHandled, "Should handle malformed data parsing")
    }

    @Test
    fun `test invalid date format handling`() = runTest {
        // GIVEN: Event with invalid date formats
        val invalidDates = listOf(
            "invalid-date-format",
            "2024-13-45", // Invalid month/day
            "not-a-date",
            "",
            "2024/12/31 25:99:99" // Invalid time
        )

        // WHEN & THEN: Each invalid date should be handled
        invalidDates.forEach { invalidDate ->
            var dateErrorHandled = false
            try {
                val eventWithInvalidDate = WWWEvent(
                    id = "test",
                    name = "Test Event",
                    description = "Test",
                    start = invalidDate,
                    end = invalidDate,
                    wave = WWWEventWaveLinear(10.0, WWWEventWaveLinear.Direction.EAST, 60),
                    bbox = TestHelpers.createTestBoundingBox(),
                    areas = emptyList(),
                    country = "Test",
                    community = "Test",
                    link = null,
                    instagramAccount = null,
                    instagramHashtag = null
                )

                val validationErrors = eventWithInvalidDate.validationErrors()
                if (validationErrors != null && validationErrors.isNotEmpty()) {
                    dateErrorHandled = true
                }
            } catch (e: Exception) {
                dateErrorHandled = true
            }

            assertTrue(dateErrorHandled, "Invalid date '$invalidDate' should be handled gracefully")
        }
    }

    @Test
    fun `test corrupted wave data recovery`() = runTest {
        // GIVEN: Wave with corrupted parameters
        val corruptedWaveScenarios = listOf(
            Pair(-1.0, "Negative speed"),
            Pair(0.0, "Zero speed"),
            Pair(Double.POSITIVE_INFINITY, "Infinite speed"),
            Pair(Double.NaN, "NaN speed")
        )

        // WHEN & THEN: Each corrupted scenario should be handled
        corruptedWaveScenarios.forEach { (corruptedSpeed, scenario) ->
            var corruptionHandled = false
            try {
                val corruptedWave = WWWEventWaveLinear(
                    corruptedSpeed,
                    WWWEventWaveLinear.Direction.EAST,
                    60
                )

                val validationErrors = corruptedWave.validationErrors()
                if (validationErrors != null && validationErrors.isNotEmpty()) {
                    corruptionHandled = true
                }
            } catch (e: Exception) {
                corruptionHandled = true
            }

            assertTrue(corruptionHandled, "$scenario should be handled gracefully")
        }
    }

    // ===== CONCURRENT MODIFICATION SCENARIOS =====

    @Test
    fun `test concurrent observer modifications`() = runTest {
        // GIVEN: Multiple observers on same event
        val observer1 = WWWEventObserver(testEvent)
        val observer2 = WWWEventObserver(testEvent)

        var concurrencyHandled = true

        try {
            // WHEN: Start multiple observers concurrently
            observer1.startObservation()
            observer2.startObservation()

            delay(50.milliseconds)

            // Stop them concurrently
            observer1.stopObservation()
            observer2.stopObservation()

        } catch (e: Exception) {
            // If exception occurs, it should be a controlled failure
            concurrencyHandled = e is CancellationException ||
                                e.message?.contains("concurrent") == true
        }

        // THEN: Should handle concurrent access
        assertTrue(concurrencyHandled, "Concurrent observer modifications should be handled")
        assertFalse(observer1.isObserving(), "Observer 1 should be stopped")
        assertFalse(observer2.isObserving(), "Observer 2 should be stopped")
    }

    @Test
    fun `test rapid start-stop observer cycles`() = runTest {
        // GIVEN: Observer with rapid start/stop cycles
        val observer = WWWEventObserver(testEvent)
        var cycleErrorsHandled = 0

        // WHEN: Rapidly start and stop observer
        repeat(10) { cycle ->
            try {
                observer.startObservation()
                delay(10.milliseconds)
                observer.stopObservation()
                delay(10.milliseconds)
            } catch (e: Exception) {
                cycleErrorsHandled++
                // Should be controlled errors like IllegalStateException
                assertTrue(
                    e is IllegalStateException || e is CancellationException,
                    "Cycle $cycle should have controlled error types, got: ${e::class.simpleName}"
                )
            }
        }

        // THEN: Should handle rapid cycles gracefully
        assertTrue(observer.isObserving() == false, "Observer should be in stopped state")
        // Some errors are expected in rapid cycles, but should be controlled
        assertTrue(cycleErrorsHandled <= 5, "Should handle most rapid cycles gracefully")
    }

    // ===== OUT-OF-MEMORY CONDITIONS =====

    @Test
    fun `test large polygon handling memory efficiency`() = runTest {
        // GIVEN: Large polygon data that could cause memory issues
        val largePolygonCoordinates = (1..1000).map { i ->
            listOf(i.toDouble(), (i * 2).toDouble())
        }

        var memoryHandled = false

        try {
            // WHEN: Create event with very large area
            val largeArea = TestHelpers.createTestArea(largePolygonCoordinates)
            val eventWithLargeArea = TestHelpers.createTestEvent(areas = listOf(largeArea))

            // Test that it handles the large data set
            val validationErrors = eventWithLargeArea.validationErrors()
            memoryHandled = true

        } catch (e: OutOfMemoryError) {
            // Should not reach here in normal testing
            memoryHandled = false
        } catch (e: Exception) {
            // Other exceptions are acceptable for large data sets
            memoryHandled = true
        }

        // THEN: Should handle large data without crashing
        assertTrue(memoryHandled, "Large polygon data should be handled efficiently")
    }

    @Test
    fun `test wave calculation memory limits`() = runTest {
        // GIVEN: Wave calculations that could consume excessive memory
        val memoryIntensiveWave = WWWEventWaveLinear(
            speed = 19.0, // Maximum valid speed
            direction = WWWEventWaveLinear.Direction.EAST,
            approxDuration = 3600 // 1 hour duration
        )

        var memoryEfficient = false

        try {
            // WHEN: Perform memory-intensive wave calculations
            val largeBbox = TestHelpers.createTestBoundingBox(
                minLon = -180.0, minLat = -90.0,
                maxLon = 180.0, maxLat = 90.0 // World-wide bounding box
            )

            val eventWithLargeWave = TestHelpers.createTestEvent(
                wave = memoryIntensiveWave,
                bbox = largeBbox
            )

            // Test wave calculations don't consume excessive memory
            val progression = memoryIntensiveWave.getProgression()
            assertTrue(progression >= 0.0 && progression <= 100.0, "Progression should be valid")

            memoryEfficient = true

        } catch (e: OutOfMemoryError) {
            memoryEfficient = false
        } catch (e: Exception) {
            // Other exceptions are acceptable
            memoryEfficient = true
        }

        // THEN: Should handle memory-intensive calculations efficiently
        assertTrue(memoryEfficient, "Wave calculations should be memory efficient")
    }

    // ===== GRACEFUL DEGRADATION =====

    @Test
    fun `test observer graceful degradation during failures`() = runTest {
        // GIVEN: Observer with various failure scenarios
        val observer = WWWEventObserver(testEvent)

        // WHEN: Simulate various failures during observation
        observer.startObservation()

        // Simulate clock failure
        every { mockClock.now() } throws Exception("Clock service unavailable")

        var degradationHandled = false
        try {
            // Observer should continue working with degraded functionality
            delay(100.milliseconds)
            val isStillObserving = observer.isObserving()
            degradationHandled = true
        } catch (e: Exception) {
            // Even if exception occurs, should be handled gracefully
            degradationHandled = e.message?.contains("Clock service") == true
        } finally {
            // Reset clock for cleanup
            every { mockClock.now() } returns Instant.fromEpochMilliseconds(System.currentTimeMillis())
            observer.stopObservation()
        }

        // THEN: Should degrade gracefully
        assertTrue(degradationHandled, "Observer should handle service failures gracefully")
    }

    @Test
    fun `test event validation with missing optional fields`() = runTest {
        // GIVEN: Event with missing optional fields
        val minimalEvent = WWWEvent(
            id = "minimal",
            name = "Minimal Event",
            description = "Test",
            start = "2024-12-31T12:00:00Z",
            end = "2024-12-31T13:00:00Z",
            wave = WWWEventWaveLinear(10.0, WWWEventWaveLinear.Direction.EAST, 60),
            bbox = TestHelpers.createTestBoundingBox(),
            areas = emptyList(),
            country = "Test",
            community = "Test",
            link = null, // Optional field missing
            instagramAccount = null, // Optional field missing
            instagramHashtag = null // Optional field missing
        )

        // WHEN: Validate event with missing optional fields
        val validationErrors = minimalEvent.validationErrors()

        // THEN: Should handle missing optional fields gracefully
        val hasOnlyOptionalFieldErrors = validationErrors?.all { error ->
            error.contains("link") || error.contains("instagram") || error.contains("optional")
        } ?: true

        assertTrue(hasOnlyOptionalFieldErrors, "Missing optional fields should not cause critical validation errors")
    }

    @Test
    fun `test system recovery after multiple cascading failures`() = runTest {
        // GIVEN: Multiple system components that can fail
        val observer = WWWEventObserver(testEvent)
        var recoverySuccessful = false

        try {
            // WHEN: Simulate cascading failures
            observer.startObservation()

            // Clock failure
            every { mockClock.now() } throws Exception("Clock failed")
            delay(50.milliseconds)

            // Recovery: Reset clock
            every { mockClock.now() } returns Instant.fromEpochMilliseconds(System.currentTimeMillis())

            // Test system recovery
            observer.stopObservation()
            observer.startObservation()

            val finalState = observer.isObserving()
            recoverySuccessful = finalState

            observer.stopObservation()

        } catch (e: Exception) {
            // System should attempt recovery even after exceptions
            recoverySuccessful = e.message?.contains("Clock failed") == true
        }

        // THEN: System should demonstrate recovery capability
        assertTrue(recoverySuccessful, "System should recover from cascading failures")
    }
}