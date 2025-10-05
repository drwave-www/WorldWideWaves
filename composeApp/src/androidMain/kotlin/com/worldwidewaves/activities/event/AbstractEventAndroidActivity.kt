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

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.google.android.play.core.splitcompat.SplitCompat
import com.worldwidewaves.compose.map.AndroidEventMap
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.ui.activities.BaseWaveActivityScreen
import com.worldwidewaves.utils.AndroidPlatformEnabler
import kotlin.time.ExperimentalTime

/**
 * Abstract base class for Android event-related activities.
 *
 * This class eliminates code duplication across EventActivity, EventFullMapActivity,
 * and WaveActivity by providing common functionality for:
 * - Event ID extraction from intent
 * - SplitCompat initialization
 * - Platform enabler creation
 * - Activity lifecycle delegation
 * - Common Compose setup patterns
 *
 * @param T The type of shared activity implementation (EventDetailScreen, FullMapScreen, etc.)
 */
@OptIn(ExperimentalTime::class)
abstract class AbstractEventAndroidActivity<T : BaseWaveActivityScreen> : AppCompatActivity() {
    protected var activityImpl: T? = null

    /**
     * Factory method to create the shared activity implementation.
     * Each concrete activity must implement this to provide the specific implementation type.
     *
     * @param eventId The event ID extracted from the intent
     * @param platformEnabler Android-specific platform enabler
     * @return The shared activity implementation instance
     */
    protected abstract fun createActivityImpl(
        eventId: String,
        platformEnabler: AndroidPlatformEnabler,
    ): T

    /**
     * Factory method to create the event map builder.
     * Override this method to customize map configuration (e.g., for EventFullMapActivity).
     *
     * @return Function that creates AndroidEventMap from IWWWEvent
     */
    protected open fun createEventMapBuilder(): (IWWWEvent) -> AndroidEventMap =
        { event ->
            AndroidEventMap(event, context = this)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val eventId = intent.getStringExtra("eventId")

        // Ensure dynamic-feature splits are available immediately
        SplitCompat.install(this)

        val platformEnabler = AndroidPlatformEnabler(this)
        if (eventId != null) {
            activityImpl = createActivityImpl(eventId, platformEnabler)
            setContent {
                activityImpl!!.asComponent(
                    eventMapBuilder = createEventMapBuilder(),
                    onFinish = { finish() },
                )
            }
        }
    }

    override fun onDestroy() {
        activityImpl?.onDestroy()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        activityImpl?.onResume()
    }

    override fun onPause() {
        activityImpl?.onPause()
        super.onPause()
    }
}
