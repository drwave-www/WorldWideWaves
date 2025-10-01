package com.worldwidewaves.shared.events.config

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

import com.worldwidewaves.shared.WWWGlobals.FileSystem
import com.worldwidewaves.shared.events.utils.CoroutineScopeProvider
import com.worldwidewaves.shared.events.utils.DefaultCoroutineScopeProvider
import com.worldwidewaves.shared.generated.resources.Res
import com.worldwidewaves.shared.utils.Log
import org.jetbrains.compose.resources.ExperimentalResourceApi

interface EventsConfigurationProvider {
    suspend fun geoEventsConfiguration(): String
}

class DefaultEventsConfigurationProvider(
    private val coroutineScopeProvider: CoroutineScopeProvider = DefaultCoroutineScopeProvider(),
) : EventsConfigurationProvider {
    @OptIn(ExperimentalResourceApi::class)
    override suspend fun geoEventsConfiguration(): String =
        coroutineScopeProvider.withIOContext {
            Log.i("EventsConfigurationProvider", "=== STARTING EVENTS CONFIGURATION LOAD ===")
            Log.i("EventsConfigurationProvider", "Target file: ${FileSystem.EVENTS_CONF}")

            try {
                Log.i("EventsConfigurationProvider", "Attempting Res.readBytes() call...")
                val bytes = Res.readBytes(FileSystem.EVENTS_CONF)
                Log.i("EventsConfigurationProvider", "Successfully read ${bytes.size} bytes from Compose Resources")

                val result = bytes.decodeToString()
                Log.i("EventsConfigurationProvider", "Successfully decoded ${result.length} characters")
                Log.i("EventsConfigurationProvider", "First 100 chars: ${result.take(100)}")
                Log.i("EventsConfigurationProvider", "=== EVENTS CONFIGURATION LOAD SUCCESSFUL ===")
                result
            } catch (e: Exception) {
                Log.e("EventsConfigurationProvider", "=== EVENTS CONFIGURATION LOAD FAILED ===")
                Log.e("EventsConfigurationProvider", "Exception type: ${e::class.simpleName}")
                Log.e("EventsConfigurationProvider", "Exception message: ${e.message}")
                Log.e("EventsConfigurationProvider", "Falling back to hardcoded minimal event for iOS debugging")

                // Fallback to hardcoded minimal event JSON for debugging
                """[
                {
                    "id": "debug_event_ios",
                    "title": "iOS Debug Event",
                    "location": {
                        "latitude": 40.7589,
                        "longitude": -73.9851,
                        "address": "New York, NY"
                    },
                    "scheduledStartTime": "2025-09-27T12:00:00Z",
                    "duration": "PT30M",
                    "description": "Debug event for iOS Compose Resources issue",
                    "status": "upcoming",
                    "waveSettings": {
                        "radius": 100,
                        "speed": 0.5,
                        "color": "#FF0000"
                    },
                    "soundChoreographyId": "debug_sound"
                }
                ]"""
            }
        }
}
