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
package com.worldwidewaves.compose

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.worldwidewaves.shared.events.WWWEvent
import com.worldwidewaves.shared.events.getCommunityImage
import com.worldwidewaves.shared.events.getCountryImage
import com.worldwidewaves.shared.events.getLocationImage
import com.worldwidewaves.shared.events.isDone
import com.worldwidewaves.shared.events.isRunning
import com.worldwidewaves.shared.events.isSoon
import com.worldwidewaves.shared.generated.resources.event_done
import com.worldwidewaves.shared.generated.resources.event_running
import com.worldwidewaves.shared.generated.resources.event_soon
import com.worldwidewaves.shared.generated.resources.events_select_all
import com.worldwidewaves.shared.generated.resources.events_select_starred
import com.worldwidewaves.ui.AppTheme
import com.worldwidewaves.ui.extendedLight
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import java.text.SimpleDateFormat
import java.util.Locale
import com.worldwidewaves.shared.generated.resources.Res as ShRes

// ----------------------------

class EventsActivity : AppCompatActivity() {

    private val viewModel: EventsViewModel by viewModels<EventsViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                EventsScreen(viewModel)
            }
        }
    }

    // ----------------------------

    @Composable
    private fun EventsScreen(viewModel: EventsViewModel) {
        val events by viewModel.events.collectAsState()

        Surface {
            Box(
                modifier = Modifier
                    .padding(start = 20.dp, end = 20.dp, top = 20.dp)
                    .fillMaxSize()
            ) {
                Column {
                    FavoritesSelector()
                    Spacer(modifier = Modifier.size(20.dp))
                    Events(events)
                }
            }
        }
    }
}

// ----------------------------

@Composable
fun FavoritesSelector(modifier: Modifier = Modifier, starredSelected: Boolean = false) {
    val allColor = if (starredSelected) extendedLight.quaternary else extendedLight.quinary
    val starredColor = if (starredSelected) extendedLight.quinary else extendedLight.quaternary

    val allWeight = if (starredSelected) FontWeight.Normal else FontWeight.Bold
    val starredWeight = if (starredSelected) FontWeight.Bold else FontWeight.Normal

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(25.dp))
            .background(extendedLight.quaternary.color)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(25.dp))
                    .height(50.dp)
                    .fillMaxWidth(.5f)
                    .background(allColor.color),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    color = allColor.onColor, fontWeight = allWeight, fontSize = 16.sp,
                    text = stringResource(ShRes.string.events_select_all)
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(25.dp))
                    .height(50.dp)
                    .fillMaxWidth()
                    .background(starredColor.color),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    color = starredColor.onColor, fontWeight = starredWeight, fontSize = 16.sp,
                    text = stringResource(ShRes.string.events_select_starred)
                )
            }
        }
    }
}

// ----------------------------

@Composable
fun Events(events: List<WWWEvent>, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier
    ) {
        items(events) { event ->
            Event(event)
        }
    }
}

@Composable
fun Event(event: WWWEvent, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        EventOverlay(event)
        EventLocationAndDate(event)
    }
}

@Composable
private fun EventOverlay(event: WWWEvent, modifier: Modifier = Modifier) {
    val heightModifier = Modifier.height(159.dp)

    // Main event space with image and layers
    Box(modifier = heightModifier) {
        // Main Image
        Box(modifier = heightModifier) {
            Image(
                modifier = modifier,
                contentScale = ContentScale.FillWidth,
                painter = painterResource(event.getLocationImage() as DrawableResource),
                contentDescription = event.location
            )
        }

        EventOverlayCountryAndCommunityFlags(event, heightModifier)
        EventOverlaySoonOrRunning(event)
        EventOverlayDone(event)
    }
}

@Composable
private fun EventOverlayDone(event: WWWEvent, modifier: Modifier = Modifier) {
    if (event.isDone()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Surface(
                color = Color.run { White.copy(alpha = 0.5f) },
                modifier = Modifier.fillMaxSize()
            ) { }
            Image(
                painter = painterResource(ShRes.drawable.event_done),
                contentDescription = stringResource(ShRes.string.event_done),
                modifier = Modifier.width(130.dp),
            )
        }
    }
}

@Composable
private fun EventOverlaySoonOrRunning(event: WWWEvent, modifier: Modifier = Modifier) {
    if (event.isSoon() || event.isRunning()) {
        val backgroundColor =
            if (event.isSoon()) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary
        val textId = if (event.isSoon()) ShRes.string.event_soon else ShRes.string.event_running

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

@Composable
private fun EventOverlayCountryAndCommunityFlags(event: WWWEvent, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        event.community?.let {
            Image(
                modifier = Modifier
                    .width(65.dp)
                    .padding(start = 10.dp, top = 10.dp)
                    .border(1.dp, Color.White),
                contentScale = ContentScale.FillWidth,
                painter = painterResource(event.getCommunityImage() as DrawableResource),
                contentDescription = event.community!!
            )
        }
        event.country?.let {
              Image(
                modifier = Modifier
                    .width(65.dp)
                    .padding(start = 10.dp, bottom = 10.dp)
                    .border(1.dp, Color.White),
                contentScale = ContentScale.FillWidth,
                painter = painterResource(event.getCountryImage() as DrawableResource),
                contentDescription = event.community!!
              )
        }
    }
}

// ----------------------------

@Composable
private fun EventLocationAndDate(event: WWWEvent, modifier: Modifier = Modifier) {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val date = formatter.parse(event.date) // Assuming event.date is in "yyyy-MM-dd" format
    val outputFormatter = SimpleDateFormat("dd/MM", Locale.getDefault())
    val eventDate = date?.let { outputFormatter.format(it) }

    Box(
        modifier = modifier
    ) {
        Column {

            // Location and date
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = event.location.uppercase(),
                    style = TextStyle(
                        color = extendedLight.quinary.color,
                        fontFamily = MaterialTheme.typography.bodyMedium.fontFamily,
                        fontSize = 28.sp
                    )
                )
                eventDate?.let {
                    Text(
                        text = it,
                        modifier = Modifier.padding(end = 2.dp),
                        style = TextStyle(
                            color = MaterialTheme.colorScheme.primary,
                            fontFamily = MaterialTheme.typography.bodyMedium.fontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 34.sp
                        )
                    )
                }
            }

            // Country if present
            Text(
                text = event.country?.lowercase()?.replaceFirstChar(Char::titlecaseChar) ?: "",
                style = TextStyle(
                    color = extendedLight.quinary.color,
                    fontFamily = MaterialTheme.typography.bodyMedium.fontFamily,
                    fontSize = 14.sp
                ),
                modifier = Modifier
                    .offset(y = (-8).dp)
                    .padding(start = 2.dp)
            )

        }
    }
}
