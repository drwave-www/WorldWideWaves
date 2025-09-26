package com.worldwidewaves.shared.ui.components

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.worldwidewaves.shared.MokoRes
import com.worldwidewaves.shared.generated.resources.Res
import com.worldwidewaves.shared.generated.resources.background
import com.worldwidewaves.shared.generated.resources.www_logo_transparent
import dev.icerock.moko.resources.compose.stringResource
import org.jetbrains.compose.resources.painterResource

/**
 * Shared splash screen component.
 * Displays background and logo consistently across platforms.
 * Works identically on both Android and iOS platforms.
 */
@Composable
fun SharedSplashScreen(
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter = painterResource(Res.drawable.background),
            contentDescription = stringResource(MokoRes.strings.background_description),
            contentScale = ContentScale.FillHeight,
            modifier = Modifier.fillMaxSize(),
        )
        Image(
            painter = painterResource(Res.drawable.www_logo_transparent),
            contentDescription = stringResource(MokoRes.strings.logo_description),
            modifier = Modifier
                .size(300.dp) // Use size() instead of width() to maintain aspect ratio
                .align(Alignment.Center),
            contentScale = ContentScale.Fit, // Ensure proper scaling
        )
    }
}