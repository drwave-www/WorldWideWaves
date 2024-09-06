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

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.worldwidewaves.compose.ButtonWave
import com.worldwidewaves.compose.EventMap
import com.worldwidewaves.compose.EventOverlayDone
import com.worldwidewaves.compose.EventOverlaySoonOrRunning
import com.worldwidewaves.compose.WWWSocialNetworks
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_DEFAULT_EXT_PADDING
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_DEFAULT_INT_PADDING
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_DIVIDER_THICKNESS
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_DIVIDER_WIDTH
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_EVENT_DATE_FONTSIZE
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_EVENT_DATE_MITER
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_EVENT_DATE_STROKE
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_EVENT_DESC_FONTSIZE
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_EVENT_GEOLOCME_BORDER
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_EVENT_GEOLOCME_FONTSIZE
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_EVENT_GEOLOCME_HEIGHT
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_EVENT_MAP_RATIO
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_EVENT_NUMBERS_BORDERROUND
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_EVENT_NUMBERS_BORDERWIDTH
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_EVENT_NUMBERS_LABEL_FONTSIZE
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_EVENT_NUMBERS_SPACER
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_EVENT_NUMBERS_TITLE_FONTSIZE
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_EVENT_NUMBERS_TZ_FONTSIZE
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_EVENT_NUMBERS_VALUE_FONTSIZE
import com.worldwidewaves.shared.events.WWWEvent
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.generated.resources.be_waved
import com.worldwidewaves.shared.generated.resources.geoloc_undone
import com.worldwidewaves.shared.generated.resources.geoloc_warm_in
import com.worldwidewaves.shared.generated.resources.geoloc_yourein
import com.worldwidewaves.shared.generated.resources.geoloc_yourenotin
import com.worldwidewaves.shared.generated.resources.wave_end_time
import com.worldwidewaves.shared.generated.resources.wave_progression
import com.worldwidewaves.shared.generated.resources.wave_speed
import com.worldwidewaves.shared.generated.resources.wave_start_time
import com.worldwidewaves.shared.generated.resources.wave_total_time
import com.worldwidewaves.theme.extraBoldTextStyle
import com.worldwidewaves.theme.extraLightTextStyle
import com.worldwidewaves.theme.extraQuinaryColoredBoldTextStyle
import com.worldwidewaves.theme.quinaryColoredTextStyle
import com.worldwidewaves.theme.quinaryLight
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.maplibre.android.geometry.LatLng
import com.worldwidewaves.shared.generated.resources.Res as ShRes

class EventActivity : AbstractEventBackActivity() {

    @Composable
    override fun Screen(modifier: Modifier, event: WWWEvent) {
        val context = LocalContext.current
        val eventDate = event.getLiteralStartDateSimple()
        val coroutineScope = rememberCoroutineScope()
        var geolocText by remember { mutableStateOf(ShRes.string.geoloc_undone) }
        var lastKnownLocation by remember { mutableStateOf<LatLng?>(null) }


        // Calculate height based on aspect ratio and available width
        val configuration = LocalConfiguration.current
        val calculatedHeight = configuration.screenWidthDp.dp / DIM_EVENT_MAP_RATIO

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(30.dp)
        ) {
            EventOverlay(event, eventDate)
            EventDescription(event)
            DividerLine()
            ButtonWave(event)
            EventMap(event,
                onLocationUpdate = { newLocation ->
                    updateGeolocText(event,
                        newLocation, lastKnownLocation,
                        coroutineScope
                    ) { geolocText = it }
                    lastKnownLocation = newLocation
                },
                onMapClick = { _, _ ->
                    context.startActivity(Intent(context, EventFullMapActivity::class.java).apply {
                        putExtra("eventId", event.id)
                    })
                }
            ).Screen(modifier = Modifier.fillMaxWidth().height(calculatedHeight))
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
            val currentPosition = Position(newLocation.latitude, newLocation.longitude)
            val newGeolocText = when {
                event.area.isPositionWithinWarming(currentPosition) -> ShRes.string.geoloc_warm_in
                event.area.isPositionWithin(currentPosition) -> ShRes.string.geoloc_yourein
                else -> ShRes.string.geoloc_yourenotin
            }
            onGeolocTextUpdated(newGeolocText)
        }
    }
}

// ----------------------------

@Composable
private fun EventDescription(event: WWWEvent, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier.padding(horizontal = DIM_DEFAULT_EXT_PADDING.dp),
        text = event.description,
        style = extraQuinaryColoredBoldTextStyle(),
        fontSize = DIM_EVENT_DESC_FONTSIZE.sp,
        textAlign = TextAlign.Justify
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
            .let { if (event.isDone()) it.padding(bottom = DIM_DEFAULT_EXT_PADDING.dp) else it },
        contentAlignment = if (event.isDone()) Alignment.BottomCenter else Alignment.Center
    ) {
        val textStyle = extraBoldTextStyle(DIM_EVENT_DATE_FONTSIZE)
        Text(
            text = eventDate,
            style = textStyle.copy(color = quinaryLight)
        )
        Text(
            text = eventDate,
            style = textStyle.copy(
                color = MaterialTheme.colorScheme.primary,
                drawStyle = Stroke(
                    miter = DIM_EVENT_DATE_MITER,
                    width = DIM_EVENT_DATE_STROKE,
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
        modifier = Modifier.width(DIM_DIVIDER_WIDTH.dp),
        color = Color.White, thickness = DIM_DIVIDER_THICKNESS.dp
    )
}

// ----------------------------

@Composable
private fun WWWEventSocialNetworks(event: WWWEvent) {
    WWWSocialNetworks(
        instagramAccount = event.instagramAccount,
        instagramHashtag = event.instagramHashtag
    )
}

// ----------------------------

@Composable
private fun GeolocalizeMe(geolocText: StringResource) {
    Row(
        modifier = Modifier
            .height(DIM_EVENT_GEOLOCME_HEIGHT.dp)
            .padding(start = DIM_DEFAULT_EXT_PADDING.dp, end = DIM_DEFAULT_EXT_PADDING.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .border(DIM_EVENT_GEOLOCME_BORDER.dp, MaterialTheme.colorScheme.primary)
                .fillMaxHeight()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(geolocText),
                style = quinaryColoredTextStyle(DIM_EVENT_GEOLOCME_FONTSIZE)
            )
        }
    }
}

// ----------------------------

@Composable
private fun EventNumbers(event: WWWEvent) {
    val eventNumbers = remember { mutableStateMapOf<StringResource, String>() }
    val eventTimeZone = remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    // Retrieve wave numbers and frequently update progession
    LaunchedEffect(eventNumbers) {
        coroutineScope.launch {
            val waveNumbers = withContext(Dispatchers.IO) { event.wave.getAllNumbers() }
            eventNumbers.clear()
            eventNumbers.putAll(mapOf(
                    ShRes.string.wave_speed to waveNumbers.waveSpeed,
                    ShRes.string.wave_start_time to waveNumbers.waveStartTime,
                    ShRes.string.wave_end_time to waveNumbers.waveEndTime,
                    ShRes.string.wave_total_time to waveNumbers.waveTotalTime,
                    ShRes.string.wave_progression to waveNumbers.waveProgression
            ))
            eventTimeZone.value = waveNumbers.waveTimezone

            // Listen for progression changes
            event.wave.addOnWaveProgressionChangedListener { _ ->
                coroutineScope.launch {
                    eventNumbers[ShRes.string.wave_progression] = event.wave.getLiteralProgression()
                }
            }
        }
    }

    Box(modifier = Modifier.padding(start = DIM_DEFAULT_EXT_PADDING.dp, end = DIM_DEFAULT_EXT_PADDING.dp)) {
        Box(
            modifier = Modifier
                .border(
                    width = DIM_EVENT_NUMBERS_BORDERWIDTH.dp,
                    color = quinaryLight,
                    shape = RoundedCornerShape(
                        topStart = DIM_EVENT_NUMBERS_BORDERROUND.dp,
                        bottomEnd = DIM_EVENT_NUMBERS_BORDERROUND.dp
                    )
                )
                .padding(DIM_DEFAULT_EXT_PADDING.dp)
        ) {
            Column(modifier = Modifier.padding(start = DIM_DEFAULT_INT_PADDING.dp, end = DIM_DEFAULT_INT_PADDING.dp)) {
                Text(
                    text = stringResource(ShRes.string.be_waved),
                    modifier = Modifier.fillMaxWidth(),
                    style = extraQuinaryColoredBoldTextStyle(DIM_EVENT_NUMBERS_TITLE_FONTSIZE).copy(
                        textAlign = TextAlign.Right
                    )
                )
                Spacer(modifier = Modifier.height(DIM_EVENT_NUMBERS_SPACER.dp))
                eventNumbers.forEach { (key, value) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Label
                        Text(
                            text = stringResource(key),
                            style = extraQuinaryColoredBoldTextStyle(DIM_EVENT_NUMBERS_LABEL_FONTSIZE)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Value
                            Text(
                                text = value,
                                style = extraBoldTextStyle(DIM_EVENT_NUMBERS_VALUE_FONTSIZE).copy(
                                    color = when (key) {
                                        ShRes.string.wave_progression -> MaterialTheme.colorScheme.secondary
                                        ShRes.string.wave_start_time -> Color.Yellow
                                        else -> MaterialTheme.colorScheme.primary
                                    }
                                )
                            )
                            // optional Timezone
                            if (key in listOf(ShRes.string.wave_start_time, ShRes.string.wave_end_time)) {
                                Text(
                                    text = " ${eventTimeZone.value}",
                                    style = extraLightTextStyle(DIM_EVENT_NUMBERS_TZ_FONTSIZE).copy(
                                        color = when (key) {
                                            ShRes.string.wave_start_time -> Color.Yellow
                                            else -> MaterialTheme.colorScheme.primary
                                        }
                                    )
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(DIM_EVENT_NUMBERS_SPACER.dp / 2))
                }
            }
        }
    }
}
