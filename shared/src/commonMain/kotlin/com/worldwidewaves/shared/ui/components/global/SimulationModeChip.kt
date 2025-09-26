package com.worldwidewaves.shared.ui.components.global

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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.worldwidewaves.shared.MokoRes
import com.worldwidewaves.shared.WWWPlatform
import dev.icerock.moko.resources.compose.stringResource

/**
 * Shared simulation mode chip overlay that displays when simulation mode is enabled.
 * Works identically on both Android and iOS platforms.
 *
 * This chip appears when simulation mode is enabled and provides a way to disable it.
 * Uses platform-specific icons through expect/actual pattern.
 *
 * @param platform The WWWPlatform instance to observe simulation mode state and handle actions
 */
@Composable
fun BoxScope.SimulationModeChip(platform: WWWPlatform) {
    val simulationModeEnabled by platform.simulationModeEnabled.collectAsState()

    if (simulationModeEnabled) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Red)
                    .clickable {
                        platform.disableSimulation()
                        platform.disableSimulationMode()
                    }.padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(
                text = stringResource(MokoRes.strings.simulation_mode),
                color = Color.White,
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = "⏹",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}
