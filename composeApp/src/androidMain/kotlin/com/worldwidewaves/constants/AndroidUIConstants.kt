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

/**
 * Android-specific UI constants for colors and Android-specific timing values.
 * This file contains constants specific to the Android implementation.
 * For shared constants, see WWWGlobals.
 */
object AndroidUIConstants {

    object Colors {
        /** Success/good performance indicator color (Material Design Green 500) */
        const val GREEN_SUCCESS = 0xFF4CAF50

        /** Warning/medium performance indicator color (Material Design Orange 500) */
        const val ORANGE_WARNING = 0xFFFF9800

        /** Error/poor performance indicator color (Material Design Red 600) */
        const val RED_ERROR = 0xFFF44336
    }

    object Audio {
        /** Default audio volume for sound tests (Android-specific default) */
        const val DEFAULT_VOLUME = 0.8f

        /** 8-bit audio bit depth (Android audio format) */
        const val AUDIO_8_BIT = 8

        /** 16-bit audio bit depth (Android audio format) */
        const val AUDIO_16_BIT = 16
    }

    object Timing {
        /** Splash screen minimum duration for Android implementation (milliseconds) */
        const val SPLASH_MIN_DURATION_MS = 500L

        /** Splash screen maximum duration for Android implementation (milliseconds) */
        const val SPLASH_MAX_DURATION_MS = 2000L

        /** Splash screen check interval (milliseconds) */
        const val SPLASH_CHECK_INTERVAL_MS = 2000
    }
}
