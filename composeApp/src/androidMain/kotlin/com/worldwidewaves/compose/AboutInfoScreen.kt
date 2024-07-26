package com.worldwidewaves.compose

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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.worldwidewaves.activities.TabScreen
import com.worldwidewaves.shared.generated.resources.Res
import com.worldwidewaves.shared.generated.resources.drwave
import com.worldwidewaves.shared.generated.resources.drwave_instagram
import com.worldwidewaves.shared.generated.resources.infos_core
import com.worldwidewaves.shared.generated.resources.instagram_icon
import com.worldwidewaves.shared.generated.resources.logo_description
import com.worldwidewaves.shared.generated.resources.www_hashtag
import com.worldwidewaves.shared.generated.resources.www_instagram
import com.worldwidewaves.shared.generated.resources.www_logo_transparent
import com.worldwidewaves.theme.displayFontFamily
import com.worldwidewaves.theme.extraFontFamily
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.worldwidewaves.shared.generated.resources.Res as ShRes

class AboutInfoScreen : TabScreen {

    override fun getName(): String = "Infos"

    @Composable
    override fun Screen(modifier: Modifier) {
        val state = rememberLazyListState()

        Surface(modifier = modifier) {
            LazyColumn(state = state,
                modifier = Modifier
                    .padding(start = 20.dp, end = 20.dp)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item { // WWW Logo
                    Image(
                        painter = painterResource(Res.drawable.www_logo_transparent),
                        contentDescription = stringResource(Res.string.logo_description),
                        modifier = Modifier
                            .width(250.dp)
                            .padding(top = 10.dp)
                    )
                    Spacer(modifier = Modifier.size(20.dp))
                }
                item { // Main Info text
                    Text(
                        text = stringResource(ShRes.string.infos_core),
                        style = TextStyle(textAlign = TextAlign.Justify),
                        fontSize = 18.sp,
                        fontFamily = displayFontFamily
                    )
                }
                item { // DrWave signature + contact
                    Spacer(modifier = Modifier.size(30.dp))
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = stringResource(ShRes.string.drwave),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = extraFontFamily
                        )
                        Row {
                            Image(
                                painter = painterResource(ShRes.drawable.instagram_icon),
                                contentDescription = "Instagram logo",
                                modifier = Modifier.width(25.dp)
                            )
                            Text(
                                modifier = Modifier.padding(start = 10.dp),
                                text = stringResource(ShRes.string.drwave_instagram),
                                fontSize = 16.sp,
                                fontFamily = displayFontFamily
                            )
                        }
                    }
                    Spacer(modifier = Modifier.size(10.dp))
                }
                item { // Divider line
                    Spacer(modifier = Modifier.size(30.dp))
                    HorizontalDivider(
                        modifier = Modifier.width(200.dp),
                        color = Color.White, thickness = 2.dp
                    )
                    Spacer(modifier = Modifier.size(30.dp))
                }
                item { // WWW social networks
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(ShRes.drawable.instagram_icon),
                            contentDescription = "Instagram logo",
                            modifier = Modifier.width(90.dp)
                        )
                        Column(
                            modifier = Modifier.padding(start = 10.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = stringResource(ShRes.string.www_instagram),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = displayFontFamily
                            )
                            Text(
                                text = stringResource(ShRes.string.www_hashtag),
                                fontSize = 16.sp,
                                fontFamily = displayFontFamily
                            )
                        }
                    }
                    Spacer(modifier = Modifier.size(50.dp))
                }
            }
        }
    }

}
