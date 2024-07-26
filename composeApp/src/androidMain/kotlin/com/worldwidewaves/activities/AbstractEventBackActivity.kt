package com.worldwidewaves.activities

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.worldwidewaves.shared.SetEventFavorite
import com.worldwidewaves.shared.events.WWWEvent
import com.worldwidewaves.shared.events.WWWEvents
import com.worldwidewaves.shared.generated.resources.back
import com.worldwidewaves.shared.generated.resources.event_favorite_off
import com.worldwidewaves.shared.generated.resources.event_favorite_on
import com.worldwidewaves.shared.generated.resources.favorite_off
import com.worldwidewaves.shared.generated.resources.favorite_on
import com.worldwidewaves.theme.AppTheme
import com.worldwidewaves.theme.quinaryLight
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.android.ext.android.inject
import com.worldwidewaves.shared.generated.resources.Res as ShRes

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

abstract class AbstractEventBackActivity : MainActivity() {

    private val wwwEvents: WWWEvents by inject()
    private val setEventFavorite: SetEventFavorite by inject()

    // ----------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var selectedEvent by mutableStateOf<WWWEvent?>(null)

        val eventId = intent.getStringExtra("eventId")
        if (eventId != null)
            wwwEvents.invokeWhenLoaded { // Update when loaded
                lifecycleScope.launch {
                    selectedEvent = wwwEvents.getEventById(eventId)
                }
            }

        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.background(MaterialTheme.colorScheme.background)
                ) {
                    tabManager.TabView(startScreen = { BackwardScreen(selectedEvent) })
                }
            }
        }
    }

    // ----------------------------

    @Composable
    private fun BackwardScreen(event: WWWEvent?) {
        var isFavorite by remember { mutableStateOf(event?.favorite ?: false) }
        val scope = rememberCoroutineScope()

        LaunchedEffect(event) {
            isFavorite = event?.favorite ?: false
        }

        if (event != null) {
            LaunchedEffect(event.favorite) {
                isFavorite = event.favorite
            }

            Column(modifier = Modifier.fillMaxWidth()) {

                // Back layer
                Row(
                    modifier = Modifier.padding(start = 10.dp, top = 10.dp, bottom = 15.dp, end = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier.clickable(onClick = { finish() }),
                        text = "< "+ stringResource(ShRes.string.back),
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = MaterialTheme.typography.bodyMedium.fontFamily,
                        fontSize = 16.sp,
                    )
                    Text(
                        modifier = Modifier.weight(1f),
                        text = event.location.uppercase(),
                        color = quinaryLight,
                        fontFamily = MaterialTheme.typography.bodyMedium.fontFamily,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        fontSize = 24.sp,
                    )
                    Image(
                        modifier = Modifier
                            .size(48.dp)
                            .clickable {
                                scope.launch {
                                    isFavorite = !isFavorite
                                    setEventFavorite.call(event, isFavorite)
                                }
                            },
                        painter = painterResource(if (isFavorite) ShRes.drawable.favorite_on else ShRes.drawable.favorite_off),
                        contentDescription = stringResource(if (isFavorite) ShRes.string.event_favorite_on else ShRes.string.event_favorite_off),
                    )
                }

                // Content screen
                Screen(modifier = Modifier, event)
            }
        } else {
            Text(
                text = "Event not found",
                color = MaterialTheme.colorScheme.primary,
                fontFamily = MaterialTheme.typography.bodyMedium.fontFamily,
                fontSize = 16.sp,
            )
        }
    }

    @Composable
    abstract fun Screen(modifier: Modifier, event: WWWEvent)

}