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

package com.worldwidewaves.map

import android.os.Bundle
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView

/**
 * Minimal test activity for MapView integration tests.
 *
 * This activity provides a real OpenGL rendering context for MapView,
 * which is required for MapView.getMapAsync() callback to fire.
 *
 * The activity is extremely minimal - just a FrameLayout container
 * with a MapView. All map configuration is done by the test class.
 *
 * Usage:
 * ```
 * @get:Rule
 * val activityRule = ActivityScenarioRule(TestMapActivity::class.java)
 *
 * activityRule.scenario.onActivity { activity ->
 *     val mapView = activity.mapView
 *     val mapLibreMap = activity.mapLibreMap
 * }
 * ```
 */
class TestMapActivity : AppCompatActivity() {
    // ============================================================
    // PUBLIC PROPERTIES
    // ============================================================

    /**
     * MapView instance created in onCreate.
     * Exposed for test access.
     */
    lateinit var mapView: MapView
        private set

    /**
     * MapLibreMap instance initialized via getMapAsync.
     * Null until map initialization completes.
     * Exposed for test access.
     */
    var mapLibreMap: MapLibreMap? = null
        private set

    // ============================================================
    // LIFECYCLE
    // ============================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create simple container layout
        val container =
            FrameLayout(this).apply {
                layoutParams =
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT,
                    )
            }

        // Create MapView
        mapView =
            MapView(this).apply {
                layoutParams =
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT,
                    )
            }

        // Add MapView to container
        container.addView(mapView)
        setContentView(container)

        // Initialize MapView lifecycle
        mapView.onCreate(savedInstanceState)
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }
}
