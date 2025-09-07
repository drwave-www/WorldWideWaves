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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_INT_TABBAR_HEIGHT

// ----------------------------

/**
 * Lightweight contract implemented by every screen that can appear inside the
 * **tab bar**.
 *
 * A screen only needs to expose:
 * • `Screen()` – the actual Composable content  
 * • `name`     – label forwarded to the host so it can be used for a11y or
 *   analytics.
 */
interface TabScreen {
    @Composable fun Screen(modifier: Modifier)
    val name : String
}

// ----------------------------

/**
 * Simple tab navigator for Compose.
 *
 * Takes a list of [TabScreen]s and a Composable factory (`tabBarItem`) that
 * draws each item (icon/text).  It then:
 * 1. Renders the currently-selected screen in a flexible content area.  
 * 2. Displays a bottom tab-bar that lets the user switch screens.  
 * 3. Persists the selected tab via `remember` so configuration changes keep the
 *    current selection.
 */
class TabManager(
    private val screens: List<TabScreen>, // List of tab screens
    val tabBarItem: @Composable ( // How to draw a tab item
        isSelected: Boolean,
        tabIndex: Int,
        contentDescription: String?
    ) -> Unit
) {

    @Composable
    /**
     * Renders the tab bar and the associated content area.
     *
     * [startScreen] can temporarily override the content (e.g. splash or
     * onboarding) until the first tab selection occurs.
     */
    fun TabView(
        modifier: Modifier = Modifier,
        startScreen: @Composable ((Modifier) -> Unit)? = null,
        selectedTab: Int = 0
    ) {
        var currentTab by remember { mutableIntStateOf(selectedTab) }
        var originalScreen by remember { mutableStateOf(startScreen) }

        Column(modifier = Modifier.fillMaxHeight()) {

            // Display the selected tab screen
            Surface(modifier = Modifier.weight(1f).fillMaxSize()) {
                originalScreen?.invoke(Modifier) ?: screens[currentTab].Screen(Modifier)
            }

            // Tab bar
            Row(
                modifier = modifier.fillMaxWidth().height(DIM_INT_TABBAR_HEIGHT.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                screens.forEachIndexed { index, tab ->
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .align(Alignment.CenterVertically)
                            .clickable {
                        originalScreen = null
                        currentTab = index
                        },
                        contentAlignment = Alignment.Center
                    ) {
                        tabBarItem(currentTab == index, index, tab.name)
                    }
                }
            }
        }
    }

}