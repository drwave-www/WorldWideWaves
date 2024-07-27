package com.worldwidewaves.activities

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

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.worldwidewaves.compose.EventOverlayDone
import com.worldwidewaves.compose.EventOverlaySoonOrRunning
import com.worldwidewaves.compose.WWWSocialNetworks
import com.worldwidewaves.shared.events.WWWEvent
import com.worldwidewaves.shared.events.getFormattedSimpleDate
import com.worldwidewaves.shared.events.getLocationImage
import com.worldwidewaves.shared.events.isDone
import com.worldwidewaves.shared.generated.resources.wave_now
import com.worldwidewaves.theme.displayFontFamily
import com.worldwidewaves.theme.extraFontFamily
import com.worldwidewaves.theme.quinaryLight
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.worldwidewaves.shared.generated.resources.Res as ShRes


class EventActivity : AbstractEventBackActivity() {

    @Composable
    override fun Screen(modifier: Modifier, event: WWWEvent) {
        val eventDate = event.getFormattedSimpleDate()

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            EventOverlay(event, eventDate)
            EventDescription(event)
            DividerLine()
            ButtonWave(event)
            WWWEventSocialNetworks(event)
        }
    }

}

// ----------------------------

@Composable
private fun EventDescription(event: WWWEvent, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier.padding(20.dp),
        text = event.description,
        fontFamily = extraFontFamily,
        color = quinaryLight,
        fontSize = 16.sp,
        textAlign = TextAlign.Justify,
        fontWeight = FontWeight.Bold
    )
}

// ----------------------------

@Composable
private fun EventOverlay(
    event: WWWEvent,
    eventDate: String
) {
    Box {
        Image(
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.FillWidth,
            painter = painterResource(event.getLocationImage() as DrawableResource),
            contentDescription = event.location
        )
        Box(modifier = Modifier.matchParentSize()) {
            EventOverlaySoonOrRunning(event)
            EventOverlayDate(event, eventDate)
            EventOverlayDone(event)
        }
    }
}

// ----------------------------

@Composable
private fun EventOverlayDate(event: WWWEvent, eventDate: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .let { if (event.isDone()) it.padding(bottom = 20.dp) else it },
        contentAlignment = if (event.isDone()) Alignment.BottomCenter else Alignment.Center
    ) {
        val textStyle = TextStyle(
            fontFamily = extraFontFamily,
            fontSize = 90.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            text = eventDate,
            style = textStyle.copy(color = quinaryLight)
        )
        Text(
            text = eventDate,
            style = textStyle.copy(
                color = MaterialTheme.colorScheme.primary,
                drawStyle = Stroke(
                    miter = 20f,
                    width = 5f,
                    join = StrokeJoin.Miter
                )
            )
        )
    }
}

// ----------------------------

@Composable
fun DividerLine() {
    HorizontalDivider(
        modifier = Modifier.width(200.dp),
        color = Color.White, thickness = 2.dp
    )
    Spacer(modifier = Modifier.size(20.dp))
}

// ----------------------------

@Composable
private fun ButtonWave(event: WWWEvent) {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .width(300.dp).height(40.dp)
            .clickable(onClick = {
                /* TODO */
            })
    ) {
        Text(
            modifier = Modifier
                .fillMaxSize()
                .wrapContentHeight(align = Alignment.CenterVertically),
            text = stringResource(ShRes.string.wave_now).uppercase(),
            color = quinaryLight,
            fontSize = 24.sp,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Black,
            fontFamily = displayFontFamily
        )
    }
}

// ----------------------------

@Composable
private fun WWWEventSocialNetworks(event: WWWEvent) {
    WWWSocialNetworks(
        modifier = Modifier.padding(top = 20.dp, bottom = 20.dp),
        instagramAccount = event.instagramAccount,
        instagramUrl = event.instagramUrl,
        instagramHashtag = event.instagramHashtag
    )
}