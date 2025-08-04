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

import com.worldwidewaves.shared.events.utils.IClock
import io.github.aakira.napier.Antilog
import io.github.aakira.napier.LogLevel
import io.github.aakira.napier.Napier
import io.mockk.mockk
import kotlin.time.Instant
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class WWWEventWaveTest : KoinTest {

    private var mockClock = mockk<IClock>()
    private var mockEvent = mockk<IWWWEvent>(relaxed = true)
    private lateinit var wave: WWWEventWave

    // ---------------------------

    init {
        Napier.base(object : Antilog() {
            override fun performLog(priority: LogLevel, tag: String?, throwable: Throwable?, message: String?) {
                println(message)
            }
        })
    }

    @BeforeTest
    fun setUp() {

        startKoin { modules(module { single { mockClock } }) }

        wave = object : WWWEventWave() {
            override val speed: Double = 10.0
            override val direction: Direction = Direction.EAST
            override val approxDuration: Int = 60
            override suspend fun getWavePolygons(lastWaveState: WavePolygons?, mode: WaveMode): WavePolygons = WavePolygons(
                clock.now(), emptyList(), emptyList()
            )
            override suspend fun getWaveDuration(): Duration = Duration.ZERO
            override suspend fun hasUserBeenHitInCurrentPosition(): Boolean = false
            override suspend fun userHitDateTime(): Instant? = null
            override suspend fun closestWaveLongitude(latitude: Double): Double = 0.0
            override suspend fun userPositionToWaveRatio() = 0.0
        }.setRelatedEvent(mockEvent)
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    // ---------------------------

}