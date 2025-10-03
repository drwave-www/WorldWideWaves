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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.worldwidewaves.shared.MokoRes
import com.worldwidewaves.shared.WWWGlobals.Common
import com.worldwidewaves.shared.WWWGlobals.Dimensions
import com.worldwidewaves.shared.generated.resources.Res
import com.worldwidewaves.shared.generated.resources.instagram_icon
import com.worldwidewaves.shared.ui.theme.sharedCommonBoldStyle
import com.worldwidewaves.shared.ui.theme.sharedCommonTextStyle
import com.worldwidewaves.shared.ui.utils.focusIndicator
import com.worldwidewaves.shared.utils.Log
import dev.icerock.moko.resources.compose.stringResource
import org.jetbrains.compose.resources.painterResource

/**
 * Shared social networks component displaying clickable Instagram account & hashtag with logo.
 * Uses callback-based URL opening for platform flexibility.
 * Works identically on both Android and iOS platforms.
 */
@Composable
fun WWWSocialNetworks(
    modifier: Modifier = Modifier,
    instagramAccount: String,
    instagramHashtag: String,
    onUrlOpen: (String) -> Unit = { url ->
        Log.i("WWWSocialNetworks", "URL click: $url")
    },
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(Res.drawable.instagram_icon),
            contentDescription = stringResource(MokoRes.strings.instagram_logo_description),
            modifier = Modifier.width(Common.SOCIALNETWORKS_INSTAGRAM_LOGO_WIDTH.dp),
        )
        Column(
            modifier = Modifier.padding(start = 10.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                modifier =
                    Modifier
                        .focusIndicator()
                        .clickable(onClick = {
                            try {
                                val uri = "https://www.instagram.com/${instagramAccount.removePrefix("@")}"
                                onUrlOpen(uri)
                            } catch (e: Exception) {
                                Log.e("WWWSocialNetworks", "Error opening Instagram URI", throwable = e)
                            }
                        }),
                text = instagramAccount,
                style =
                    sharedCommonBoldStyle(Common.SOCIALNETWORKS_ACCOUNT_FONTSIZE).copy(
                        textDecoration = TextDecoration.Underline,
                    ),
            )
            Text(
                text = instagramHashtag,
                style = sharedCommonTextStyle(Common.SOCIALNETWORKS_HASHTAG_FONTSIZE),
            )
        }
    }
    Spacer(modifier = Modifier.size(Dimensions.SPACER_MEDIUM.dp))
}
