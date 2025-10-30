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

package com.worldwidewaves

import android.app.Application
import org.maplibre.android.MapLibre

/**
 * Test Application class for instrumented tests.
 *
 * This custom Application ensures MapLibre SDK is properly initialized
 * with the application context before any tests run. This prevents
 * NullPointerException in MapLibre's FileSource when it tries to access
 * SharedPreferences.
 *
 * The test manifest must specify this as the application class:
 * ```xml
 * <application android:name="com.worldwidewaves.TestApplication">
 * ```
 */
class TestApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize MapLibre SDK with application context
        // This is critical for FileSource to have proper context for cache/preferences
        MapLibre.getInstance(this)
    }
}
