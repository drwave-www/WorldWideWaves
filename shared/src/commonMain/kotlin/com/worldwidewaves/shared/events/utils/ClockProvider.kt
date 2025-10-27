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

/**
 * Abstraction for time and delay operations, enabling simulation and testing.
 *
 * ## Purpose
 * Provides a testable abstraction over system time and delays:
 * - **Production**: Uses real system time ([Clock.System.now])
 * - **Simulation**: Uses virtual time that can be accelerated or controlled
 * - **Testing**: Allows deterministic time control for unit tests
 *
 * ## Simulation Mode
 * When [WWWPlatform.isOnSimulation] returns true, the clock behavior changes:
 * - [now] returns simulated time instead of system time
 * - [delay] is scaled by simulation speed (e.g., 10x speed = 10x faster delays)
 * - Minimum delay enforced (50ms) to prevent tight loops
 *
 * This enables testing time-sensitive features like wave progression without real-time delays.
 *
 * ## Usage Patterns
 * ```kotlin
 * class EventScheduler(
 *     private val clock: IClock
 * ) {
 *     suspend fun waitUntilEventStarts(eventTime: Instant) {
 *         val currentTime = clock.now()
 *         if (eventTime > currentTime) {
 *             clock.delay(eventTime - currentTime)
 *         }
 *     }
 *
 *     fun isEventActive(startTime: Instant, endTime: Instant): Boolean {
 *         val now = clock.now()
 *         return now in startTime..endTime
 *     }
 * }
 * ```
 *
 * ## Testing with Simulated Time
 * ```kotlin
 * @Test
 * fun testEventScheduling() = runTest {
 *     // Configure platform with simulation
 *     val mockPlatform = MockPlatform(simulationEnabled = true)
 *     val clock = SystemClock() // Will use simulation time
 *
 *     val startTime = clock.now() + 10.minutes
 *     scheduler.scheduleEvent(startTime)
 *
 *     // Advance simulated time
 *     mockPlatform.advanceTime(10.minutes)
 *
 *     // Event should now be active
 *     assertTrue(scheduler.isEventActive())
 * }
 * ```
 *
 * ## Platform-Aware Formatting
 * The companion object provides [instantToLiteral] for platform-aware time formatting:
 * - Respects platform locale (12/24-hour format)
 * - Uses [DateTimeFormats] for consistent rendering across Android and iOS
 *
 * @see SystemClock for production implementation
 * @see WWWPlatform for simulation configuration
 * @see DateTimeFormats for time formatting
 */
@OptIn(ExperimentalTime::class)
interface IClock {
    /**
     * Returns the current instant.
     *
     * - Production: System time from [Clock.System.now]
     * - Simulation: Virtual time from platform simulation
     *
     * @return Current instant (real or simulated)
     */
    fun now(): Instant

    /**
     * Suspends for the specified duration.
     *
     * - Production: Real delay using [kotlinx.coroutines.delay]
     * - Simulation: Scaled delay based on simulation speed
     *   - Speed 10x: 1 second delay takes 100ms real time
     *   - Minimum 50ms enforced to prevent tight loops
     *
     * @param duration How long to delay (adjusted by simulation speed if active)
     */
    suspend fun delay(duration: Duration)

    companion object {
        /**
         * Formats an instant as a human-readable time string.
         *
         * Uses platform-specific conventions:
         * - Android: Respects system 12/24-hour setting
         * - iOS: Respects system 12/24-hour setting
         *
         * Example output: "2:30 PM" or "14:30"
         *
         * @param instant The instant to format
         * @param timeZone The time zone for formatting
         * @return Formatted time string in platform's locale format
         */
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

/**
 * Production implementation of [IClock] with simulation support.
 *
 * ## iOS Safety
 * **CRITICAL**: This class uses lazy initialization of [WWWPlatform] to prevent iOS deadlocks.
 * - ❌ WRONG: `init { platform = get() }` - Causes iOS deadlock
 * - ✅ CORRECT: Lazy resolution via [getPlatformSafely] on first use
 *
 * ## Behavior Modes
 * 1. **Normal mode** (platform not available or simulation off):
 *    - [now] returns [Clock.System.now]
 *    - [delay] performs real [kotlinx.coroutines.delay]
 *
 * 2. **Simulation mode** (platform available and [WWWPlatform.isOnSimulation] is true):
 *    - [now] returns simulation time from [WWWPlatform.getSimulation]
 *    - [delay] scales duration by simulation speed with 50ms minimum
 *
 * ## Simulation Speed Scaling
 * If simulation speed is 10.0:
 * - Requested delay: 1000ms
 * - Actual delay: 100ms (1000ms / 10.0)
 * - Minimum enforced: max(100ms, 50ms) = 100ms
 *
 * This allows fast-forwarding through time-dependent tests.
 *
 * ## Thread Safety
 * Platform resolution is thread-safe through lazy initialization pattern.
 * Multiple calls to [getPlatformSafely] from different threads will only
 * attempt DI resolution once (though not formally synchronized).
 *
 * @see IClock for interface documentation
 * @see WWWPlatform for simulation configuration
 */
@OptIn(ExperimentalTime::class)
class SystemClock :
    IClock,
    KoinComponent {
    private var platform: WWWPlatform? = null

    // iOS FIX: Removed init{} block that calls DI get() to prevent potential deadlocks
    // Platform is now resolved lazily on first access

    /**
     * Lazily resolves [WWWPlatform] from Koin DI with error handling.
     *
     * **iOS Safety**: This lazy approach prevents deadlocks that occur when
     * calling DI get() in init{} blocks on iOS.
     *
     * @return Platform instance if available, null if DI not initialized or platform missing
     */
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
            platform?.getSimulation()?.now() ?: Clock.System.now()
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
