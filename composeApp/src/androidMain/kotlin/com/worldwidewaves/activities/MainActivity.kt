package com.worldwidewaves.activities

/*
 * Copyright 2024 DrWave
 *
 * WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and countries,
 * culminating in a global wave. The project aims to transcend physical and cultural boundaries, fostering unity,
 * community, and shared human experience by leveraging real-time coordination and location-based services.
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.worldwidewaves.compose.AboutScreen
import com.worldwidewaves.compose.EventsScreen
import com.worldwidewaves.compose.SettingsScreen
import com.worldwidewaves.shared.generated.resources.about_icon
import com.worldwidewaves.shared.generated.resources.about_icon_selected
import com.worldwidewaves.shared.generated.resources.settings_icon
import com.worldwidewaves.shared.generated.resources.settings_icon_selected
import com.worldwidewaves.shared.generated.resources.waves_icon
import com.worldwidewaves.shared.generated.resources.waves_icon_selected
import com.worldwidewaves.theme.AppTheme
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.android.ext.android.inject
import com.worldwidewaves.shared.generated.resources.Res as ShRes

// ----------------------------

private enum class Tab {
    Events, About, Settings,
    None
}

private val tabInfo = mapOf(
    Tab.Events to Pair(ShRes.drawable.waves_icon, ShRes.drawable.waves_icon_selected),
    Tab.About to Pair(ShRes.drawable.about_icon, ShRes.drawable.about_icon_selected),
    Tab.Settings to Pair(ShRes.drawable.settings_icon, ShRes.drawable.settings_icon_selected)
)

interface TabScreen {
    @Composable
    fun Screen(modifier: Modifier)
}

// ----------------------------

class MainActivity : AppCompatActivity() {

    // Inject dependencies
    private val eventsScreen: EventsScreen by inject()
    private val aboutScreen: AboutScreen by inject()
    private val settingsScreen: SettingsScreen by inject()

    // Initialize the map with the injected instances
    private val tabViews: Map<Tab, TabScreen> = mapOf(
        Tab.Events to eventsScreen as TabScreen,
        Tab.About to aboutScreen as TabScreen,
        Tab.Settings to settingsScreen as TabScreen
    )

    // ----------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.background(MaterialTheme.colorScheme.background)
                ) {
                 TabView(selectedTab = Tab.Events)
                }
            }
        }
    }

    // ----------------------------

    @Composable
    private fun TabView(modifier: Modifier = Modifier, selectedTab: Tab = Tab.None) {
        var currentTab by remember { mutableStateOf(selectedTab) }

        Column(modifier = Modifier.fillMaxHeight()) {

            // Display the selected tab screen
            tabViews[currentTab]?.Screen(modifier = Modifier
                .fillMaxSize()
                .weight(1f))

            // Tab bar
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .height(60.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                tabInfo.forEach { (tab, info) ->
                    val (icon, selectedIcon) = info
                    val iconResource = if (currentTab == tab) selectedIcon else icon
                    TabBarItem(
                        icon = iconResource,
                        onClick = {
                            currentTab = tab
                        }
                    )
                }
            }
        }
    }

    @Composable
    private fun TabBarItem(
        modifier: Modifier = Modifier,
        icon: DrawableResource,
        onClick: () -> Unit
    ) {
        Box(
            modifier = modifier
                .clickable(onClick = onClick)
        ) {
            Image(
                painter = painterResource(icon),
                contentDescription = null, // Accessibility description should be added based on the context
                modifier = Modifier.height(45.dp),
                contentScale = ContentScale.Fit
            )
        }
    }

}