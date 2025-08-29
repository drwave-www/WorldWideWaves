package com.worldwidewaves.compose.tabs.about

/*
 * Copyright 2025 DrWave
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
import androidx.compose.ui.unit.dp
import com.worldwidewaves.activities.utils.TabScreen
import com.worldwidewaves.compose.tabs.AboutDividerLine
import com.worldwidewaves.compose.tabs.AboutWWWLogo
import com.worldwidewaves.compose.tabs.AboutWWWSocialNetworks
import com.worldwidewaves.shared.MokoRes
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_DEFAULT_SPACER_BIG
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_DEFAULT_SPACER_SMALL
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_INFO_DRWAVE_FONTSIZE
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_INFO_TEXT_FONTSIZE
import com.worldwidewaves.theme.commonJustifiedTextStyle
import com.worldwidewaves.theme.extraBoldTextStyle
import dev.icerock.moko.resources.compose.stringResource

class AboutInfoScreen : TabScreen {
    override val name = "Infos"

    @Composable
    override fun Screen(modifier: Modifier) {
        val state = rememberLazyListState()

        Box(modifier = modifier) {
            LazyColumn(
                state = state,
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item { AboutWWWLogo() }
                item { MainInfo() }
                item { DrWaveSignature() }
                item { AboutDividerLine() }
                item { AboutWWWSocialNetworks() }
            }
        }
    }

    // ----------------------------

    @Composable
    private fun MainInfo() {
        val items = listOf(
            MokoRes.strings.infos_core_1,
            MokoRes.strings.infos_core_2,
            MokoRes.strings.infos_core_3,
            MokoRes.strings.infos_core_4,
            MokoRes.strings.infos_core_5,
            MokoRes.strings.infos_core_6,
            MokoRes.strings.infos_core_7,
            MokoRes.strings.infos_core_8,
            MokoRes.strings.infos_core_9
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp) // space between items
        ) {
            items.forEach { res ->
                Text(
                    text = stringResource(res),
                    style = commonJustifiedTextStyle(DIM_INFO_TEXT_FONTSIZE)
                )
            }
        }
    }


    // ----------------------------

    @Composable
    private fun DrWaveSignature() {
        Spacer(modifier = Modifier.size(DIM_DEFAULT_SPACER_BIG.dp))
        Column(horizontalAlignment = Alignment.Start) {
            Text(
                text = stringResource(MokoRes.strings.drwave),
                style = extraBoldTextStyle(DIM_INFO_DRWAVE_FONTSIZE)
            )
        }
        Spacer(modifier = Modifier.size(DIM_DEFAULT_SPACER_SMALL.dp))
    }

}
