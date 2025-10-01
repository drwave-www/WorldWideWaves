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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.worldwidewaves.shared.map.EventMapConfig
import com.worldwidewaves.shared.map.IOSEventMap
import com.worldwidewaves.shared.map.MapCameraPosition
import com.worldwidewaves.shared.ui.activities.WWWEventActivity
import com.worldwidewaves.shared.ui.activities.WWWFullMapActivity
import com.worldwidewaves.shared.ui.activities.WWWMainActivity
import com.worldwidewaves.shared.ui.activities.WWWWaveActivity
import com.worldwidewaves.shared.utils.BindIosLifecycle
import com.worldwidewaves.shared.utils.Log
import com.worldwidewaves.shared.utils.finishIOS
import com.worldwidewaves.shared.viewmodels.MapViewModel
import org.koin.mp.KoinPlatform
import platform.UIKit.UIViewController

private const val TAG = "RootController"

// ---------- Small helpers (DI + VC factory) ----------

class VCBox(
    var vc: UIViewController? = null,
)

private fun diEnabler(): PlatformEnabler = KoinPlatform.getKoin().get()

private fun diMapVm(): MapViewModel = KoinPlatform.getKoin().get()

/** Common VC creator that wires `finish()` and logs entry per screen. */
private inline fun makeComposeVC(
    logLabel: String,
    crossinline finish: @Composable (finish: () -> Unit) -> Unit,
): UIViewController {
    val box = VCBox()
    val vc =
        ComposeUIViewController(configure = { enforceStrictPlistSanityCheck = false }) {
            Log.v(TAG, ">>> ENTERING $logLabel")
            finish { box.vc?.finishIOS() }
        }

    box.vc = vc
    return vc
}

// ---------- Public factories ----------

@Throws(Throwable::class)
fun makeMainViewController(): UIViewController =
    makeComposeVC("IOS MAIN VIEW CONTROLLER") {
        val enabler = diEnabler()
        WWWMainActivity(platformEnabler = enabler).Draw()
    }

@Suppress("unused")
@Throws(Throwable::class)
fun makeEventViewController(eventId: String): UIViewController =
    makeComposeVC("IOS EVENT VIEW CONTROLLER") { finish ->
        val enabler = diEnabler()
        val mapVm = diMapVm()

        val host =
            remember(eventId) {
                WWWEventActivity(eventId = eventId, platformEnabler = enabler, mapViewModel = mapVm)
            }

        BindIosLifecycle(host)

        host.asComponent(
            eventMapBuilder = { event -> IOSEventMap(event) },
            onFinish = finish,
        )
    }

@Suppress("unused")
@Throws(Throwable::class)
fun makeWaveViewController(eventId: String): UIViewController =
    makeComposeVC("IOS WAVE VIEW CONTROLLER") { finish ->
        val enabler = diEnabler()

        val host =
            remember(eventId) {
                WWWWaveActivity(eventId = eventId, platformEnabler = enabler)
            }

        BindIosLifecycle(host)

        host.asComponent(
            eventMapBuilder = { event -> IOSEventMap(event) },
            onFinish = finish,
        )
    }

@Suppress("unused")
@Throws(Throwable::class)
fun makeFullMapViewController(eventId: String): UIViewController =
    makeComposeVC("IOS FULL MAP VIEW CONTROLLER") { finish ->
        val enabler = diEnabler()

        val host =
            remember(eventId) {
                WWWFullMapActivity(eventId = eventId, platformEnabler = enabler)
            }

        BindIosLifecycle(host)

        host.asComponent(
            eventMapBuilder = { event ->
                IOSEventMap(
                    event,
                    mapConfig =
                        EventMapConfig(
                            initialCameraPosition = MapCameraPosition.WINDOW,
                            autoTargetUserOnFirstLocation = true,
                        ),
                )
            },
            onFinish = finish,
        )
    }
