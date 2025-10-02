package com.worldwidewaves.shared.utils

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
import androidx.compose.runtime.DisposableEffect
import com.worldwidewaves.shared.ui.activities.MainScreen
import platform.Foundation.NSNotificationCenter
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIApplicationDidEnterBackgroundNotification
import platform.UIKit.UIApplicationWillEnterForegroundNotification
import platform.UIKit.UIApplicationWillResignActiveNotification
import platform.darwin.NSObjectProtocol

@Composable
internal fun BindIosLifecycle(host: MainScreen) {
    DisposableEffect(host) {
        val nc = NSNotificationCenter.defaultCenter

        val obsActive: NSObjectProtocol =
            nc.addObserverForName(
                name = UIApplicationDidBecomeActiveNotification,
                `object` = null,
                queue = null,
            ) { _ -> host.onResume() }

        val obsForeground: NSObjectProtocol =
            nc.addObserverForName(
                name = UIApplicationWillEnterForegroundNotification,
                `object` = null,
                queue = null,
            ) { _ -> host.onResume() }

        val obsResign: NSObjectProtocol =
            nc.addObserverForName(
                name = UIApplicationWillResignActiveNotification,
                `object` = null,
                queue = null,
            ) { _ -> host.onPause() }

        val obsBackground: NSObjectProtocol =
            nc.addObserverForName(
                name = UIApplicationDidEnterBackgroundNotification,
                `object` = null,
                queue = null,
            ) { _ -> host.onPause() }

        // First time shown ≈ onResume
        host.onResume()

        onDispose {
            // View is being torn down
            host.onPause()
            host.onDestroy()
            nc.removeObserver(obsActive)
            nc.removeObserver(obsForeground)
            nc.removeObserver(obsResign)
            nc.removeObserver(obsBackground)
        }
    }
}
