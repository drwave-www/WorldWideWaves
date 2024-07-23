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

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.worldwidewaves.shared.generated.resources.about_icon
import com.worldwidewaves.shared.generated.resources.about_icon_selected
import com.worldwidewaves.shared.generated.resources.settings_icon
import com.worldwidewaves.shared.generated.resources.settings_icon_selected
import com.worldwidewaves.shared.generated.resources.waves_icon
import com.worldwidewaves.shared.generated.resources.waves_icon_selected
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import com.worldwidewaves.shared.generated.resources.Res as ShRes

enum class Tab {
    Events,
    About,
    Settings,
    None
}

@Composable
fun TabBar(modifier: Modifier = Modifier, selectedTab: Tab = Tab.None) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(selectedTab) }

    val tabInfo = mapOf(
        Tab.Events to Triple(ShRes.drawable.waves_icon, ShRes.drawable.waves_icon_selected, EventsActivity::class.java),
        Tab.About to Triple(ShRes.drawable.about_icon, ShRes.drawable.about_icon_selected, AboutActivity::class.java),
        Tab.Settings to Triple(ShRes.drawable.settings_icon, ShRes.drawable.settings_icon_selected, SettingsActivity::class.java)
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .padding(start = 24.dp, end = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        tabInfo.forEach { (tab, info) ->
            val (icon, selectedIcon, activityClass) = info
            val iconResource = if (currentTab == tab) selectedIcon else icon
            TabBarItem(
                icon = iconResource,
                onClick = {
                    currentTab = tab
                    val intent = Intent(context, activityClass)
                    context.startActivity(intent)
                }
            )
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