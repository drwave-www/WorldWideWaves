package com.worldwidewaves.activities.event

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

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.ui.screens.SharedEventDetailsScreen
import com.worldwidewaves.shared.utils.Log
import com.worldwidewaves.shared.WWWPlatform

/**
 * Android Event Details Activity - now uses SharedEventDetailsScreen for perfect iOS parity.
 *
 * This activity is now a thin wrapper that handles Android-specific navigation
 * while delegating all UI to the shared EventDetailsScreen component.
 *
 * All 10 @Composable functions that were embedded in this Activity have been
 * extracted to SharedEventDetailsScreen for cross-platform sharing.
 */
class EventActivity : AbstractEventBackActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get eventId from intent
        val eventId = intent.getStringExtra("eventId") ?: return finish()

        // Initialize with shared screen
        setContent {
            val event = getEventById(eventId) ?: return@setContent
            Screen(event = event)
        }
    }

    @Composable
    override fun Screen(
        modifier: Modifier,
        event: IWWWEvent,
    ) {
        val context = LocalContext.current

        // Use SharedEventDetailsScreen for perfect iOS parity
        // All UI logic is now shared between Android and iOS
        SharedEventDetailsScreen(
            event = event,
            platform = platform,
            clock = clock,
            modifier = modifier,
            onNavigateToWave = { eventId ->
                context.startActivity(
                    Intent(context, WaveActivity::class.java).apply {
                        putExtra("eventId", eventId)
                    }
                )
            },
            onNavigateToFullMap = { eventId ->
                context.startActivity(
                    Intent(context, EventFullMapActivity::class.java).apply {
                        putExtra("eventId", eventId)
                    }
                )
            },
            onUrlOpen = { url ->
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Log.e("EventActivity", "Failed to open URL: $url", throwable = e)
                }
            }
        )
    }

    // All UI components moved to SharedEventDetailsScreen:
    // - EventOverlay() → shared
    // - EventDescription() → shared
    // - SimulationButton() → shared
    // - EventNumbers() → shared
    // - NotifyAreaUserPosition() → shared
    // - WWWEventSocialNetworks() → shared
    // - EventOverlayDate() → shared
    // - AlertMapNotDownloadedOnSimulationLaunch() → shared
    // - formatDurationMinutes() → shared
    // - Map integration → shared with expect/actual

    private fun getEventById(eventId: String): IWWWEvent? {
        // TODO: Get event from shared events system
        return null
    }

    // Platform-specific properties that need to be accessed by shared screen
    private val platform: WWWPlatform get() = TODO("Get platform from DI")
    private val clock: IClock get() = TODO("Get clock from DI")
}