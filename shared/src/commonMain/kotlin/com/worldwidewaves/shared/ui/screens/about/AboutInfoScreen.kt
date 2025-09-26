package com.worldwidewaves.shared.ui.screens.about

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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.worldwidewaves.shared.MokoRes
import com.worldwidewaves.shared.WWWGlobals.Dimensions
import com.worldwidewaves.shared.WWWGlobals.Info
import com.worldwidewaves.shared.infos_core
import com.worldwidewaves.shared.ui.components.AboutDividerLine
import com.worldwidewaves.shared.ui.components.AboutWWWLogo
import com.worldwidewaves.shared.ui.components.AboutWWWSocialNetworks
import com.worldwidewaves.shared.ui.theme.sharedCommonTextStyle
import com.worldwidewaves.shared.ui.theme.sharedExtraBoldTextStyle
import dev.icerock.moko.resources.compose.stringResource

/**
 * Shared About > Info screen.
 *
 * Displays the core descriptive paragraphs about the WorldWideWaves project
 * along with the author signature and social-network links.
 * Content is fully localized via MokoRes.strings.
 * Works identically on both Android and iOS platforms.
 */
@Composable
fun SharedAboutInfoScreen(
    modifier: Modifier = Modifier,
    onUrlOpen: (String) -> Unit = { url ->
        com.worldwidewaves.shared.utils.Log.i("AboutInfoScreen", "URL click: $url")
    },
) {
    val state = rememberLazyListState()

    Box(modifier = modifier) {
        LazyColumn(
            state = state,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item { AboutWWWLogo() }
            item { MainInfo() }
            item { DrWaveSignature() }
            item { AboutDividerLine() }
            item { AboutWWWSocialNetworks(onUrlOpen = onUrlOpen) }
        }
    }
}

/**
 * Iterates over a fixed list of localized string resources and renders each
 * paragraph with justified alignment (or start-aligned for RTL locales).
 */
@Composable
private fun MainInfo() {
    val dir = LocalLayoutDirection.current

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp), // space between items
    ) {
        infos_core.forEach { res ->
            Text(
                text = stringResource(res),
                style = sharedCommonTextStyle(Info.TEXT_FONTSIZE).copy(
                    textAlign = if (dir == LayoutDirection.Rtl) TextAlign.Start else TextAlign.Justify,
                ),
            )
        }
    }
}

/**
 * Displays "Dr Wave" signature block with custom sizing and spacing.
 */
@Composable
private fun DrWaveSignature() {
    Spacer(modifier = Modifier.size(Dimensions.SPACER_BIG.dp))
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            text = stringResource(MokoRes.strings.drwave),
            style = sharedExtraBoldTextStyle(Info.DRWAVE_FONTSIZE),
        )
    }
    Spacer(modifier = Modifier.size(Dimensions.SPACER_SMALL.dp))
}