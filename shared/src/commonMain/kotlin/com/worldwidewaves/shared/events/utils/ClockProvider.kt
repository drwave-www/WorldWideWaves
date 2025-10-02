package com.worldwidewaves.shared.events.utils

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

import com.worldwidewaves.shared.WWWPlatform
import com.worldwidewaves.shared.format.DateTimeFormats
import io.github.aakira.napier.Napier
import kotlinx.datetime.TimeZone
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
interface IClock {
    fun now(): Instant

    suspend fun delay(duration: Duration)

    companion object {
        fun instantToLiteral(
            instant: Instant,
            timeZone: TimeZone,
        ): String {
            // Delegate to shared, locale-aware formatter so both Android & iOS
            // use the same logic (12/24 h handled per platform conventions).
            return DateTimeFormats.timeShort(instant, timeZone)
        }
    }
}

@OptIn(ExperimentalTime::class)
class SystemClock :
    IClock,
    KoinComponent {
    private var platform: WWWPlatform? = null

    // iOS FIX: Removed init{} block that calls DI get() to prevent potential deadlocks
    // Platform is now resolved lazily on first access

    private fun getPlatformSafely(): WWWPlatform? {
        if (platform == null) {
            try {
                platform = get()
            } catch (_: Exception) {
                Napier.w("${SystemClock::class.simpleName}: Platform not found, simulation disabled")
            }
        }
        return platform
    }

    override fun now(): Instant =
        if (getPlatformSafely()?.isOnSimulation() == true) {
            platform!!.getSimulation()!!.now()
        } else {
            Clock.System.now()
        }

    override suspend fun delay(duration: Duration) {
        val simulation = getPlatformSafely()?.takeIf { it.isOnSimulation() }?.getSimulation()

        if (simulation != null) {
            val speed =
                simulation.speed.takeIf { it > 0.0 } ?: run {
                    Napier.w("${SystemClock::class.simpleName}: Simulation speed is ${simulation.speed}, using 1.0 instead")
                    1.0
                }
            val adjustedDuration = maxOf(duration / speed.toDouble(), 50.milliseconds) // Minimum 50 ms
            kotlinx.coroutines.delay(adjustedDuration)
        } else {
            kotlinx.coroutines.delay(duration)
        }
    }
}
