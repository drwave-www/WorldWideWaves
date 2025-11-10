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

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowInsets
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.google.android.play.core.splitcompat.SplitCompat
import com.worldwidewaves.activities.utils.hideStatusBar
import com.worldwidewaves.activities.utils.setStatusBarColor
import com.worldwidewaves.shared.WWWGlobals
import com.worldwidewaves.shared.localization.LocalizationManager
import com.worldwidewaves.shared.ui.activities.MainScreen
import com.worldwidewaves.shared.viewmodels.EventsViewModel
import com.worldwidewaves.utils.AndroidPlatformEnabler
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.mp.KoinPlatform
import java.util.Locale

// ----------------------------

open class MainActivity : AppCompatActivity() {
    /** Controls how long the *official* (system) splash stays on-screen (~10 ms). */
    private var isOfficialSplashDismissed = false

    private var mainActivityImpl: MainScreen? = null

    /** Tracks the last known locale for detecting runtime language changes. */
    private var lastKnownLocale: Locale? = null

    /** EventsViewModel for refreshing events list when returning from EventActivity. */
    private val eventsViewModel: EventsViewModel by inject()

    /**
     * Permission launcher for POST_NOTIFICATIONS (Android 13+).
     * Uses modern ActivityResultContracts API (no deprecated onRequestPermissionsResult).
     */
    private val notificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            Log.i(
                "MainActivity",
                "POST_NOTIFICATIONS permission result: ${if (isGranted) "GRANTED" else "DENIED"}",
            )
            if (!isGranted) {
                Log.w(
                    "MainActivity",
                    "User denied notification permission - notifications will not work until granted in settings",
                )
            }
        }

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
                    delay(WWWGlobals.Timing.SYSTEM_SPLASH_DURATION.inWholeMilliseconds)
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
                mainActivityImpl = MainScreen(AndroidPlatformEnabler(this))
            }
            mainActivityImpl!!.Draw()
        }

        // Request notification permission on Android 13+ (fresh install or app update)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermissionIfNeeded()
        }
    }

    /**
     * Requests POST_NOTIFICATIONS permission on Android 13+ if not already granted.
     *
     * ## Behavior
     * - Only requests on Android 13+ (API 33+)
     * - Only requests if permission not already granted
     * - Requests on EVERY app launch (fresh install or app update)
     * - Uses ActivityResultContracts for modern permission handling
     * - Respects user's "Don't ask again" choice (Android won't show dialog)
     *
     * ## When Permission is Requested
     * - Fresh app install
     * - App update (if permission wasn't requested in previous version)
     * - User revoked permission and app is relaunched
     *
     * ## When Dialog Won't Appear
     * - Permission already granted (most common after first grant)
     * - User selected "Don't ask again" (Android blocks the request)
     * - Android version < 13 (permission not needed)
     */
    private fun requestNotificationPermissionIfNeeded() {
        val permission = Manifest.permission.POST_NOTIFICATIONS

        when (ContextCompat.checkSelfPermission(this, permission)) {
            PackageManager.PERMISSION_GRANTED -> {
                Log.d("MainActivity", "POST_NOTIFICATIONS permission already granted")
            }
            else -> {
                Log.i("MainActivity", "Requesting POST_NOTIFICATIONS permission")
                notificationPermissionLauncher.launch(permission)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mainActivityImpl?.onResume()

        // Refresh events list when returning from EventActivity (picks up favorite changes)
        lifecycleScope.launch {
            eventsViewModel.refreshEvents()
        }
    }

    override fun onPause() {
        mainActivityImpl?.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        mainActivityImpl?.onDestroy()
        super.onDestroy()
    }

    /**
     * Detects runtime locale/language changes and notifies LocalizationManager.
     *
     * ## Purpose
     * Called by Android when configuration changes occur, including:
     * - System language change (Settings → System → Languages)
     * - Per-app language override (Android 13+)
     * - Locale changes via Locale.setDefault()
     * - Layout direction changes (LTR ↔ RTL)
     *
     * ## Behavior
     * When a locale change is detected:
     * 1. Compares new locale with lastKnownLocale
     * 2. Notifies LocalizationManager via StateFlow emission
     * 3. Triggers Compose UI recomposition with new localized strings
     * 4. Updates lastKnownLocale for next comparison
     *
     * ## AndroidManifest Requirement
     * Requires `android:configChanges="locale|layoutDirection"` in manifest
     * to receive this callback instead of activity recreation.
     *
     * @param newConfig The new configuration with updated locale
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        val newLocale =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                newConfig.locales.get(0)
            } else {
                @Suppress("DEPRECATION")
                newConfig.locale
            }

        Log.d("MainActivity", "Configuration changed: locale=$newLocale, lastKnownLocale=$lastKnownLocale")

        if (newLocale != lastKnownLocale) {
            Log.i("MainActivity", "=== Locale Changed ===")
            Log.d("MainActivity", "Old locale: $lastKnownLocale")
            Log.d("MainActivity", "New locale: $newLocale")

            lastKnownLocale = newLocale

            // Notify LocalizationManager of the locale change
            try {
                Log.d("MainActivity", "Notifying LocalizationManager of locale change")
                val localizationManager = KoinPlatform.getKoin().get<LocalizationManager>()
                localizationManager.notifyLocaleChanged(newLocale.toLanguageTag())
                Log.i("MainActivity", "Locale change notification sent successfully")
            } catch (e: Exception) {
                // Log but don't crash - localization is not critical for app function
                Log.w("MainActivity", "Failed to notify locale change: ${e.message}", e)
            }
        }
    }
}
