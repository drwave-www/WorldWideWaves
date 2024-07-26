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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.worldwidewaves.activities.utils.TabScreen
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
import com.worldwidewaves.theme.displayFontFamily
import com.worldwidewaves.theme.extraFontFamily
import com.worldwidewaves.theme.quinaryLight
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
        var scrollToFAQPosition  by remember { mutableStateOf(0F) }

        var expandedFaqItem by remember { mutableStateOf(-1) }

        Box(modifier = modifier) {
            Column(
                modifier = Modifier
                    .padding(start = 20.dp, end = 20.dp)
                    .fillMaxSize()
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // WWW Logo
                AboutWWWLogo()

                // Main FAQ text - Rules & Security
                FAQTitle {
                    // Scroll to the top of the FAQ section
                    coroutineScope.launch {
                        scrollState.animateScrollTo(scrollToFAQPosition.roundToInt())
                    }
                }

                // Divider line
                AboutDividerLine()

                // For each rules_hierarchy entry, display the title and the list of items
                ShowRulesHierarchy()

                // Divider line
                AboutDividerLine()

                // FAQ title
                Spacer(modifier = Modifier.size(10.dp))
                Text(
                    modifier = Modifier.onGloballyPositioned { coordinates ->
                        // Save the position of the FAQ section
                        scrollToFAQPosition = coordinates.positionInRoot().y
                    },
                    text = stringResource(ShRes.string.faq),
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = extraFontFamily
                )
                Spacer(modifier = Modifier.size(30.dp))

                // FAQ Items
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(), color = Color.White, thickness = 2.dp
                )
                faq_contents.forEachIndexed { index, (question, answer) ->
                    FAQItem(index, question, answer,
                            expandedFaqItem,
                            onExpand = { expandedFaqItem = it })
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(), color = Color.White, thickness = 2.dp
                    )
                }

                // WWW Social Networks
                Spacer(modifier = Modifier.size(20.dp))
                AboutDividerLine()
                AboutWWWSocialNetworks()
            }
        }
    }

    // ----------------------------

    @Composable
    private fun FAQTitle(
        scrollToFAQPosition: () -> Unit
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                modifier = Modifier.fillMaxWidth(0.5f),
                fontSize = 16.sp, fontFamily = extraFontFamily,
                textAlign = TextAlign.Start,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                text = stringResource(ShRes.string.warn_rules_security_title)
            )
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = scrollToFAQPosition),
                fontSize = 16.sp, fontFamily = displayFontFamily,
                textAlign = TextAlign.End,
                fontWeight = FontWeight.Black,
                color = quinaryLight,
                text = stringResource(ShRes.string.faq_access),
                textDecoration = TextDecoration.Underline
            )
        }
        Spacer(modifier = Modifier.size(20.dp))
        Text(
            text = stringResource(ShRes.string.warn_rules_security_text),
            fontSize = 18.sp,
            style = TextStyle(textAlign = TextAlign.Justify),
            fontFamily = displayFontFamily
        )
    }

    // ----------------------------

    @Composable
    private fun ShowRulesHierarchy() {
        rules_hierarchy.forEach { (title, items) ->
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(title),
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Black,
                fontFamily = extraFontFamily,
                textAlign = TextAlign.Start
            )
            Spacer(modifier = Modifier.size(10.dp))
            items.forEachIndexed { index, item ->
                Row(modifier = Modifier.padding(bottom = 5.dp)) {
                    Text(
                        modifier = Modifier.width(20.dp),
                        text = (index + 1).toString() + ".",
                        fontSize = 12.sp,
                        fontFamily = displayFontFamily,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        modifier = Modifier.padding(start = 10.dp),
                        text = stringResource(item),
                        fontSize = 12.sp,
                        style = TextStyle(textAlign = TextAlign.Justify),
                        fontFamily = displayFontFamily
                    )
                }
            }
            Spacer(modifier = Modifier.size(20.dp))
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
            .padding(10.dp)
            .clickable {
                onExpand(if (expandedFaqItem == itemIndex) -1 else itemIndex)
            }
        ) {
            Text(
                text = stringResource(question),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = displayFontFamily,
                color = MaterialTheme.colorScheme.primary
            )
            if (expandedFaqItem == itemIndex) {
                Spacer(modifier = Modifier.size(10.dp))
                Text(
                    text = stringResource(answer),
                    fontSize = 16.sp,
                    fontFamily = displayFontFamily,
                    style = TextStyle(textAlign = TextAlign.Justify)
                )
            }
        }
    }

}
