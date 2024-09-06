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
import com.worldwidewaves.shared.generated.resources.faq
import com.worldwidewaves.shared.generated.resources.faq_access
import com.worldwidewaves.shared.generated.resources.faq_answer_1
import com.worldwidewaves.shared.generated.resources.faq_answer_2
import com.worldwidewaves.shared.generated.resources.faq_answer_3
import com.worldwidewaves.shared.generated.resources.faq_answer_4
import com.worldwidewaves.shared.generated.resources.faq_answer_5
import com.worldwidewaves.shared.generated.resources.faq_question_1
import com.worldwidewaves.shared.generated.resources.faq_question_2
import com.worldwidewaves.shared.generated.resources.faq_question_3
import com.worldwidewaves.shared.generated.resources.faq_question_4
import com.worldwidewaves.shared.generated.resources.faq_question_5
import com.worldwidewaves.shared.generated.resources.warn_emergency_item_1
import com.worldwidewaves.shared.generated.resources.warn_emergency_item_2
import com.worldwidewaves.shared.generated.resources.warn_emergency_item_3
import com.worldwidewaves.shared.generated.resources.warn_emergency_title
import com.worldwidewaves.shared.generated.resources.warn_general_item_1
import com.worldwidewaves.shared.generated.resources.warn_general_item_2
import com.worldwidewaves.shared.generated.resources.warn_general_item_3
import com.worldwidewaves.shared.generated.resources.warn_general_item_4
import com.worldwidewaves.shared.generated.resources.warn_general_item_5
import com.worldwidewaves.shared.generated.resources.warn_general_item_6
import com.worldwidewaves.shared.generated.resources.warn_general_title
import com.worldwidewaves.shared.generated.resources.warn_legal_item_1
import com.worldwidewaves.shared.generated.resources.warn_legal_item_2
import com.worldwidewaves.shared.generated.resources.warn_legal_title
import com.worldwidewaves.shared.generated.resources.warn_rules_security_text
import com.worldwidewaves.shared.generated.resources.warn_rules_security_title
import com.worldwidewaves.shared.generated.resources.warn_safety_item_1
import com.worldwidewaves.shared.generated.resources.warn_safety_item_2
import com.worldwidewaves.shared.generated.resources.warn_safety_item_3
import com.worldwidewaves.shared.generated.resources.warn_safety_item_4
import com.worldwidewaves.shared.generated.resources.warn_safety_item_5
import com.worldwidewaves.shared.generated.resources.warn_safety_title
import com.worldwidewaves.theme.commonBoldStyle
import com.worldwidewaves.theme.commonJustifiedTextStyle
import com.worldwidewaves.theme.commonTextStyle
import com.worldwidewaves.theme.displayFontFamily
import com.worldwidewaves.theme.extraBoldTextStyle
import com.worldwidewaves.theme.primaryColoredBoldTextStyle
import com.worldwidewaves.theme.primaryColoredExtraBoldTextStyle
import com.worldwidewaves.theme.quinaryColoredBoldTextStyle
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt
import com.worldwidewaves.shared.generated.resources.Res as ShRes

val rules_hierarchy = mapOf(
    ShRes.string.warn_general_title to listOf(
        ShRes.string.warn_general_item_1,
        ShRes.string.warn_general_item_2,
        ShRes.string.warn_general_item_3,
        ShRes.string.warn_general_item_4,
        ShRes.string.warn_general_item_5,
        ShRes.string.warn_general_item_6
    ),
    ShRes.string.warn_safety_title to listOf(
        ShRes.string.warn_safety_item_1,
        ShRes.string.warn_safety_item_2,
        ShRes.string.warn_safety_item_3,
        ShRes.string.warn_safety_item_4,
        ShRes.string.warn_safety_item_5
    ),
    ShRes.string.warn_emergency_title to listOf(
        ShRes.string.warn_emergency_item_1,
        ShRes.string.warn_emergency_item_2,
        ShRes.string.warn_emergency_item_3
    ),
    ShRes.string.warn_legal_title to listOf(
        ShRes.string.warn_legal_item_1,
        ShRes.string.warn_legal_item_2
    )
)

val faq_contents = listOf(
    Pair(ShRes.string.faq_question_1, ShRes.string.faq_answer_1),
    Pair(ShRes.string.faq_question_2, ShRes.string.faq_answer_2),
    Pair(ShRes.string.faq_question_3, ShRes.string.faq_answer_3),
    Pair(ShRes.string.faq_question_4, ShRes.string.faq_answer_4),
    Pair(ShRes.string.faq_question_5, ShRes.string.faq_answer_5)
)

// ----------------------------

class AboutFaqScreen : TabScreen {

    override fun getName(): String = "FAQ"

    @Composable
    override fun Screen(modifier: Modifier) {
        val coroutineScope = rememberCoroutineScope()
        val scrollState = rememberScrollState()
        var scrollToFAQPosition by remember { mutableFloatStateOf(0F) }
        var expandedFaqItem by remember { mutableIntStateOf(-1) }

        Box(modifier = modifier) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
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
                    modifier = Modifier.onGloballyPositioned { coordinates ->
                        // Save the position of the FAQ section
                        scrollToFAQPosition = coordinates.positionInRoot().y
                    },
                    text = stringResource(ShRes.string.faq),
                    style = extraBoldTextStyle.copy(
                        fontSize = DIM_FAQ_TITLE_FONTSIZE.sp
                    )
                )
                Spacer(modifier = Modifier.size(DIM_DEFAULT_SPACER_BIG.dp))

                // FAQ Items
                FAQDividerLine()
                faq_contents.forEachIndexed { index, (question, answer) ->
                    FAQItem(index, question, answer,
                        expandedFaqItem,
                        onExpand = { expandedFaqItem = it })
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
    private fun FAQTitle(scrollToFAQPosition: () -> Unit) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                modifier = Modifier.fillMaxWidth(0.5f),
                text = stringResource(ShRes.string.warn_rules_security_title),
                style = primaryColoredExtraBoldTextStyle.copy(
                    fontSize = DIM_FAQ_SECTION_TITLE_FONTSIZE.sp,
                    textAlign = TextAlign.Start
                )
            )
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = scrollToFAQPosition),
                text = stringResource(ShRes.string.faq_access),
                style = quinaryColoredBoldTextStyle.copy(
                    fontSize = DIM_FAQ_LINK_FONTSIZE.sp, fontFamily = displayFontFamily,
                    textDecoration = TextDecoration.Underline,
                    textAlign = TextAlign.End
                )
            )
        }
        Spacer(modifier = Modifier.size(DIM_DEFAULT_SPACER_MEDIUM.dp))
        Text(
            text = stringResource(ShRes.string.warn_rules_security_text),
            fontSize = DIM_FAQ_INTRO_FONTSIZE.sp,
            style = commonTextStyle.copy(textAlign = TextAlign.Justify)
        )
    }

    @Composable
    private fun FAQDividerLine() {
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(), color = Color.White, thickness = 2.dp
        )
    }

    // ----------------------------

    @Composable
    private fun ShowRulesHierarchy() {
        rules_hierarchy.forEach { (title, items) ->
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(title),
                style = primaryColoredExtraBoldTextStyle.copy(
                    fontSize = DIM_FAQ_RULE_TITLE_FONTSIZE.sp,
                    textAlign = TextAlign.Start
                )
            )
            Spacer(modifier = Modifier.size(DIM_DEFAULT_SPACER_SMALL.dp))
            items.forEachIndexed { index, item ->
                Row(modifier = Modifier.padding(bottom = DIM_DEFAULT_INT_PADDING.dp / 2)) {
                    Text(
                        modifier = Modifier.width(DIM_FAQ_RULE_NBRING_WIDTH.dp),
                        text = (index + 1).toString() + ".",
                        style = commonBoldStyle.copy(
                            fontSize = DIM_FAQ_RULE_CONTENTS_FONTSIZE.sp
                        )
                    )
                    Text(
                        modifier = Modifier.padding(start = DIM_DEFAULT_INT_PADDING.dp),
                        text = stringResource(item),
                        style = commonJustifiedTextStyle.copy(
                            fontSize = DIM_FAQ_RULE_CONTENTS_FONTSIZE.sp
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.size(DIM_DEFAULT_SPACER_MEDIUM.dp))
        }
    }

    // ----------------------------

    @Composable
    private fun FAQItem(
        itemIndex: Int,
        question: StringResource,
        answer: StringResource,
        expandedFaqItem: Int,
        onExpand: (Int) -> Unit
    ) {

        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(DIM_DEFAULT_INT_PADDING.dp)
            .clickable {
                onExpand(if (expandedFaqItem == itemIndex) -1 else itemIndex)
            }
        ) {
            Text(
                text = stringResource(question),
                style = primaryColoredBoldTextStyle.copy(
                    fontSize = DIM_FAQ_RULE_QUESTION_FONTSIZE.sp
                )
            )
            if (expandedFaqItem == itemIndex) {
                Spacer(modifier = Modifier.size(10.dp))
                Text(
                    text = stringResource(answer),
                    style = commonJustifiedTextStyle.copy(
                        fontSize = DIM_FAQ_RULE_ANSWER_FONTSIZE.sp
                    )
                )
            }
        }
    }

}
