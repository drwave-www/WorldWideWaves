package com.worldwidewaves.shared.events

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

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class WWWEventWaveTest {

    private val testEvent = createRandomWWWEvent("test_event")

    @Test
    fun testGetLiteralStartTime() = runTest {
        val wave = WWWEventWave(testEvent)
        assertEquals("10:00", wave.getLiteralStartTime())
    }

    @Test
    fun testGetLiteralSpeed() = runTest {
        val wave = WWWEventWave(testEvent)
        assertEquals("10 m/s", wave.getLiteralSpeed())
    }

    @Test
    fun testGetLiteralEndTime() = runTest {
        val wave = WWWEventWave(testEvent)
        // Mocking getBoundingBox is necessary for this test
        val mockEventArea = object : WWWEventArea(testEvent, DefaultGeoJsonDataProvider()) {
            override suspend fun getBoundingBox(): BoundingBox {
                return BoundingBox(
                    minLatitude = 48.85,
                    minLongitude = 2.35,
                    maxLatitude = 48.86,
                    maxLongitude = 2.36
                )
            }}
        testEvent.area = mockEventArea
        val expectedEndTime = "10:07" // Approximate, based on mock bounding box
        val actualEndTime = wave.getLiteralEndTime()
        // Using assertEquals with a tolerance for time due to potential rounding differences
        val timeDifferenceInSeconds = abs(
            LocalDateTime.parse("2024-05-10T$actualEndTime").toInstant(TimeZone.UTC).epochSeconds -
                    LocalDateTime.parse("2024-05-10T$expectedEndTime").toInstant(TimeZone.UTC).epochSeconds
        )
        assertTrue(timeDifferenceInSeconds <= 60, "End time is within expected range")
    }

    @Test
    fun testGetLiteralTotalTime() = runTest {
        val wave = WWWEventWave(testEvent)
        // Mocking getBoundingBox is necessary for this test
        val mockEventArea = object : WWWEventArea(testEvent, DefaultGeoJsonDataProvider()) {
            override suspend fun getBoundingBox(): BoundingBox {
                return BoundingBox(
                    minLatitude = 48.85,
                    minLongitude = 2.35,
                    maxLatitude = 48.86,
                    maxLongitude = 2.36
                )
            }
        }
        testEvent.area = mockEventArea
        val expectedTotalTime = "7 min" // Approximate, based on mock bounding box
        assertEquals(expectedTotalTime, wave.getLiteralTotalTime())
    }

    @Test
    fun testGetLiteralProgressionNotStarted() = runTest {
        val wave = WWWEventWave(testEvent)
        assertEquals("0%", wave.getLiteralProgression())
    }

    @Test
    fun testGetLiteralProgressionDone() = runTest {
        val doneEvent = testEvent.copy(id = "paris_france") // Assuming "paris_france" is a done event
        val wave = WWWEventWave(doneEvent)
        assertEquals("100%", wave.getLiteralProgression())
    }

    @Test
    fun testGetLiteralProgressionRunning() = runTest {
        val runningEvent = testEvent.copy(id = "riodejaneiro_brazil")
        val wave = WWWEventWave(runningEvent)

        // Mocking getLocalDatetime and getTotalTime for this test
        val mockGetLocalDatetime = {
            Clock.System.now().plus(300,DateTimeUnit.SECOND).toLocalDateTime(TimeZone.currentSystemDefault())
        }
        val mockGetTotalTime = { 600.seconds }

        val expectedProgression = "50%"
        val actualProgression = wave.getLiteralProgression(mockGetLocalDatetime, mockGetTotalTime)
        assertEquals(expectedProgression, actualProgression)
    }

    // Helper function to inject mocked functions for testing getLiteralProgression
    private suspend fun WWWEventWave.getLiteralProgression(
        getLocalDatetime: () -> LocalDateTime,
        getTotalTime: suspend () -> Duration
    ): String {
        // Use the provided mocked functions directly
        val elapsedTime = getLocalDatetime().toInstant(event.getTimeZone()).epochSeconds -
                event.getStartDateTimeAsLocal().toInstant(event.getTimeZone()).epochSeconds
        val totalTime = getTotalTime().inWholeSeconds
        val progression = (elapsedTime.toDouble() / totalTime) * 100
        return "$progression%"
    }
}