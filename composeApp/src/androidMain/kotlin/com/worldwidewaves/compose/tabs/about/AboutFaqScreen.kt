package com.worldwidewaves.compose.tabs.about

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.worldwidewaves.activities.utils.TabScreen
import com.worldwidewaves.compose.tabs.AboutDividerLine
import com.worldwidewaves.compose.tabs.AboutWWWLogo
import com.worldwidewaves.compose.tabs.AboutWWWSocialNetworks
import com.worldwidewaves.shared.MokoRes
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_DEFAULT_INT_PADDING
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_DEFAULT_SPACER_BIG
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_DEFAULT_SPACER_MEDIUM
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_DEFAULT_SPACER_SMALL
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_FAQ_INTRO_FONTSIZE
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_FAQ_LINK_FONTSIZE
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_FAQ_RULE_ANSWER_FONTSIZE
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_FAQ_RULE_CONTENTS_FONTSIZE
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_FAQ_RULE_NBRING_WIDTH
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_FAQ_RULE_QUESTION_FONTSIZE
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_FAQ_RULE_TITLE_FONTSIZE
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_FAQ_SECTION_TITLE_FONTSIZE
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_FAQ_TITLE_FONTSIZE
import com.worldwidewaves.shared.WWWPlatform
import com.worldwidewaves.shared.faq_contents
import com.worldwidewaves.shared.rules_hierarchy
import com.worldwidewaves.theme.commonBoldStyle
import com.worldwidewaves.theme.commonJustifiedTextStyle
import com.worldwidewaves.theme.commonTextStyle
import com.worldwidewaves.theme.extraBoldTextStyle
import com.worldwidewaves.theme.extraPrimaryColoredBoldTextStyle
import com.worldwidewaves.theme.primaryColoredBoldTextStyle
import com.worldwidewaves.theme.quinaryColoredBoldTextStyle
import dev.icerock.moko.resources.StringResource
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * *About > FAQ* tab implementation.
 *
 * Implements [TabScreen] to display:
 * • A “Rules & Security” section rendered from [rules_hierarchy]
 * • An interactive **FAQ** list whose items expand / collapse on tap
 * • A “Jump to FAQ” link that smoothly scrolls to the FAQ section
 *
 * State is maintained with `remember` so expansion and scroll positions survive
 * recompositions.  All strings are localized via `MokoRes`.
 */
class AboutFaqScreen(
    private val platform: WWWPlatform,
) : TabScreen {
    override val name = "FAQ"

    companion object {
        // UI Layout Constants
        private const val LAYOUT_HALF_WIDTH = 0.5f
        private const val SPACER_SMALL_SIZE = 10f
    }

    @Composable
    override fun Screen(modifier: Modifier) {
        val coroutineScope = rememberCoroutineScope()
        val scrollState = rememberScrollState()
        var scrollToFAQPosition by remember { mutableFloatStateOf(0F) }
        var expandedFaqItem by remember { mutableIntStateOf(-1) }

        Box(modifier = modifier) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(scrollState),
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
                Spacer(modifier = Modifier.size(DIM_DEFAULT_SPACER_SMALL.dp))
                Text(
                    modifier =
                        Modifier.onGloballyPositioned { coordinates ->
                            // Save the position of the FAQ section
                            scrollToFAQPosition = coordinates.positionInRoot().y
                        },
                    text = stringResource(MokoRes.strings.faq),
                    style = extraBoldTextStyle(DIM_FAQ_TITLE_FONTSIZE),
                )
                Spacer(modifier = Modifier.size(DIM_DEFAULT_SPACER_BIG.dp))

                // FAQ Items
                FAQDividerLine()
                faq_contents.forEachIndexed { index, (question, answer) ->
                    FAQItem(
                        index,
                        question,
                        answer,
                        expandedFaqItem,
                        onExpand = { expandedFaqItem = it },
                        showSimulateButton = (question == MokoRes.strings.faq_question_6),
                    )
                    FAQDividerLine()
                }

                // WWW Social Networks
                Spacer(modifier = Modifier.size(DIM_DEFAULT_SPACER_MEDIUM.dp))
                AboutDividerLine()
                AboutWWWSocialNetworks()
            }
        }
    }

    // ----------------------------

    @Composable
    /**
     * Header shown above the rules section.
     *
     * Displays the title + an underlined link that, when tapped, invokes the
     * provided callback to scroll to the FAQ block further down in the list.
     */
    private fun FAQTitle(scrollToFAQPosition: () -> Unit) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                modifier = Modifier.fillMaxWidth(LAYOUT_HALF_WIDTH),
                text = stringResource(MokoRes.strings.warn_rules_security_title),
                style =
                    extraPrimaryColoredBoldTextStyle(DIM_FAQ_SECTION_TITLE_FONTSIZE).copy(
                        textAlign = TextAlign.Start,
                    ),
            )
            Text(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable(onClick = scrollToFAQPosition),
                text = stringResource(MokoRes.strings.faq_access),
                style =
                    quinaryColoredBoldTextStyle(DIM_FAQ_LINK_FONTSIZE).copy(
                        textDecoration = TextDecoration.Underline,
                        textAlign = TextAlign.End,
                    ),
            )
        }
        Spacer(modifier = Modifier.size(DIM_DEFAULT_SPACER_MEDIUM.dp))
        Text(
            text = stringResource(MokoRes.strings.warn_rules_security_text),
            fontSize = DIM_FAQ_INTRO_FONTSIZE.sp,
            style = commonTextStyle().copy(textAlign = TextAlign.Justify),
        )
    }

    @Composable
    private fun FAQDividerLine() {
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            thickness = 2.dp,
        )
    }

    // ----------------------------

    @Composable
    /**
     * Iterates over [rules_hierarchy] and renders each rule section with a
     * numbered list of items.
     */
    private fun ShowRulesHierarchy() {
        rules_hierarchy.forEach { (title, items) ->
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(title),
                style =
                    extraPrimaryColoredBoldTextStyle(DIM_FAQ_RULE_TITLE_FONTSIZE).copy(
                        textAlign = TextAlign.Start,
                    ),
            )
            Spacer(modifier = Modifier.size(DIM_DEFAULT_SPACER_SMALL.dp))
            items.forEachIndexed { index, item ->
                Row(modifier = Modifier.padding(bottom = DIM_DEFAULT_INT_PADDING.dp / 2)) {
                    Text(
                        modifier = Modifier.width(DIM_FAQ_RULE_NBRING_WIDTH.dp),
                        text = (index + 1).toString() + ".",
                        style = commonBoldStyle(DIM_FAQ_RULE_CONTENTS_FONTSIZE),
                    )
                    Text(
                        modifier = Modifier.padding(start = DIM_DEFAULT_INT_PADDING.dp),
                        text = stringResource(item),
                        style = commonJustifiedTextStyle(DIM_FAQ_RULE_CONTENTS_FONTSIZE),
                    )
                }
            }
            Spacer(modifier = Modifier.size(DIM_DEFAULT_SPACER_MEDIUM.dp))
        }
    }

    // ----------------------------

    @Composable
    /**
     * Single expandable FAQ entry.
     *
     * Tapping the row toggles its expanded state via [onExpand]; the currently
     * expanded item index is held by the parent so only one entry can be open
     * at a time.
     */
    private fun FAQItem(
        itemIndex: Int,
        questionResource: StringResource,
        answerResource: StringResource,
        expandedFaqItem: Int,
        onExpand: (Int) -> Unit,
        showSimulateButton: Boolean = false,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(DIM_DEFAULT_INT_PADDING.dp)
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
                    style = primaryColoredBoldTextStyle(DIM_FAQ_RULE_QUESTION_FONTSIZE),
                    modifier = Modifier.weight(1f),
                )
            }

            if (expandedFaqItem == itemIndex) {
                Spacer(modifier = Modifier.size(SPACER_SMALL_SIZE.dp))
                Text(
                    text = stringResource(answerResource),
                    style = commonJustifiedTextStyle(DIM_FAQ_RULE_ANSWER_FONTSIZE),
                )
                if (showSimulateButton) {
                    Spacer(modifier = Modifier.size(DIM_DEFAULT_SPACER_SMALL.dp))
                    OutlinedButton(
                        onClick = { platform.enableSimulationMode() },
                    ) {
                        Text(
                            text = stringResource(MokoRes.strings.test_simulation),
                            style = primaryColoredBoldTextStyle(DIM_FAQ_RULE_QUESTION_FONTSIZE - 2),
                        )
                    }
                }
            }
        }
    }
}
