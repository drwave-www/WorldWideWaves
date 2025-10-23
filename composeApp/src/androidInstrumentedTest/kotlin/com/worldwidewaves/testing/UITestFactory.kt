package com.worldwidewaves.testing

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
import com.worldwidewaves.shared.viewmodels.EventsViewModel
import io.mockk.every
import io.mockk.mockk
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Factory for creating mock test data for WorldWideWaves UI testing.
 * Provides consistent, configurable mock objects for various testing scenarios.
 */
object UITestFactory {
    /**
     * Create a mock wave event for testing wave participation scenarios.
     */
    fun createMockWaveEvent(
        id: String = "test-event-123",
        name: String = "Test Wave Event",
        status: String = "RUNNING",
        isInArea: Boolean = true,
        isWarmingInProgress: Boolean = false,
        isGoingToBeHit: Boolean = false,
        hasBeenHit: Boolean = false,
        progression: Double = 50.0,
        userPositionRatio: Double = 0.5,
        timeBeforeHit: Duration = 5.minutes,
        isFavorite: Boolean = false,
        isDownloaded: Boolean = true,
    ): IWWWEvent =
        mockk<IWWWEvent>(relaxed = true) {
            every { id } returns id
            every { name } returns name
            every { status } returns status
            every { isInArea } returns isInArea
            every { isWarmingInProgress } returns isWarmingInProgress
            every { isGoingToBeHit } returns isGoingToBeHit
            every { hasBeenHit } returns hasBeenHit
            every { progression } returns progression
            every { userPositionRatio } returns userPositionRatio
            every { timeBeforeHit } returns timeBeforeHit
            every { isFavorite } returns isFavorite
            every { isDownloaded } returns isDownloaded
        }

    /**
     * Create mock events list for testing events screen scenarios.
     */
    fun createMockEventsList(
        count: Int = 3,
        withDifferentStates: Boolean = true,
    ): List<IWWWEvent> =
        if (withDifferentStates) {
            listOf(
                createMockEvent("event1", "New York", isFavorite = true, isDownloaded = true),
                createMockEvent("event2", "London", isFavorite = false, isDownloaded = true),
                createMockEvent("event3", "Tokyo", isFavorite = true, isDownloaded = false),
                createMockEvent("event4", "Paris", isFavorite = false, isDownloaded = false),
            ).take(count)
        } else {
            (1..count).map { index ->
                createMockEvent("event$index", "Test City $index")
            }
        }

    /**
     * Create a mock event with specific properties for easier testing.
     */
    fun createMockEvent(
        id: String,
        cityName: String,
        isFavorite: Boolean = false,
        isDownloaded: Boolean = true,
        status: String = "UPCOMING",
    ): IWWWEvent =
        createMockWaveEvent(
            id = id,
            name = "$cityName Wave Event",
            status = status,
            isFavorite = isFavorite,
            isDownloaded = isDownloaded,
        )

    /**
     * Create test data for timer functionality with various durations.
     */
    fun createTimerTestData(): List<Duration> =
        listOf(
            0.seconds,
            30.seconds,
            1.minutes,
            5.minutes + 30.seconds,
            15.minutes,
            1.hours,
            2.hours + 30.minutes,
            24.hours,
        )

    /**
     * Create wave phase scenarios for testing phase transitions.
     */
    fun createWavePhaseScenarios(): List<Map<String, Any>> =
        listOf(
            mapOf(
                "phase" to "OBSERVER",
                "isInArea" to false,
                "isWarmingInProgress" to false,
                "isGoingToBeHit" to false,
                "hasBeenHit" to false,
            ),
            mapOf(
                "phase" to "WARMING",
                "isInArea" to true,
                "isWarmingInProgress" to true,
                "isGoingToBeHit" to false,
                "hasBeenHit" to false,
            ),
            mapOf(
                "phase" to "WAITING",
                "isInArea" to true,
                "isWarmingInProgress" to false,
                "isGoingToBeHit" to true,
                "hasBeenHit" to false,
            ),
            mapOf(
                "phase" to "HIT",
                "isInArea" to true,
                "isWarmingInProgress" to false,
                "isGoingToBeHit" to false,
                "hasBeenHit" to true,
            ),
            mapOf(
                "phase" to "COMPLETED",
                "isInArea" to true,
                "isWarmingInProgress" to false,
                "isGoingToBeHit" to false,
                "hasBeenHit" to true,
            ),
        )

    /**
     * Create progression test data for testing wave progression visualization.
     */
    fun createProgressionTestData(): List<Double> = listOf(0.0, 10.0, 25.0, 50.0, 75.0, 90.0, 100.0)

    /**
     * Create participation feedback scenarios for testing user status display.
     */
    fun createParticipationScenarios(): List<Map<String, Any>> =
        listOf(
            mapOf(
                "scenario" to "successful_participation",
                "hasBeenHit" to true,
                "isInArea" to true,
                "timingAccuracy" to 50L, // milliseconds off target
            ),
            mapOf(
                "scenario" to "missed_wave",
                "hasBeenHit" to false,
                "isInArea" to true,
                "timingAccuracy" to 0L,
            ),
            mapOf(
                "scenario" to "outside_area",
                "hasBeenHit" to false,
                "isInArea" to false,
                "timingAccuracy" to 0L,
            ),
            mapOf(
                "scenario" to "perfect_timing",
                "hasBeenHit" to true,
                "isInArea" to true,
                "timingAccuracy" to 0L, // Perfect timing
            ),
        )

    /**
     * Create mock EventsViewModel for testing events list functionality.
     */
    fun createMockEventsViewModel(events: List<IWWWEvent> = createMockEventsList()): EventsViewModel {
        val mockViewModel = mockk<EventsViewModel>(relaxed = true)

        every { mockViewModel.events } returns kotlinx.coroutines.flow.MutableStateFlow(events)

        return mockViewModel
    }

    /**
     * Create map states for testing map download functionality.
     */
    fun createMockMapStates(
        eventIds: List<String> = listOf("event1", "event2", "event3"),
        defaultDownloaded: Boolean = true,
    ): Map<String, Boolean> = eventIds.associateWith { defaultDownloaded }

    /**
     * Create error scenarios for testing error handling.
     */
    fun createErrorScenarios(): List<Map<String, Any>> =
        listOf(
            mapOf(
                "type" to "network_error",
                "progression" to Double.NaN,
                "userPositionRatio" to 0.5,
                "timeBeforeHit" to 5.minutes,
            ),
            mapOf(
                "type" to "gps_error",
                "progression" to 50.0,
                "userPositionRatio" to -1.0, // Invalid position
                "timeBeforeHit" to 5.minutes,
            ),
            mapOf(
                "type" to "time_sync_error",
                "progression" to 50.0,
                "userPositionRatio" to 0.5,
                "timeBeforeHit" to (-5).seconds, // Negative time
            ),
            mapOf(
                "type" to "data_corruption",
                "progression" to Double.POSITIVE_INFINITY,
                "userPositionRatio" to Double.NaN,
                "timeBeforeHit" to Duration.INFINITE,
            ),
        )

    /**
     * Create geographic coordinate test data.
     */
    fun createCoordinateTestData(): List<Pair<Double, Double>> =
        listOf(
            // Valid coordinates
            Pair(40.7128, -74.0060), // New York
            Pair(51.5074, -0.1278), // London
            Pair(35.6762, 139.6503), // Tokyo
            Pair(48.8566, 2.3522), // Paris
            // Edge cases
            Pair(90.0, 180.0), // North pole, International date line
            Pair(-90.0, -180.0), // South pole, opposite date line
            Pair(0.0, 0.0), // Equator, Prime meridian
            // Invalid coordinates (for testing validation)
            Pair(91.0, 0.0), // Invalid latitude
            Pair(0.0, 181.0), // Invalid longitude
            Pair(Double.NaN, 0.0), // NaN coordinates
            Pair(0.0, Double.POSITIVE_INFINITY), // Infinite coordinates
        )

    /**
     * Create network response time test data.
     */
    fun createNetworkTestData(): List<Long> =
        listOf(
            50L, // Excellent
            200L, // Good
            500L, // Acceptable
            1000L, // Slow
            3000L, // Very slow
            5000L, // Timeout threshold
            10000L, // Should timeout
            30000L, // Definitely timeout
        )

    /**
     * Create user-friendly error message test data.
     */
    fun createErrorMessageTestData(): List<String> =
        listOf(
            "Connection lost. Please check your internet.",
            "Location unavailable. Enable GPS to participate.",
            "Wave has ended. View results in the app.",
            "", // Empty message (should fail validation)
            "This is a very long error message that exceeds the recommended character limit for good user experience and should be flagged as too verbose for mobile screens where space is limited and users prefer concise, actionable messages that don't overwhelm them with technical details they don't need to understand or act upon immediately.", // Too long
            "GPS_ERROR_CODE_1001_SATELLITE_ACQUISITION_FAILED", // Technical jargon
            "Network timeout occurred while synchronizing wave data. Please ensure you have a stable internet connection and try again. If the problem persists, contact support.", // Good balance
        )

    /**
     * Format duration for testing display purposes.
     */
    fun formatDurationForTest(duration: Duration): String =
        when {
            duration < 1.hours -> {
                val minutes = duration.inWholeMinutes.toString().padStart(2, '0')
                val seconds = (duration.inWholeSeconds % 60).toString().padStart(2, '0')
                "$minutes:$seconds"
            }
            else -> {
                val hours = duration.inWholeHours.toString().padStart(2, '0')
                val minutes = (duration.inWholeMinutes % 60).toString().padStart(2, '0')
                "$hours:$minutes"
            }
        }
}
