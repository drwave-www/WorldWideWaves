package com.worldwidewaves.shared.events

/*
 * Copyright 2024 DrWave
 *
 * WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and
 * countries, culminating in a global wave. The project aims to transcend physical and cultural
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

import com.worldwidewaves.shared.events.WWWEventWaveWarming.Type.METERS
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.events.utils.Polygon
import io.github.aakira.napier.Antilog
import io.github.aakira.napier.LogLevel
import io.github.aakira.napier.Napier
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.datetime.Instant
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
    private var event = mockk<IWWWEvent>(relaxed = true)
    private lateinit var wave: WWWEventWave

    // ---------------------------

    @BeforeTest
    fun setUp() {

        startKoin { modules(
            module {
                single { clock }
            }
        )}

        Napier.base(object : Antilog() {
            override fun performLog(priority: LogLevel, tag: String?, throwable: Throwable?, message: String?) {
                println(message)
            }
        })

        wave = object : WWWEventWave() {
            override val speed: Double = 10.0
            override val direction: Direction = Direction.EAST
            override val warming: WWWEventWaveWarming = WWWEventWaveWarming(type = METERS)
            override suspend fun getWarmingPolygons(): List<Polygon> = emptyList()
            override suspend fun getWavePolygons(): List<Polygon> = emptyList()
            override suspend fun getWaveDuration(): Duration = Duration.ZERO
            override suspend fun hasUserBeenHitInCurrentPosition(): Boolean = false
            override suspend fun timeBeforeHit(): Duration = Duration.INFINITE
        }.setRelatedEvent(event)
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    // ---------------------------

    @Test
    fun testIsNearTheEvent() {

        // GIVEN --------------------------------
        val now = Instant.parse("2023-12-31T13:15:00+02:00") // Close from the event
        val eventStartTime = Instant.parse("2024-01-01T01:00:00+12:45")

        every { clock.now() } returns now
        every { event.getStartDateTime() } returns eventStartTime

        // WHEN ---------------------------------
        val result = wave.isNearTheEvent()

        // THEN ---------------------------------
        assertTrue(result)

        verify { clock.now() }
        verify { event.getStartDateTime() }
    }

    @Test
    fun testIsNearTheEvent_Fails() {

        // GIVEN --------------------------------
        val now = Instant.parse("2023-01-01T00:00:00+01:00") // Far from the event
        val eventStartTime = Instant.parse("2024-01-01T01:00:00+12:45")

        every { clock.now() } returns now
        every { event.getStartDateTime() } returns eventStartTime

        // WHEN ---------------------------------
        val result = wave.isNearTheEvent()

        // THEN ---------------------------------
        assertFalse(result) // Assert that it's NOT near the event

        verify { clock.now() }
        verify { event.getStartDateTime() }
    }

}