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

class IOSPlatformEnabler : PlatformEnabler {
    override fun openEventActivity(eventId: String) {
        TODO()
    }

    override fun openWaveActivity(eventId: String) {
        TODO("Not yet implemented")
    }

    override fun toast(message: String) {
        TODO("Not yet implemented")
    }

    @Composable
    override fun OpenUrl(url: String) {
        TODO("Not yet implemented")
    }
}
