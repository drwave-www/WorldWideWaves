package com.worldwidewaves.activities

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

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.google.android.play.core.splitcompat.SplitCompat
import com.worldwidewaves.activities.utils.hideStatusBar
import com.worldwidewaves.activities.utils.setStatusBarColor
import com.worldwidewaves.shared.ui.activities.WWWMainActivity
import com.worldwidewaves.utils.AndroidPlatformEnabler
import kotlinx.coroutines.launch

// ----------------------------

open class MainActivity : AppCompatActivity() {
    /** Controls how long the *official* (system) splash stays on-screen (~10 ms). */
    private var isOfficialSplashDismissed = false

    private var mainActivityImpl: WWWMainActivity? = null

    // ----------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        // Ensure the Activity is split-compat aware so newly installed
        // dynamic-feature assets (maps) are immediately visible.
        SplitCompat.installActivity(this)

        /* ---------------------------------------------------------------------
         * Official Android 12+ splash-screen installation
         * The splash remains until BOTH: min duration elapsed AND data loaded.
         * ------------------------------------------------------------------- */
        val splashScreen = installSplashScreen()

        /* ------------------------------------------------------------------
         * Keep the *official* splash for some time so it can show our theme
         * colours/logo, then dismiss it and let the programmatic splash take
         * over until the real readiness criteria are met.
         * ---------------------------------------------------------------- */
        splashScreen.setKeepOnScreenCondition {
            if (!isOfficialSplashDismissed) {
                lifecycleScope.launch {
                    kotlinx.coroutines.delay(com.worldwidewaves.shared.WWWGlobals.Timing.SYSTEM_SPLASH_DURATION.inWholeMilliseconds)
                    isOfficialSplashDismissed = true
                }
                true // keep the official splash right now
            } else {
                false // dismiss – programmatic splash continues in Compose
            }
        }

        super.onCreate(savedInstanceState)
        setStatusBarColor(window)
        hideStatusBar(this)

        // Hide system UI like old SplashActivity
        window.decorView.post {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.insetsController?.hide(WindowInsets.Type.statusBars())
                window.insetsController?.hide(WindowInsets.Type.navigationBars())
            } else {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility =
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            @Suppress("DEPRECATION")
            window.setDecorFitsSystemWindows(false)
        } else {
            @Suppress("DEPRECATION")
            window.statusBarColor = android.graphics.Color.TRANSPARENT
        }

        setContent {
            if (mainActivityImpl == null) {
                mainActivityImpl = WWWMainActivity(AndroidPlatformEnabler(this))
            }
            mainActivityImpl!!.Draw()
        }
    }

    override fun onResume() {
        super.onResume()
        mainActivityImpl?.onResume()
    }

    override fun onPause() {
        mainActivityImpl?.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        mainActivityImpl?.onDestroy()
        super.onDestroy()
    }
}
