package com.worldwidewaves.compose

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
import com.worldwidewaves.activities.TabManager
import com.worldwidewaves.activities.TabScreen

private val tabInfo = listOf(
    "Infos", "FAQ"
)

class AboutScreen(aboutInfoScreen: AboutInfoScreen, aboutFaqScreen: AboutFaqScreen) : TabScreen {

    private val tabManager = TabManager(listOf(
        aboutInfoScreen,
        aboutFaqScreen
    )) { isSelected, tabIndex, _ ->
        TabBarItem(isSelected, tabIndex)
    }

    // ----------------------------

    override fun getName(): String = "Info"

    // ----------------------------

    @Composable
    override fun Screen(modifier: Modifier) {
        Surface(modifier = modifier) {
            tabManager.TabView()
        }
    }

    // ----------------------------

    @Composable
    private fun TabBarItem(
        isSelected: Boolean,
        tabIndex: Int
    ) {
        Box(
            modifier = Modifier
                .height(60.dp)
                .width(150.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) { // Draw a line on top of the selected tab
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 10.dp, end = 10.dp)
                        .offset(y = (-20).dp),
                    color = Color.White, thickness = 2.dp
                )
            }
            Text(
                text = tabInfo[tabIndex].uppercase(),
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }

}
