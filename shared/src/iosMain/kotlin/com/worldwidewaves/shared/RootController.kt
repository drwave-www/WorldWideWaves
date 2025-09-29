package com.worldwidewaves.shared

/* Copyright 2025 DrWave
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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.ComposeUIViewController
import com.worldwidewaves.shared.map.IOSEventMap
import com.worldwidewaves.shared.ui.activities.WWWEventActivity
import com.worldwidewaves.shared.ui.activities.WWWMainActivity
import com.worldwidewaves.shared.utils.BindIosLifecycle
import com.worldwidewaves.shared.utils.Log
import com.worldwidewaves.shared.utils.finishIOS
import com.worldwidewaves.shared.viewmodels.MapViewModel
import org.koin.mp.KoinPlatform
import platform.UIKit.UIViewController

const val TAG = "RootController"

@Composable
private fun NotWiredUI() {
    Log.v(TAG, ">>> NotWiredUI ENTER")
    // TODO: Replace with your real Full Map screen Composable.
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color(0xFFFFD54F)),
        // visible amber background
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Not Yet Wired",
            color = Color.Black,
            fontSize = 28.sp,
        )
    }
}

fun makeMainViewController(): UIViewController =
    ComposeUIViewController(configure = { enforceStrictPlistSanityCheck = false }) {
        Log.v(TAG, ">>> ENTERING IOS MAIN VIEW CONTROLLER")
        val enabler: PlatformEnabler = KoinPlatform.getKoin().get()
        WWWMainActivity(platformEnabler = enabler).Draw()
    }

fun makeEventViewController(eventId: String): UIViewController {
    // Use a box to avoid lateinit capture warnings
    class VCBox(
        var vc: UIViewController? = null,
    )
    val box = VCBox()

    val vc =
        ComposeUIViewController(configure = { enforceStrictPlistSanityCheck = false }) {
            Log.v(TAG, ">>> ENTERING IOS EVENT VIEW CONTROLLER")

            val enabler: PlatformEnabler = KoinPlatform.getKoin().get()
            val mapVm: MapViewModel = KoinPlatform.getKoin().get()

            val eventHost =
                WWWEventActivity(
                    eventId = eventId,
                    platformEnabler = enabler,
                    mapViewModel = mapVm,
                )

            BindIosLifecycle(eventHost)

            eventHost.asComponent(
                eventMapBuilder = { event -> IOSEventMap(event) },
                onFinish = { box.vc?.finishIOS() },
            )
        }

    box.vc = vc
    return vc
}

fun makeWaveViewController(eventId: String): UIViewController =
    ComposeUIViewController(configure = { enforceStrictPlistSanityCheck = false }) {
        Log.v(TAG, ">>> ENTERING IOS WAVE VIEW CONTROLLER")

        NotWiredUI()
    }

fun makeFullMapViewController(): UIViewController =
    ComposeUIViewController(configure = { enforceStrictPlistSanityCheck = false }) {
        Log.v(TAG, ">>> ENTERING IOS FULL MAP VIEW CONTROLLER")

        NotWiredUI()
    }
