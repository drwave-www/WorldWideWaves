package com.worldwidewaves.shared.ui.components.navigation

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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.worldwidewaves.shared.MokoRes
import com.worldwidewaves.shared.WWWGlobals.BackNav
import com.worldwidewaves.shared.ui.theme.sharedPrimaryColoredTextStyle
import dev.icerock.moko.resources.compose.stringResource

/**
 * Shared backward navigation screen component.
 * Displays a back button with proper Android/iOS styling.
 * Works identically on both platforms.
 */
@Composable
fun BackwardScreen(
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Column(modifier = modifier.fillMaxSize()) {
        // Back layer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = BackNav.PADDING[0].dp,
                    end = BackNav.PADDING[1].dp,
                    top = BackNav.PADDING[2].dp,
                    bottom = BackNav.PADDING[3].dp,
                ),
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .clickable { onBackClick() },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "←",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 4.dp),
                    )
                    Text(
                        text = stringResource(MokoRes.strings.back),
                        style = sharedPrimaryColoredTextStyle(BackNav.FONTSIZE),
                    )
                }
            }
        }
    }
}