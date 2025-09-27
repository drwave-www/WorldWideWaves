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
@file:OptIn(ExperimentalTime::class)

package com.worldwidewaves.activities.event

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.google.android.play.core.splitcompat.SplitCompat
import com.worldwidewaves.compose.map.AndroidEventMap
import com.worldwidewaves.shared.ui.activities.WWWWaveActivity
import com.worldwidewaves.utils.AndroidPlatformEnabler
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class WaveActivity : AppCompatActivity() {
    private var waveActivity: WWWWaveActivity? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val eventId = intent.getStringExtra("eventId")

        // Ensure dynamic-feature splits are available immediately
        SplitCompat.install(this)

        if (eventId != null) {
            val platformEnabler = AndroidPlatformEnabler(this)
            waveActivity = WWWWaveActivity(eventId, platformEnabler)
            waveActivity?.onEventLoaded { event ->
                // Construct the event map
                val eventMap = AndroidEventMap(event, context = this as AppCompatActivity)
                setContent {
                    waveActivity!!.Draw(event, eventMap = eventMap, onFinish = { finish() })
                }
            }
        }
    }

    override fun onDestroy() {
        waveActivity?.onDestroy()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        waveActivity?.onResume()
    }

    override fun onPause() {
        waveActivity?.onPause()
        super.onPause()
    }
}
