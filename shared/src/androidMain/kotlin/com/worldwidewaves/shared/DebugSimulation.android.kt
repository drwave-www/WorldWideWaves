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

package com.worldwidewaves.shared

import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.utils.Log
import com.worldwidewaves.shared.utils.LogConfig
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.koin.mp.KoinPlatform
import kotlin.time.ExperimentalTime

/**
 * Android implementation of debug simulation setup.
 *
 * Sets up default simulation for development and testing when debug logging is enabled.
 */
@OptIn(ExperimentalTime::class)
actual fun setupDebugSimulation() {
    try {
        if (LogConfig.ENABLE_DEBUG_LOGGING) {
            Log.d("DebugSimulation", "Setting up Android DEBUG simulation")

            val wwwPlatform = KoinPlatform.getKoin().get<WWWPlatform>()
            val timeZone = TimeZone.of("Europe/Paris")
            val now = LocalDateTime(2026, 7, 14, 17, 59).toInstant(timeZone)

            wwwPlatform.setSimulation(
                WWWSimulation(
                    startDateTime = now,
                    // Use test-verified Paris coordinates (known to be inside area)
                    userPosition = Position(lat = 48.8566, lng = 2.3522),
                    initialSpeed = WWWGlobals.Wave.DEFAULT_SPEED_SIMULATION,
                ),
            ) // In Paris, 1h is 2mn

            Log.i("DebugSimulation", "Android DEBUG simulation setup completed")
        } else {
            Log.d("DebugSimulation", "Not in DEBUG mode, skipping simulation setup")
        }
    } catch (e: Exception) {
        Log.e("DebugSimulation", "Failed to setup Android DEBUG simulation", e)
    }
}
