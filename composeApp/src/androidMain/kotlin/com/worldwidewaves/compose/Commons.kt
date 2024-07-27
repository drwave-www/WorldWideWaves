package com.worldwidewaves.compose

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.worldwidewaves.shared.events.WWWEvent
import com.worldwidewaves.shared.events.isDone
import com.worldwidewaves.shared.events.isRunning
import com.worldwidewaves.shared.events.isSoon
import com.worldwidewaves.shared.generated.resources.Res
import com.worldwidewaves.shared.generated.resources.event_done
import com.worldwidewaves.shared.generated.resources.event_running
import com.worldwidewaves.shared.generated.resources.event_soon
import com.worldwidewaves.shared.generated.resources.instagram_icon
import com.worldwidewaves.theme.displayFontFamily
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

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

@Composable
fun EventOverlaySoonOrRunning(event: WWWEvent, modifier: Modifier = Modifier) {
    if (event.isSoon() || event.isRunning()) {
        val backgroundColor =
            if (event.isSoon()) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary
        val textId = if (event.isSoon()) Res.string.event_soon else Res.string.event_running

        Box(
            modifier = modifier
                .fillMaxWidth()
                .offset(y = (-5).dp),
            contentAlignment = Alignment.TopEnd
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 15.dp, end = 15.dp)
                    .size(width = 115.dp, height = 26.dp)
                    .background(backgroundColor)
                    .padding(end = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(textId),
                    style = TextStyle(
                        fontFamily = MaterialTheme.typography.bodyMedium.fontFamily,
                        fontSize = 16.sp
                    )
                )
            }
        }
    }
}

// ----------------------------

@Composable
fun EventOverlayDone(event: WWWEvent, modifier: Modifier = Modifier) {
    if (event.isDone()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Surface(
                color = Color.run { White.copy(alpha = 0.5f) },
                modifier = Modifier.fillMaxSize()
            ) { }
            Image(
                painter = painterResource(Res.drawable.event_done),
                contentDescription = stringResource(Res.string.event_done),
                modifier = Modifier.width(130.dp),
            )
        }
    }
}

// ----------------------------

@Composable
fun WWWSocialNetworks(
    modifier: Modifier = Modifier,
    instagramAccount: String,
    instagramUrl: String,
    instagramHashtag: String
) {
    val uriHandler = LocalUriHandler.current

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(Res.drawable.instagram_icon),
            contentDescription = "Instagram logo",
            modifier = Modifier.width(90.dp)
        )
        Column(
            modifier = Modifier.padding(start = 10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                modifier = Modifier.clickable(onClick = {
                    try {
                        uriHandler.openUri(instagramUrl)
                    } catch (e: Exception) {
                        Log.e("AboutWWWSocialNetworks", "Failed to open URI", e)
                    }
                }),
                text = instagramAccount,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                fontFamily = displayFontFamily,
                textDecoration = TextDecoration.Underline
            )
            Text(
                text = instagramHashtag,
                fontSize = 16.sp,
                fontFamily = displayFontFamily
            )
        }
    }
    Spacer(modifier = Modifier.size(50.dp))
}