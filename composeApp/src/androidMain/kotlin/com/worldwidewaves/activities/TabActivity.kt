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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.worldwidewaves.theme.AppTheme

// ----------------------------

interface TabScreen {
    @Composable
    fun Screen(modifier: Modifier)
    fun getName(): String?
}

// ----------------------------

abstract class TabActivity : AppCompatActivity() {

    private val screens: List<TabScreen> by lazy { this.getTabScreens() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.background(MaterialTheme.colorScheme.background)
                ) {
                 TabView(selectedTab = getDefaultSelectedTab())
                }
            }
        }
    }

    // ----------------------------

    @Composable
    protected fun TabView(modifier: Modifier = Modifier, selectedTab: Int) {
        var currentTab by remember { mutableIntStateOf(selectedTab) }

        Column(modifier = Modifier.fillMaxHeight()) {

            // Display the selected tab screen
            screens[currentTab].Screen(modifier = Modifier
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
                screens.forEachIndexed { index, tab ->
                    TabBarItem(
                        isSelected = currentTab == index,
                        tabIndex = index,
                        contentDescription = tab.getName(),
                        onClick = {
                            currentTab = index
                        }
                    )
                }
            }
        }
    }

    // ----------------------------

    @Composable
    abstract fun TabBarItem(
        isSelected: Boolean,
        tabIndex: Int,
        contentDescription: String?,
        onClick: () -> Unit
    )

    abstract fun getTabScreens(): List<TabScreen>
    abstract fun getDefaultSelectedTab(): Int

}