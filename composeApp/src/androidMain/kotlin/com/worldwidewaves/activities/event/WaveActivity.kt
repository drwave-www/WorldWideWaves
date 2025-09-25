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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.ui.screens.SharedWaveScreen
import com.worldwidewaves.shared.WWWPlatform
import org.koin.android.ext.android.inject
import kotlin.time.ExperimentalTime

/**
 * Android Wave Participation Activity.
 * Handles Android-specific navigation while delegating UI to shared components.
 */
@OptIn(ExperimentalTime::class)
class WaveActivity : AbstractEventWaveActivity() {
    private val clock: IClock by inject()
    private val platform: WWWPlatform by inject()


    @Composable
    override fun Screen(
        modifier: Modifier,
        event: IWWWEvent,
    ) {
        val context = LocalContext.current

        SharedWaveScreen(
            event = event,
            platform = platform,
            clock = clock,
            modifier = modifier,
            onNavigateToFullMap = { eventId ->
                context.startActivity(
                    Intent(context, EventFullMapActivity::class.java).apply {
                        putExtra("eventId", eventId)
                    }
                )
            }
        )
    }
}
