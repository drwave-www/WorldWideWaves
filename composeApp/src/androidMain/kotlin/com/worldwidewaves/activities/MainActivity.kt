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

import android.os.Bundle
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
import com.worldwidewaves.activities.utils.TabManager
import com.worldwidewaves.activities.utils.setStatusBarColor
import com.worldwidewaves.compose.tabs.AboutScreen
import com.worldwidewaves.compose.tabs.EventsListScreen
import com.worldwidewaves.compose.tabs.SettingsScreen
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_EXT_TABBAR_HEIGHT
import com.worldwidewaves.shared.generated.resources.about_icon
import com.worldwidewaves.shared.generated.resources.about_icon_selected
import com.worldwidewaves.shared.generated.resources.settings_icon
import com.worldwidewaves.shared.generated.resources.settings_icon_selected
import com.worldwidewaves.shared.generated.resources.waves_icon
import com.worldwidewaves.shared.generated.resources.waves_icon_selected
import com.worldwidewaves.theme.AppTheme
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
        super.onCreate(savedInstanceState)

        setStatusBarColor(window)

        setContent {
            AppTheme {
                Surface(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                    tabManager.TabView()
                }
            }
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

