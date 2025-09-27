package com.worldwidewaves.shared.utils

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

import androidx.compose.runtime.Composable
import com.worldwidewaves.shared.PlatformEnabler
import com.worldwidewaves.shared.utils.Log
import platform.Foundation.NSURL
import platform.UIKit.UIAlertAction
import platform.UIKit.UIAlertActionStyleDefault
import platform.UIKit.UIAlertController
import platform.UIKit.UIAlertControllerStyleAlert
import platform.UIKit.UIApplication

class IOSPlatformEnabler : PlatformEnabler {
    override fun openEventActivity(eventId: String) {
        Log.i("IOSPlatformEnabler", "Opening Event for eventId: $eventId")
        toast("Event details: $eventId")
    }

    override fun openWaveActivity(eventId: String) {
        Log.i("IOSPlatformEnabler", "Opening Wave for eventId: $eventId")
        toast("Wave activity: $eventId")
    }

    override fun toast(message: String) {
        val alert =
            UIAlertController.alertControllerWithTitle(
                title = "WorldWideWaves",
                message = message,
                preferredStyle = UIAlertControllerStyleAlert,
            )

        alert.addAction(
            UIAlertAction.actionWithTitle(
                title = "OK",
                style = UIAlertActionStyleDefault,
                handler = null,
            ),
        )

        UIApplication.sharedApplication.keyWindow?.rootViewController?.presentViewController(
            alert,
            animated = true,
            completion = null,
        )
    }

    @Composable
    override fun OpenUrl(url: String) {
        openUrl(url)
    }

    override fun openUrl(url: String) {
        try {
            val nsUrl = NSURL.URLWithString(url)
            if (nsUrl != null && UIApplication.sharedApplication.canOpenURL(nsUrl)) {
                UIApplication.sharedApplication.openURL(nsUrl)
            } else {
                Log.e("IOSPlatformEnabler", "Cannot open URL: $url")
            }
        } catch (e: Exception) {
            Log.e("IOSPlatformEnabler", "Failed to open URL: $url", throwable = e)
        }
    }
}
