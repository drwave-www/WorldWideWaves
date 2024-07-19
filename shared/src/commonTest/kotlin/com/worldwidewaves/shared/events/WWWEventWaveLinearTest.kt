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

import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class WWWEventWaveLinearTest {

    private val testEvent = WWWEvent(
        id = "paris_france",
        type = "city",
        location = "Paris",
        country = "france",
        community = "europe",
        date = "2024-03-15",
        startHour = "18:00",
        speed = 5,
        description = "Experience the wave in Paris",
        instagramAccount = "worldwidewaves.paris",
        instagramHashtag = "#waveparis",
        mapOsmadminid = 71525,
        mapMaxzoom = 14.0,
        mapLanguage = "fr",
        mapOsmarea = "europe/france/ile-de-france",
        mapWarmingZoneLongitude = 2.3417,
        timeZone = "Europe/Paris"
    )

    private val wave = WWWEventWaveLinear(testEvent)

    @Test
    fun testGetLiteralSpeed() {
        assertEquals("5 m/s", wave.getLiteralSpeed())
    }

    @Test
    fun testGetLiteralStartTime() {
        assertEquals("18:00", wave.getLiteralStartTime())
    }

    @Test
    fun testGetLiteralTimezone() {
        val currentMonth = Clock.System.now().toLocalDateTime(TimeZone.of("Europe/Paris")).month
        val expectedOffset = if (currentMonth in listOf(Month.MARCH, Month.APRIL, Month.MAY, Month.JUNE, Month.JULY, Month.AUGUST, Month.SEPTEMBER, Month.OCTOBER)) "UTC+2" else "UTC+1"
        assertEquals(expectedOffset, wave.getLiteralTimezone())
    }

    @Test
    fun testGetLiteralEndTime() = runTest {
        val expectedEndTime = "18:59" // Adjust based on your event duration calculation
        assertEquals(expectedEndTime, wave.getLiteralEndTime())
    }

    @Test
    fun testGetLiteralTotalTime() = runTest {
        val expectedTotalTime = "59 min" // Adjust based on your event duration calculation
        assertEquals(expectedTotalTime, wave.getLiteralTotalTime())
    }

    @Test
    fun testGetLiteralProgression() = runTest {
        val expectedProgression = "100%" // Adjust based on your event progression calculation
        assertEquals(expectedProgression, wave.getLiteralProgression())
    }

    @Test
    fun testGetAllNumbers() = runTest {
        val currentMonth = Clock.System.now().toLocalDateTime(TimeZone.of("Europe/Paris")).month
        val expectedOffset = if (currentMonth in listOf(Month.MARCH, Month.APRIL, Month.MAY, Month.JUNE, Month.JULY, Month.AUGUST, Month.SEPTEMBER, Month.OCTOBER)) "UTC+2" else "UTC+1"
        val numbers = wave.getAllNumbers()
        assertEquals(expectedOffset, numbers.waveTimezone)
        assertEquals("5 m/s", numbers.waveSpeed)
        assertEquals("18:00", numbers.waveStartTime)
        assertEquals("18:59", numbers.waveEndTime)
        assertEquals("59 min", numbers.waveTotalTime)
        assertEquals("100%", numbers.waveProgression)
    }

}