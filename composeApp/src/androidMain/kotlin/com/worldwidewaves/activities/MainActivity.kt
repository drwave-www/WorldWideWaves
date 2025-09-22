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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.google.android.play.core.splitcompat.SplitCompat
import com.worldwidewaves.BuildConfig
import com.worldwidewaves.activities.utils.TabManager
import com.worldwidewaves.activities.utils.hideStatusBar
import com.worldwidewaves.activities.utils.setStatusBarColor
import com.worldwidewaves.compose.common.SimulationModeChip
import com.worldwidewaves.compose.tabs.AboutScreen
import com.worldwidewaves.compose.tabs.DebugScreen
import com.worldwidewaves.compose.tabs.EventsListScreen
import com.worldwidewaves.shared.MokoRes
import com.worldwidewaves.shared.WWWPlatform
import com.worldwidewaves.shared.events.WWWEvents
import com.worldwidewaves.shared.generated.resources.about_icon
import com.worldwidewaves.shared.generated.resources.about_icon_selected
import com.worldwidewaves.shared.generated.resources.background
import com.worldwidewaves.shared.generated.resources.debug_icon
import com.worldwidewaves.shared.generated.resources.debug_icon_selected
import com.worldwidewaves.shared.generated.resources.waves_icon
import com.worldwidewaves.shared.generated.resources.waves_icon_selected
import com.worldwidewaves.shared.generated.resources.www_logo_transparent
import com.worldwidewaves.shared.WWWGlobals.Companion.Dimensions
import com.worldwidewaves.shared.WWWGlobals.Companion.TabBar
import com.worldwidewaves.theme.AppTheme
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.android.ext.android.inject
import com.worldwidewaves.shared.generated.resources.Res as ShRes

// ----------------------------

private fun getTabInfo(includeDebug: Boolean) =
    if (includeDebug) {
        listOf(
            Pair(ShRes.drawable.waves_icon, ShRes.drawable.waves_icon_selected),
            Pair(ShRes.drawable.about_icon, ShRes.drawable.about_icon_selected),
            Pair(ShRes.drawable.debug_icon, ShRes.drawable.debug_icon_selected),
        )
    } else {
        listOf(
            Pair(ShRes.drawable.waves_icon, ShRes.drawable.waves_icon_selected),
            Pair(ShRes.drawable.about_icon, ShRes.drawable.about_icon_selected),
        )
    }

// ----------------------------

open class MainActivity : AppCompatActivity() {
    private val eventsListScreen: EventsListScreen by inject()
    private val aboutScreen: AboutScreen by inject()
    private val debugScreen: DebugScreen? by inject()
    private val events: WWWEvents by inject()
    private val platform: WWWPlatform by inject()

    /** Flag updated when `events.loadEvents()` finishes. */
    @Volatile
    private var isDataLoaded: Boolean = false

    /** Flow observed by Compose to know when we can display main content. */
    private val isSplashFinished = MutableStateFlow(false)

    /** Controls how long the *official* (system) splash stays on-screen (~10 ms). */
    private var isOfficialSplashDismissed = false

    protected val tabManager by lazy {
        val screens = mutableListOf(
            eventsListScreen,
            aboutScreen,
        )
        debugScreen?.let { screens.add(it) }

        TabManager(
            screens.toList()
        ) { isSelected, tabIndex, contentDescription ->
            TabBarItem(isSelected, tabIndex, contentDescription, screens.size)
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

        // Record start time to enforce minimum duration
        val startTime = System.currentTimeMillis()

        /* ------------------------------------------------------------------
         * Keep the *official* splash for some time so it can show our theme
         * colours/logo, then dismiss it and let the programmatic splash take
         * over until the real readiness criteria are met.
         * ---------------------------------------------------------------- */
        splashScreen.setKeepOnScreenCondition {
            if (!isOfficialSplashDismissed) {
                lifecycleScope.launch {
                    kotlinx.coroutines.delay(500L) // 500 ms
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
            window.setDecorFitsSystemWindows(false)
        } else {
            @Suppress("DEPRECATION")
            window.statusBarColor = android.graphics.Color.TRANSPARENT
        }

        setContent {
            AppTheme {
                Surface(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                    // Box to stack main content and simulation-mode overlay
                    Box(modifier = Modifier.fillMaxSize()) {
                        val ready by isSplashFinished.collectAsState()
                        if (ready) {
                            tabManager.TabView()
                        } else {
                            ProgrammaticSplashScreen()
                        }

                        // -----------------------------------------------------------------
                        //  Global Simulation-Mode chip shown whenever the mode is enabled
                        // -----------------------------------------------------------------
                        SimulationModeChip(platform)
                    }
                }
            }
        }

        // Begin loading events – when done, flag so splash can disappear
        events.loadEvents(onTermination = {
            isDataLoaded = true
            checkSplashFinished(startTime)
        })

        // Also enforce minimum duration
        lifecycleScope.launch {
            kotlinx.coroutines.delay(2000) // Timing.SPLASH_MIN_DURATION
            checkSplashFinished(startTime)
        }
    }

    /** Updates [isSplashFinished] once both data and min duration requirements are met. */
    private fun checkSplashFinished(startTime: Long) {
        val elapsed = System.currentTimeMillis() - startTime
        if (isDataLoaded && elapsed >= 2000) { // Timing.SPLASH_MIN_DURATION.inWholeMilliseconds
            isSplashFinished.update { true }
        }
    }

    // ----------------------------

    @Composable
    private fun TabBarItem(
        isSelected: Boolean,
        tabIndex: Int,
        contentDescription: String?,
        totalTabs: Int
    ) {
        val tabInfo = getTabInfo(totalTabs > 2)
        Image(
            painter = painterResource(if (!isSelected) tabInfo[tabIndex].first else tabInfo[tabIndex].second),
            contentDescription = contentDescription,
            modifier = Modifier.height(TabBar.EXT_HEIGHT.dp),
            contentScale = ContentScale.Fit,
        )
    }

    // -------------------------------------------------
    // Programmatic Splash UI (mirrors previous design)
    // -------------------------------------------------

    @Composable
    private fun ProgrammaticSplashScreen() {
        Box {
            Image(
                painter = painterResource(ShRes.drawable.background),
                contentDescription = stringResource(MokoRes.strings.background_description),
                contentScale = ContentScale.FillHeight,
                modifier = Modifier.fillMaxSize(),
            )
            Image(
                painter = painterResource(ShRes.drawable.www_logo_transparent),
                contentDescription = stringResource(MokoRes.strings.logo_description),
                modifier =
                    Modifier
                        .align(androidx.compose.ui.Alignment.BottomCenter)
                        .padding(bottom = Dimensions.DEFAULT_INT_PADDING.dp), // original SplashActivity padding
            )
        }
    }
}
