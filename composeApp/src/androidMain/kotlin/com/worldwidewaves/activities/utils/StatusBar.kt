package com.worldwidewaves.activities.utils

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

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowInsetsController
import androidx.core.view.WindowCompat

/**
 * Sets the status bar to transparent with light icons to match the app's full-screen design.
 *
 * Configures the system UI to be edge-to-edge by:
 * • Making the status bar transparent to blend with the app background
 * • Setting light status bar icons for better visibility against dark backgrounds
 * • Ensuring compatibility across different Android versions (using WindowCompat for newer versions)
 */

fun setStatusBarColor(window: Window) {
    // Make the status bar transparent
    window.statusBarColor = android.graphics.Color.TRANSPARENT

    // Make the navigation bar transparent on newer devices
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        window.isNavigationBarContrastEnforced = false
    }

    // Use WindowCompat to configure edge-to-edge display
    WindowCompat.setDecorFitsSystemWindows(window, false)

    // Set light status bar icons for better visibility against dark backgrounds
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        window.insetsController?.setSystemBarsAppearance(
            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
        )
    } else {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
    }
}

/**
 * Hides the status bar for a full immersive experience.
 */
fun hideStatusBar(activity: Activity) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        activity.window.insetsController?.hide(
            android.view.WindowInsets.Type
                .statusBars(),
        )
    } else {
        @Suppress("DEPRECATION")
        activity.window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
        )
    }
}
