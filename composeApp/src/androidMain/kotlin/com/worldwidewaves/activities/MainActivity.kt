package com.worldwidewaves.activities

/*
 * Copyright 2024 DrWave
 *
 * WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and
 * countries, culminating in a global wave. The project aims to transcend physical and cultural
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
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.worldwidewaves.activities.utils.TabManager
import com.worldwidewaves.activities.utils.setStatusBarColor
import com.worldwidewaves.compose.tabs.AboutScreen
import com.worldwidewaves.compose.tabs.EventsListScreen
import com.worldwidewaves.compose.tabs.SettingsScreen
import com.worldwidewaves.shared.WWWGlobals.Companion.CONST_SPLASH_MIN_DURATION
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_EXT_TABBAR_HEIGHT
import com.worldwidewaves.shared.events.WWWEvents
import com.worldwidewaves.shared.generated.resources.about_icon
import com.worldwidewaves.shared.generated.resources.about_icon_selected
import com.worldwidewaves.shared.generated.resources.settings_icon
import com.worldwidewaves.shared.generated.resources.settings_icon_selected
import com.worldwidewaves.shared.generated.resources.waves_icon
import com.worldwidewaves.shared.generated.resources.waves_icon_selected
import com.worldwidewaves.theme.AppTheme
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.android.ext.android.inject
import com.worldwidewaves.shared.generated.resources.Res as ShRes

// ----------------------------

private val tabInfo = listOf(
    Pair(ShRes.drawable.waves_icon, ShRes.drawable.waves_icon_selected),
    Pair(ShRes.drawable.about_icon, ShRes.drawable.about_icon_selected),
    Pair(ShRes.drawable.settings_icon, ShRes.drawable.settings_icon_selected)
)

// ----------------------------

open class MainActivity : AppCompatActivity() {

    private val eventsListScreen: EventsListScreen by inject()
    private val aboutScreen: AboutScreen by inject()
    private val settingsScreen: SettingsScreen by inject()
    private val events: WWWEvents by inject()

    /** Flag updated when `events.loadEvents()` finishes. */
    @Volatile
    private var isDataLoaded: Boolean = false

    protected val tabManager = TabManager(
        listOf(
            eventsListScreen,
            aboutScreen,
            settingsScreen
        )
    ) { isSelected, tabIndex, contentDescription ->
        TabBarItem(isSelected, tabIndex, contentDescription)
    }

    // ----------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        /* ---------------------------------------------------------------------
         * Official Android 12+ splash-screen installation
         * The splash remains until BOTH: min duration elapsed AND data loaded.
         * ------------------------------------------------------------------- */

        val splashScreen = installSplashScreen()

        // Record start time to enforce minimum duration
        val startTime = System.currentTimeMillis()

        // Keep the official splash screen visible until both conditions are met
        splashScreen.setKeepOnScreenCondition { 
            val elapsed = System.currentTimeMillis() - startTime
            !isDataLoaded || elapsed < CONST_SPLASH_MIN_DURATION.inWholeMilliseconds
        }

        super.onCreate(savedInstanceState)

        setStatusBarColor(window)

        /* Hide system UI like old SplashActivity */
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
        window.statusBarColor = android.graphics.Color.TRANSPARENT

        setContent {
            AppTheme {
                Surface(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                    tabManager.TabView()
                }
            }
        }

        /* Begin loading events – when done, flag so splash can disappear */
        events.loadEvents(onTermination = {
            isDataLoaded = true
        })

        /* Also enforce minimum duration */
        lifecycleScope.launch {
            kotlinx.coroutines.delay(CONST_SPLASH_MIN_DURATION)
        }
    }

    // ----------------------------

    @Composable
    private fun TabBarItem(
        isSelected: Boolean,
        tabIndex: Int,
        contentDescription: String?
    ) {
        Image(
            painter = painterResource(if (!isSelected) tabInfo[tabIndex].first else tabInfo[tabIndex].second),
            contentDescription = contentDescription,
            modifier = Modifier.height(DIM_EXT_TABBAR_HEIGHT.dp),
            contentScale = ContentScale.Fit
        )
    }
}
