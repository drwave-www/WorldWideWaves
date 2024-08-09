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

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.worldwidewaves.activities.utils.TabScreen
import com.worldwidewaves.shared.generated.resources.drwave
import com.worldwidewaves.shared.generated.resources.drwave_instagram
import com.worldwidewaves.shared.generated.resources.drwave_instagram_url
import com.worldwidewaves.shared.generated.resources.infos_core
import com.worldwidewaves.shared.generated.resources.instagram_icon
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

        Box(modifier = modifier) {
            LazyColumn(
                state = state,
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item { AboutWWWLogo() }
                item { MainInfo() }
                item { DrWaveSignatureAndContact() }
                item { AboutDividerLine() }
                item { AboutWWWSocialNetworks() }
            }
        }
    }

    // ----------------------------

    @Composable
    private fun MainInfo() {
        Text(
            text = stringResource(ShRes.string.infos_core),
            style = TextStyle(textAlign = TextAlign.Justify),
            fontSize = 18.sp,
            fontFamily = displayFontFamily
        )
    }

    // ----------------------------

    @Composable
    private fun DrWaveSignatureAndContact() {
        val uriHandler = LocalUriHandler.current

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
                Text( // TODO Convert it to WWWALink
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .clickable(onClick = {
                            try {
                                uriHandler.openUri(ShRes.string.drwave_instagram_url.toString())
                            } catch (e: Exception) {
                                Log.e("AboutWWWSocialNetworks", "Failed to open URI", e)
                            }
                        }),
                    text = stringResource(ShRes.string.drwave_instagram),
                    fontSize = 16.sp,
                    fontFamily = displayFontFamily,
                    textDecoration = TextDecoration.Underline
                )
            }
        }
        Spacer(modifier = Modifier.size(10.dp))
    }

}
