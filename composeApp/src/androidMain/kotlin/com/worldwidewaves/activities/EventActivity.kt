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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.worldwidewaves.compose.EventMap
import com.worldwidewaves.compose.EventOverlayDone
import com.worldwidewaves.compose.EventOverlaySoonOrRunning
import com.worldwidewaves.compose.WWWSocialNetworks
import com.worldwidewaves.shared.events.Position
import com.worldwidewaves.shared.events.WWWEvent
import com.worldwidewaves.shared.events.getLocationImage
import com.worldwidewaves.shared.events.getStartDateSimpleAsLocal
import com.worldwidewaves.shared.events.isDone
import com.worldwidewaves.shared.events.isRunning
import com.worldwidewaves.shared.generated.resources.be_waved
import com.worldwidewaves.shared.generated.resources.geoloc_undone
import com.worldwidewaves.shared.generated.resources.geoloc_yourein
import com.worldwidewaves.shared.generated.resources.geoloc_yourenotin
import com.worldwidewaves.shared.generated.resources.wave_end_time
import com.worldwidewaves.shared.generated.resources.wave_now
import com.worldwidewaves.shared.generated.resources.wave_progression
import com.worldwidewaves.shared.generated.resources.wave_speed
import com.worldwidewaves.shared.generated.resources.wave_start_time
import com.worldwidewaves.shared.generated.resources.wave_total_time
import com.worldwidewaves.theme.displayFontFamily
import com.worldwidewaves.theme.extraFontFamily
import com.worldwidewaves.theme.quinaryLight
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.maplibre.android.geometry.LatLng
import com.worldwidewaves.shared.generated.resources.Res as ShRes

class EventActivity : AbstractEventBackActivity() {

    @Composable
    override fun Screen(modifier: Modifier, event: WWWEvent) {
        val eventDate = event.getStartDateSimpleAsLocal()
        val coroutineScope = rememberCoroutineScope()
        var geolocText by remember { mutableStateOf(ShRes.string.geoloc_undone) }
        var lastKnownLocation by remember { mutableStateOf<LatLng?>(null) }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(30.dp)
        ) {
            EventOverlay(event, eventDate)
            EventDescription(event)
            DividerLine()
            ButtonWave(event)
            EventMap(event, onLocationUpdate = { newLocation ->
                updateGeolocText(event,
                    newLocation, lastKnownLocation,
                    coroutineScope
                ) { geolocText = it }
                lastKnownLocation = newLocation
            }).Screen(
                modifier = Modifier.fillMaxWidth()
            )
            GeolocalizeMe(geolocText)
            EventNumbers(event)
            WWWEventSocialNetworks(event)
        }
    }
}

private fun updateGeolocText(
    event: WWWEvent,
    newLocation: LatLng,
    lastKnownLocation: LatLng?,
    coroutineScope: CoroutineScope,
    onGeolocTextUpdated: (StringResource) -> Unit
) {
    if (lastKnownLocation == null || lastKnownLocation != newLocation) {
        coroutineScope.launch {
            val newGeolocText = if (
                event.area.isPositionWithin(
                    Position(
                        newLocation.latitude,
                        newLocation.longitude
                    )
                )
            ) {
                ShRes.string.geoloc_yourein
            } else {
                ShRes.string.geoloc_yourenotin
            }
            onGeolocTextUpdated(newGeolocText)
        }
    }
}

// ----------------------------

@Composable
private fun EventDescription(event: WWWEvent, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier.padding(horizontal = 20.dp),
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
}

// ----------------------------

@Composable
private fun ButtonWave(event: WWWEvent) {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .width(300.dp)
            .height(40.dp)
            .clickable(onClick = {
                /* TODO: click on Wave button */
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
        instagramAccount = event.instagramAccount,
        instagramUrl = event.instagramUrl,
        instagramHashtag = event.instagramHashtag
    )
}

// ----------------------------

@Composable
private fun GeolocalizeMe(geolocText: StringResource) {
    Row(
        modifier = Modifier
            .height(45.dp)
            .padding(start = 20.dp, end = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) { // TODO: change colors depending on in/out area changes
        // check https://medium.com/nerd-for-tech/jetpack-compose-pulsating-effect-4b9f2928d31a
        Box(
            modifier = Modifier
                .border(2.dp, MaterialTheme.colorScheme.primary)
                .fillMaxHeight()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(geolocText),
                color = quinaryLight,
                fontSize = 14.sp,
                fontFamily = MaterialTheme.typography.bodyMedium.fontFamily
            )
        }
    }
}

// ----------------------------

@Composable
private fun EventNumbers(event: WWWEvent) {
    val eventNumbers = remember { mutableStateMapOf<StringResource, String>() }
    val coroutineScope = rememberCoroutineScope()

    // Retrieve wave numbers and frequently update progession
    LaunchedEffect(event) {
        var lastProgressionValue = ""
        coroutineScope.launch {
            val waveNumbers = event.wave.getAllNumbers()
            eventNumbers.clear()
            eventNumbers.putAll(mapOf(
                    ShRes.string.wave_speed to waveNumbers.waveSpeed,
                    ShRes.string.wave_start_time to waveNumbers.waveStartTime,
                    ShRes.string.wave_end_time to waveNumbers.waveEndTime,
                    ShRes.string.wave_total_time to waveNumbers.waveTotalTime,
                    ShRes.string.wave_progression to waveNumbers.waveProgression
            ))
            while (event.isRunning()) {
                delay(10000) // 10s : TODO: static number
                val newProgressionValue = event.wave.getLiteralProgression()
                if (newProgressionValue != lastProgressionValue) {
                    eventNumbers[ShRes.string.wave_progression] = newProgressionValue
                    lastProgressionValue = newProgressionValue
                }
            }
        }
    }

    Box(modifier = Modifier.padding(start = 20.dp, end = 20.dp)) {
        Box(
            modifier = Modifier
                .border(
                    width = 2.dp,
                    color = quinaryLight,
                    shape = RoundedCornerShape(topStart = 50.dp, bottomEnd = 50.dp)
                )
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.padding(start = 10.dp, end = 10.dp)) {
                Text(
                    text = stringResource(ShRes.string.be_waved),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right,
                    color = quinaryLight,
                    fontFamily = extraFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                eventNumbers.forEach { (key, value) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(key),
                            color = quinaryLight,
                            fontFamily = extraFontFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = value,
                            color = when (key) {
                                ShRes.string.wave_progression -> MaterialTheme.colorScheme.secondary
                                ShRes.string.wave_start_time -> Color.Yellow
                                else -> MaterialTheme.colorScheme.primary
                            },
                            fontFamily = extraFontFamily,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}
