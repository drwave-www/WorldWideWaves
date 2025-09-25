package com.worldwidewaves.compose.tabs

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

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.worldwidewaves.shared.ui.TabManager
import com.worldwidewaves.shared.ui.TabScreen
import com.worldwidewaves.shared.ui.components.WWWSocialNetworks
import com.worldwidewaves.compose.tabs.about.AboutFaqScreen
import com.worldwidewaves.compose.tabs.about.AboutInfoScreen
import com.worldwidewaves.shared.MokoRes
import com.worldwidewaves.shared.WWWGlobals.Dimensions
import com.worldwidewaves.shared.WWWGlobals.TabBar
import com.worldwidewaves.shared.generated.resources.Res
import com.worldwidewaves.shared.generated.resources.www_logo_transparent
import com.worldwidewaves.theme.commonTextStyle
import dev.icerock.moko.resources.compose.stringResource
import org.jetbrains.compose.resources.painterResource

private val tabInfo =
    listOf(
        MokoRes.strings.tab_infos_name,
        MokoRes.strings.tab_faq_name,
    )

/**
 * *About* root tab that aggregates the **Info** and **FAQ** sub-sections.
 *
 * Uses an internal [TabManager] to switch between the two supplied [TabScreen]s
 * (`aboutInfoScreen`, `aboutFaqScreen`) and provides its own tiny tab-bar
 * implementation via [TabBarItem].
 */
class AboutScreen(
    aboutInfoScreen: AboutInfoScreen,
    aboutFaqScreen: AboutFaqScreen,
) : TabScreen {
    override val name = "Info"

    private val tabManager =
        TabManager(
            listOf(
                aboutInfoScreen,
                aboutFaqScreen,
            ),
        ) { isSelected, tabIndex, _ -> TabBarItem(isSelected, tabIndex) }

    // ----------------------------

    @Composable
    override fun Screen(modifier: Modifier) {
        Surface(modifier = modifier.padding(Dimensions.DEFAULT_EXT_PADDING.dp)) {
            tabManager.TabView()
        }
    }

    // ----------------------------

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
                            .padding(start = Dimensions.DEFAULT_INT_PADDING.dp, end = Dimensions.DEFAULT_INT_PADDING.dp)
                            .offset(y = (-Dimensions.DEFAULT_EXT_PADDING).dp),
                    color = Color.White,
                    thickness = 2.dp,
                )
            }
            Text(
                text = stringResource(tabInfo[tabIndex]),
                style =
                    commonTextStyle(TabBar.INT_ITEM_FONTSIZE).copy(
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal,
                    ),
            )
        }
    }
}

// ------------- Common composables for About Screens ---------------

@Composable
fun AboutDividerLine() {
    Spacer(modifier = Modifier.size(30.dp))
    HorizontalDivider(
        modifier = Modifier.width(200.dp),
        color = Color.White,
        thickness = 2.dp,
    )
    Spacer(modifier = Modifier.size(30.dp))
}

@Composable
fun AboutWWWSocialNetworks() {
    WWWSocialNetworks(
        instagramAccount = stringResource(MokoRes.strings.www_instagram),
        instagramHashtag = stringResource(MokoRes.strings.www_hashtag),
    )
}

@Composable
fun AboutWWWLogo() {
    Image(
        painter = painterResource(Res.drawable.www_logo_transparent),
        contentDescription = stringResource(MokoRes.strings.logo_description),
        modifier =
            Modifier
                .width(250.dp)
                .padding(top = 10.dp),
    )
    Spacer(modifier = Modifier.size(20.dp))
}
