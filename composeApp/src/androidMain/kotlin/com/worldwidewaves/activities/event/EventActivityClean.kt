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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.ui.screens.SharedEventDetailsScreen
import com.worldwidewaves.shared.utils.Log
import com.worldwidewaves.shared.WWWPlatform
import org.koin.android.ext.android.inject
import kotlin.time.ExperimentalTime

/**
 * Android Event Details Activity - Pure wrapper using SharedEventDetailsScreen.
 *
 * ALL 10 @Composable functions have been extracted to SharedEventDetailsScreen:
 * - EventOverlay() → shared
 * - EventDescription() → shared
 * - SimulationButton() → shared
 * - EventNumbers() → shared
 * - NotifyAreaUserPosition() → shared
 * - WWWEventSocialNetworks() → shared
 * - EventOverlayDate() → shared
 * - AlertMapNotDownloadedOnSimulationLaunch() → shared
 * - formatDurationMinutes() → shared
 * - Map integration → shared with expect/actual
 *
 * This Activity now handles only Android-specific navigation.
 * UI is 100% shared between Android and iOS.
 */
@OptIn(ExperimentalTime::class)
class EventActivity : AbstractEventWaveActivity() {
    private val clock: IClock by inject()
    private val platform: WWWPlatform by inject()

    companion object {
        private const val TAG = "EventActivity"
    }

    @Composable
    override fun Screen(
        modifier: Modifier,
        event: IWWWEvent,
    ) {
        val context = LocalContext.current

        // Use SharedEventDetailsScreen for perfect iOS parity
        // ALL UI logic is now shared between Android and iOS
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
                    Log.e(TAG, "Failed to open URL: $url", throwable = e)
                }
            }
        )
    }

    // ALL UI COMPONENTS MOVED TO SHAREDEVENTDETAILSSCREEN
    // Zero @Composable functions remain in this Activity except Screen() wrapper
}