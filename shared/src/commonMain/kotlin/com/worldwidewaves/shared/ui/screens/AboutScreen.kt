package com.worldwidewaves.shared.ui.screens

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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.worldwidewaves.shared.MokoRes
import com.worldwidewaves.shared.PlatformEnabler
import com.worldwidewaves.shared.WWWGlobals.Dimensions
import com.worldwidewaves.shared.WWWGlobals.TabBar
import com.worldwidewaves.shared.WWWPlatform
import com.worldwidewaves.shared.ui.TabManager
import com.worldwidewaves.shared.ui.TabScreen
import com.worldwidewaves.shared.ui.screens.about.AboutFaqScreen
import com.worldwidewaves.shared.ui.screens.about.AboutInfoScreen
import com.worldwidewaves.shared.ui.theme.sharedCommonTextStyle
import com.worldwidewaves.shared.utils.Log
import dev.icerock.moko.resources.compose.stringResource

private val tabInfo =
    listOf(
        MokoRes.strings.tab_infos_name,
        MokoRes.strings.tab_faq_name,
    )

/**
 * Shared About root screen that aggregates the Info and FAQ sub-sections.
 *
 * Uses an internal TabManager to switch between the two sub-screens
 * and provides a tiny tab-bar implementation.
 * Works identically on both Android and iOS platforms.
 */
@Composable
fun AboutScreen(
    platform: WWWPlatform,
    platformEnabler: PlatformEnabler,
    modifier: Modifier = Modifier,
    onUrlOpen: (String) -> Unit = { url ->
        Log.i("AboutScreen", "URL click: $url")
    },
) {
    // Create tab manager with shared sub-screens
    val tabManager =
        TabManager(
            platformEnabler,
            screens =
                listOf(
                    object : TabScreen {
                        override val name = "Infos"

                        @Composable
                        override fun Screen(
                            platformEnabler: PlatformEnabler,
                            modifier: Modifier,
                        ) {
                            AboutInfoScreen(modifier = modifier, onUrlOpen = onUrlOpen)
                        }
                    },
                    object : TabScreen {
                        override val name = "FAQ"

                        @Composable
                        override fun Screen(
                            platformEnabler: PlatformEnabler,
                            modifier: Modifier,
                        ) {
                            AboutFaqScreen(
                                platform = platform,
                                modifier = modifier,
                                onUrlOpen = onUrlOpen,
                                onSimulateClick = {
                                    platform.enableSimulationMode()
                                },
                            )
                        }
                    },
                ),
            tabBarItem = { isSelected, tabIndex, _ ->
                TabBarItem(isSelected = isSelected, tabIndex = tabIndex)
            },
        )

    Surface(modifier = modifier.padding(Dimensions.DEFAULT_EXT_PADDING.dp)) {
        tabManager.TabView()
    }
}

@Composable
private fun TabBarItem(
    isSelected: Boolean,
    tabIndex: Int,
) {
    Box(
        modifier =
            Modifier
                .height(TabBar.INT_HEIGHT.dp)
                .width(TabBar.INT_ITEM_WIDTH.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) { // Draw a line on top of the selected tab
            HorizontalDivider(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            start = Dimensions.DEFAULT_INT_PADDING.dp,
                            end = Dimensions.DEFAULT_INT_PADDING.dp,
                        ).offset(y = (-Dimensions.DEFAULT_EXT_PADDING).dp),
                color = Color.White,
                thickness = 2.dp,
            )
        }
        Text(
            text = stringResource(tabInfo[tabIndex]),
            style =
                sharedCommonTextStyle(TabBar.INT_ITEM_FONTSIZE).copy(
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal,
                ),
        )
    }
}
