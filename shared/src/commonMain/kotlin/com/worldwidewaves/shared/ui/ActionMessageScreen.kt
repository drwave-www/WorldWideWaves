package com.worldwidewaves.shared.ui

/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.worldwidewaves.shared.MokoRes
import com.worldwidewaves.shared.PlatformEnabler
import com.worldwidewaves.shared.WWWGlobals.Dimensions
import com.worldwidewaves.shared.WWWGlobals.Info
import com.worldwidewaves.shared.ui.components.about.AboutWWWLogo
import com.worldwidewaves.shared.ui.components.about.LogoSeparator
import com.worldwidewaves.shared.ui.theme.sharedCommonTextStyle
import com.worldwidewaves.shared.ui.theme.sharedExtraBoldTextStyle
import dev.icerock.moko.resources.compose.stringResource

/**
 * Tab screen for Action Message content.
 * Displays a call-to-action message with bold phrases and explanatory sections.
 */
class ActionMessageScreen : TabScreen {
    override val name = "Action"

    @Composable
    override fun Screen(
        platformEnabler: PlatformEnabler,
        modifier: Modifier,
    ) {
        ActionMessageContent(modifier = modifier)
    }
}

/**
 * Displays the Action Message content with title, bold phrases, and explanatory sections.
 */
@Composable
private fun ActionMessageContent(modifier: Modifier = Modifier) {
    val state = rememberLazyListState()
    val dir = LocalLayoutDirection.current

    Surface(modifier = modifier.padding(Dimensions.DEFAULT_EXT_PADDING.dp)) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = state,
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    AboutWWWLogo()
                }

                item {
                    Text(
                        text = stringResource(MokoRes.strings.action_message_title),
                        style =
                            sharedExtraBoldTextStyle(Info.DRWAVE_FONTSIZE).copy(
                                textAlign = TextAlign.Center,
                            ),
                        modifier =
                            Modifier
                                .padding(vertical = 8.dp)
                                .semantics { heading() },
                    )
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BoldPhrase(MokoRes.strings.action_message_phrase_1)
                        BoldPhrase(MokoRes.strings.action_message_phrase_2)
                        BoldPhrase(MokoRes.strings.action_message_phrase_3)
                        BoldPhrase(MokoRes.strings.action_message_phrase_4)
                        BoldPhrase(MokoRes.strings.action_message_phrase_5)
                        BoldPhrase(MokoRes.strings.action_message_phrase_6)
                    }
                }

                item {
                    Spacer(modifier = Modifier.size(8.dp))
                }

                item {
                    TextSection(MokoRes.strings.action_message_section_1, dir)
                }

                item {
                    Spacer(modifier = Modifier.height((-8).dp))
                }

                item {
                    LogoSeparator()
                }

                item {
                    Spacer(modifier = Modifier.height((-8).dp))
                }

                item {
                    TextSection(MokoRes.strings.action_message_section_2, dir)
                }

                item {
                    Spacer(modifier = Modifier.height((-8).dp))
                }

                item {
                    LogoSeparator()
                }

                item {
                    Spacer(modifier = Modifier.height((-8).dp))
                }

                item {
                    TextSection(MokoRes.strings.action_message_section_3, dir)
                }
            }
        }
    }
}

/**
 * Displays a text section with proper text alignment based on layout direction.
 */
@Composable
private fun TextSection(
    stringRes: dev.icerock.moko.resources.StringResource,
    layoutDirection: LayoutDirection,
) {
    Text(
        text = stringResource(stringRes),
        style =
            sharedCommonTextStyle(Info.TEXT_FONTSIZE).copy(
                textAlign = if (layoutDirection == LayoutDirection.Rtl) TextAlign.Start else TextAlign.Justify,
            ),
    )
}

/**
 * Displays a bold phrase with a bullet point.
 */
@Composable
private fun BoldPhrase(stringRes: dev.icerock.moko.resources.StringResource) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = "•",
            style =
                sharedCommonTextStyle(Info.TEXT_FONTSIZE).copy(
                    fontWeight = FontWeight.Bold,
                ),
        )
        Text(
            text = stringResource(stringRes),
            style =
                sharedCommonTextStyle(Info.TEXT_FONTSIZE).copy(
                    fontWeight = FontWeight.Bold,
                ),
        )
    }
}
