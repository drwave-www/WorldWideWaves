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

package com.worldwidewaves.constants

import com.worldwidewaves.shared.WWWGlobals

/**
 * Android-specific UI constants.
 * This file contains constants specific to the Android implementation.
 * Most timing constants are now in shared WWWGlobals for cross-platform consistency.
 */
object AndroidUIConstants {
    object Timing {
        /** System splash screen duration - short handoff to programmatic splash */
        val SPLASH_MIN_DURATION_MS = WWWGlobals.Timing.SYSTEM_SPLASH_DURATION.inWholeMilliseconds

        /** Splash screen maximum duration - uses shared constant */
        val SPLASH_MAX_DURATION_MS = WWWGlobals.Timing.SPLASH_MAX_DURATION.inWholeMilliseconds

        /** Splash screen check interval - uses shared constant */
        const val SPLASH_CHECK_INTERVAL_MS = WWWGlobals.Timing.SPLASH_CHECK_INTERVAL_MS
    }
}
