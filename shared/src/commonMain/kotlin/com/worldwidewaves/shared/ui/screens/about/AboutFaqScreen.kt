package com.worldwidewaves.shared.ui.screens.about

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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.worldwidewaves.shared.MokoRes
import com.worldwidewaves.shared.WWWGlobals.Dimensions
import com.worldwidewaves.shared.WWWGlobals.FAQ
import com.worldwidewaves.shared.WWWPlatform
import com.worldwidewaves.shared.resources.faq_contents
import com.worldwidewaves.shared.resources.rules_hierarchy
import com.worldwidewaves.shared.ui.components.about.AboutDividerLine
import com.worldwidewaves.shared.ui.components.about.AboutWWWLogo
import com.worldwidewaves.shared.ui.components.about.AboutWWWSocialNetworks
import com.worldwidewaves.shared.ui.theme.sharedCommonBoldStyle
import com.worldwidewaves.shared.ui.theme.sharedCommonJustifiedTextStyle
import com.worldwidewaves.shared.ui.theme.sharedCommonTextStyle
import com.worldwidewaves.shared.ui.theme.sharedExtraBoldTextStyle
import com.worldwidewaves.shared.ui.theme.sharedExtraPrimaryColoredBoldTextStyle
import com.worldwidewaves.shared.ui.theme.sharedPrimaryColoredBoldTextStyle
import com.worldwidewaves.shared.ui.theme.sharedQuinaryColoredBoldTextStyle
import com.worldwidewaves.shared.ui.utils.findClickableLinks
import com.worldwidewaves.shared.ui.utils.focusIndicator
import com.worldwidewaves.shared.utils.Log
import dev.icerock.moko.resources.StringResource
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Shared About > FAQ screen implementation.
 *
 * Displays:
 * • A "Rules & Security" section rendered from rules_hierarchy
 * • An interactive FAQ list whose items expand / collapse on tap
 * • A "Jump to FAQ" link that smoothly scrolls to the FAQ section
 *
 * State is maintained with remember so expansion and scroll positions survive
 * recompositions. All strings are localized via MokoRes.
 * Works identically on both Android and iOS platforms.
 */
@Composable
fun AboutFaqScreen(
    platform: WWWPlatform,
    modifier: Modifier = Modifier,
    onUrlOpen: (String) -> Unit = { url ->
        Log.i("AboutFaqScreen", "URL click: $url")
    },
    onSimulateClick: () -> Unit = {
        platform.enableSimulationMode()
    },
) {
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    var scrollToFAQPosition by remember { mutableFloatStateOf(0F) }
    var expandedFaqItem by remember { mutableIntStateOf(-1) }

    Box(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(scrollState).testTag("FaqList"),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AboutWWWLogo()

            // Main FAQ text - Rules & Security
            FAQTitle {
                // Scroll to the top of the FAQ section
                coroutineScope.launch {
                    scrollState.animateScrollTo(scrollToFAQPosition.roundToInt())
                }
            }
            AboutDividerLine()

            // For each rules_hierarchy entry, display the title and the list of items
            ShowRulesHierarchy()
            AboutDividerLine()

            // FAQ title
            Spacer(modifier = Modifier.size(Dimensions.SPACER_SMALL.dp))
            Text(
                modifier =
                    Modifier
                        .onGloballyPositioned { coordinates ->
                            // Save the position of the FAQ section
                            scrollToFAQPosition = coordinates.positionInRoot().y
                        }.semantics { heading() },
                text = stringResource(MokoRes.strings.faq),
                style = sharedExtraBoldTextStyle(FAQ.TITLE_FONTSIZE),
            )
            Spacer(modifier = Modifier.size(Dimensions.SPACER_BIG.dp))

            // FAQ Items
            FAQDividerLine()
            faq_contents.forEachIndexed { index, (question, answer) ->
                FAQItem(
                    index,
                    question,
                    answer,
                    expandedFaqItem,
                    onExpand = { expandedFaqItem = it },
                    showSimulateButton = (question == MokoRes.strings.faq_question_7),
                    onSimulateClick = onSimulateClick,
                    onUrlOpen = onUrlOpen,
                )
                FAQDividerLine()
            }

            Spacer(modifier = Modifier.size(Dimensions.SPACER_BIG.dp))
            AboutWWWSocialNetworks(onUrlOpen = onUrlOpen)
        }
    }
}

// FAQ-specific components
private const val LAYOUT_HALF_WIDTH = 0.5f
private const val SPACER_SMALL_SIZE = 10f

@Composable
private fun FAQTitle(scrollToFAQPosition: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            modifier =
                Modifier
                    .fillMaxWidth(LAYOUT_HALF_WIDTH)
                    .semantics { heading() },
            text = stringResource(MokoRes.strings.warn_rules_security_title),
            style =
                sharedExtraPrimaryColoredBoldTextStyle(FAQ.SECTION_TITLE_FONTSIZE).copy(
                    textAlign = TextAlign.Start,
                ),
        )
        Text(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .focusIndicator()
                    .clickable(onClick = scrollToFAQPosition),
            text = stringResource(MokoRes.strings.faq_access),
            style =
                sharedQuinaryColoredBoldTextStyle(FAQ.LINK_FONTSIZE).copy(
                    textDecoration = TextDecoration.Underline,
                    textAlign = TextAlign.End,
                ),
        )
    }
    Spacer(modifier = Modifier.size(Dimensions.SPACER_MEDIUM.dp))
    Text(
        text = stringResource(MokoRes.strings.warn_rules_security_text),
        fontSize = FAQ.INTRO_FONTSIZE.sp,
        style = sharedCommonTextStyle().copy(textAlign = TextAlign.Justify),
    )
}

@Composable
private fun ShowRulesHierarchy() {
    rules_hierarchy.forEach { (title, items) ->
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(title),
            style =
                sharedExtraPrimaryColoredBoldTextStyle(FAQ.RULE_TITLE_FONTSIZE).copy(
                    textAlign = TextAlign.Start,
                ),
        )
        Spacer(modifier = Modifier.size(Dimensions.SPACER_SMALL.dp))
        items.forEachIndexed { index, item ->
            Row(modifier = Modifier.padding(bottom = Dimensions.DEFAULT_INT_PADDING.dp / 2)) {
                Text(
                    modifier = Modifier.width(FAQ.RULE_NBRING_WIDTH.dp),
                    text = (index + 1).toString() + ".",
                    style = sharedCommonBoldStyle(FAQ.RULE_CONTENTS_FONTSIZE),
                )
                Text(
                    modifier = Modifier.padding(start = Dimensions.DEFAULT_INT_PADDING.dp),
                    text = stringResource(item),
                    style = sharedCommonJustifiedTextStyle(FAQ.RULE_CONTENTS_FONTSIZE),
                )
            }
        }
        Spacer(modifier = Modifier.size(Dimensions.SPACER_MEDIUM.dp))
    }
}

/**
 * Creates an AnnotatedString with clickable URLs and email addresses using LinkAnnotation.
 * URLs and emails are styled with underline and made clickable.
 * Uses modern LinkAnnotation API for better accessibility support.
 */
@Composable
private fun createClickableText(
    text: String,
    onUrlOpen: (String) -> Unit,
): AnnotatedString {
    val linkColor = sharedQuinaryColoredBoldTextStyle(FAQ.RULE_ANSWER_FONTSIZE).color
    val links = findClickableLinks(text)

    return buildAnnotatedString {
        // First append all the text
        append(text)

        // Then add link annotations at the correct positions
        links.forEach { link ->
            val linkAnnotation =
                LinkAnnotation.Clickable(
                    tag = "URL",
                    styles =
                        TextLinkStyles(
                            style =
                                SpanStyle(
                                    color = linkColor,
                                    textDecoration = TextDecoration.Underline,
                                ),
                        ),
                    linkInteractionListener = {
                        onUrlOpen(link.url)
                    },
                )
            addLink(linkAnnotation, link.range.first, link.range.last + 1)
        }
    }
}

@Composable
private fun FAQItem(
    itemIndex: Int,
    questionResource: StringResource,
    answerResource: StringResource,
    expandedFaqItem: Int,
    onExpand: (Int) -> Unit,
    showSimulateButton: Boolean = false,
    onSimulateClick: () -> Unit,
    onUrlOpen: (String) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .testTag("FaqItem_$itemIndex")
                .fillMaxWidth()
                .padding(Dimensions.DEFAULT_INT_PADDING.dp)
                .clickable {
                    onExpand(if (expandedFaqItem == itemIndex) -1 else itemIndex)
                },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(questionResource),
                style = sharedPrimaryColoredBoldTextStyle(FAQ.RULE_QUESTION_FONTSIZE),
                modifier = Modifier.weight(1f),
            )
        }

        if (expandedFaqItem == itemIndex) {
            Spacer(modifier = Modifier.size(SPACER_SMALL_SIZE.dp))
            val answerText = stringResource(answerResource)
            val annotatedString = createClickableText(text = answerText, onUrlOpen = onUrlOpen)

            BasicText(
                text = annotatedString,
                style = sharedCommonJustifiedTextStyle(FAQ.RULE_ANSWER_FONTSIZE),
                modifier =
                    Modifier.semantics {
                        role = Role.Button
                        contentDescription = "FAQ answer with clickable links"
                    },
            )
            if (showSimulateButton) {
                Spacer(modifier = Modifier.size(Dimensions.SPACER_SMALL.dp))
                OutlinedButton(
                    onClick = onSimulateClick,
                ) {
                    Text(
                        text = stringResource(MokoRes.strings.test_simulation),
                        style = sharedPrimaryColoredBoldTextStyle(FAQ.RULE_QUESTION_FONTSIZE - 2),
                    )
                }
            }
        }
    }
}

@Composable
private fun FAQDividerLine() {
    HorizontalDivider(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        thickness = 2.dp,
    )
}
