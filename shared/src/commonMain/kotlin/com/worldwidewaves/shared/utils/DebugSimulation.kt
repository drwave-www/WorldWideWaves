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

package com.worldwidewaves.shared.utils

import com.worldwidewaves.shared.WWWGlobals
import com.worldwidewaves.shared.WWWPlatform
import com.worldwidewaves.shared.WWWSimulation
import com.worldwidewaves.shared.events.utils.Position
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.koin.mp.KoinPlatform
import kotlin.time.ExperimentalTime

/**
 * Unified debug simulation setup for all platforms.
 *
 * Sets up default simulation for development and testing when in debug build.
 * Uses identical parameters across Android, iOS, and other platforms to ensure
 * consistent testing behavior.
 *
 * Called after Koin initialization when WWWPlatform is available.
 */
@OptIn(ExperimentalTime::class)
fun setupDebugSimulation() {
    try {
        // Check if debug mode is enabled using LogConfig (which reflects BuildConfig)
        if (WWWGlobals.LogConfig.ENABLE_DEBUG_LOGGING) {
            Log.d("DebugSimulation", "Setting up cross-platform DEBUG simulation")

            val wwwPlatform = KoinPlatform.getKoin().get<WWWPlatform>()
            val timeZone = TimeZone.of("Europe/Paris")
            val now = LocalDateTime(2026, 7, 14, 17, 59).toInstant(timeZone)

            wwwPlatform.setSimulation(
                WWWSimulation(
                    startDateTime = now,
                    // Use test-verified Paris coordinates (known to be inside paris_france event area)
                    userPosition = Position(lat = 48.8566, lng = 2.3522),
                    initialSpeed = WWWGlobals.Wave.DEFAULT_SPEED_SIMULATION,
                ),
            ) // In Paris, 1h wave duration = 2mn real time

            Log.i("DebugSimulation", "Cross-platform DEBUG simulation setup completed")
        } else {
            Log.d("DebugSimulation", "Debug logging disabled, skipping simulation setup")
        }
    } catch (e: Exception) {
        Log.e("DebugSimulation", "Failed to setup DEBUG simulation", e)
        // Non-critical error, don't throw - debug simulation failure shouldn't crash the app
    }
}
