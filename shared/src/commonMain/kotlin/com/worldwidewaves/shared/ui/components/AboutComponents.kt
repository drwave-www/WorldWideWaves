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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.worldwidewaves.shared.MokoRes
import com.worldwidewaves.shared.generated.resources.Res
import com.worldwidewaves.shared.generated.resources.www_logo_transparent
import dev.icerock.moko.resources.compose.stringResource
import org.jetbrains.compose.resources.painterResource

/**
 * Shared About-specific divider line with custom spacing.
 * Used throughout About screens for visual separation.
 */
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

/**
 * Shared WWW logo component used in About screens.
 * Displays the main WorldWideWaves logo with consistent sizing.
 */
@Composable
fun AboutWWWLogo() {
    Image(
        painter = painterResource(Res.drawable.www_logo_transparent),
        contentDescription = stringResource(MokoRes.strings.logo_description),
        modifier = Modifier
            .width(250.dp)
            .padding(top = 10.dp),
    )
    Spacer(modifier = Modifier.size(20.dp))
}

/**
 * Shared social networks component specific to About screens.
 * Uses the main WWW Instagram account and hashtag.
 */
@Composable
fun AboutWWWSocialNetworks(
    onUrlOpen: (String) -> Unit = { url ->
        com.worldwidewaves.shared.utils.Log.i("AboutWWWSocialNetworks", "URL click: $url")
    },
) {
    WWWSocialNetworks(
        instagramAccount = stringResource(MokoRes.strings.www_instagram),
        instagramHashtag = stringResource(MokoRes.strings.www_hashtag),
        onUrlOpen = onUrlOpen,
    )
}