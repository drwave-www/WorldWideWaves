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

import com.worldwidewaves.shared.debugBuild
import com.worldwidewaves.shared.events.utils.IClock
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration

class WWWEventWaveTest : KoinTest {

    private var clock = mockk<IClock>()
    private var event = mockk<WWWEvent>()
    private lateinit var wave: WWWEventWave

    // ---------------------------

    @BeforeTest
    fun setUp() {

        debugBuild()

        startKoin { modules(
            module {
                single { clock }
            }
        )}

        wave = object : WWWEventWave() {
            override val speed: Double = 0.0
            override val direction: String = "N"
            override suspend fun getObservationInterval(): Long = 0L
            override suspend fun getEndTime(): LocalDateTime = LocalDateTime(2024, 1, 1, 0, 0)
            override suspend fun getTotalTime(): Duration = Duration.ZERO
            override suspend fun getProgression(): Double = 0.0
            override suspend fun isWarmingEnded(): Boolean = false
            override suspend fun hasUserBeenHit(): Boolean = false
        }.setEvent(event)
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    // ---------------------------

    @Test
    fun testIsNearTheEvent() {

        // GIVEN --------------------------------
        val eventTimeZone = TimeZone.of("Pacific/Chatham") // Exotic timezone
        val now = Instant.parse("2023-12-31T13:15:00+02:00") // Close from the event
        val eventStartTime = Instant.parse("2024-01-01T01:00:00+12:45")

        every { clock.now() } returns now
        every { event.getTZ() } answers { eventTimeZone }
        every { event.getStartDateTime() } returns eventStartTime.toLocalDateTime(eventTimeZone)

        // WHEN ---------------------------------
        val result = wave.isNearTheEvent()

        // THEN ---------------------------------
        assertTrue(result)

        verify { clock.now() }
        verify { event.getTZ() }
        verify { event.getStartDateTime() }

        confirmVerified(clock, event)
    }

    @Test
    fun testIsNearTheEvent_Fails() {

        // GIVEN --------------------------------
        val eventTimeZone = TimeZone.of("Pacific/Chatham") // Exotic timezone
        val now = Instant.parse("2023-01-01T00:00:00+01:00") // Far from the event
        val eventStartTime = Instant.parse("2024-01-01T01:00:00+12:45")

        every { clock.now() } returns now
        every { event.getTZ() } answers { eventTimeZone }
        every { event.getStartDateTime() } returns eventStartTime.toLocalDateTime(eventTimeZone)

        // WHEN ---------------------------------
        val result = wave.isNearTheEvent()

        // THEN ---------------------------------
        assertFalse(result) // Assert that it's NOT near the event

        verify { clock.now() }
        verify { event.getTZ() }
        verify { event.getStartDateTime() }

        confirmVerified(clock)
        confirmVerified(event)
    }

}