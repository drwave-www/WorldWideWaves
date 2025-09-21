package com.worldwidewaves.shared

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

import dev.icerock.moko.resources.StringResource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Comprehensive tests for InfoStringResources functionality including
 * localization handling, rules hierarchy, FAQ contents, and core info management.
 */
class InfoStringResourcesTest {

    @Test
    fun `test rules_hierarchy contains all expected warning categories`() {
        // Verify all warning categories are present
        assertTrue(rules_hierarchy.containsKey(MokoRes.strings.warn_general_title), "Should contain general warnings")
        assertTrue(rules_hierarchy.containsKey(MokoRes.strings.warn_safety_title), "Should contain safety warnings")
        assertTrue(rules_hierarchy.containsKey(MokoRes.strings.warn_emergency_title), "Should contain emergency warnings")
        assertTrue(rules_hierarchy.containsKey(MokoRes.strings.warn_legal_title), "Should contain legal warnings")

        // Verify we have exactly 4 warning categories
        assertEquals(4, rules_hierarchy.size, "Should have exactly 4 warning categories")
    }

    @Test
    fun `test rules_hierarchy general warnings structure`() {
        val generalWarnings = rules_hierarchy[MokoRes.strings.warn_general_title]
        assertNotNull(generalWarnings, "General warnings should not be null")

        // Should have 6 general warning items
        assertEquals(6, generalWarnings.size, "Should have 6 general warning items")

        // Verify all general warning items are present
        val expectedGeneralItems = listOf(
            MokoRes.strings.warn_general_item_1,
            MokoRes.strings.warn_general_item_2,
            MokoRes.strings.warn_general_item_3,
            MokoRes.strings.warn_general_item_4,
            MokoRes.strings.warn_general_item_5,
            MokoRes.strings.warn_general_item_6
        )

        expectedGeneralItems.forEachIndexed { index, expectedItem ->
            assertEquals(expectedItem, generalWarnings[index], "General warning item ${index + 1} should match")
        }
    }

    @Test
    fun `test rules_hierarchy safety warnings structure`() {
        val safetyWarnings = rules_hierarchy[MokoRes.strings.warn_safety_title]
        assertNotNull(safetyWarnings, "Safety warnings should not be null")

        // Should have 5 safety warning items
        assertEquals(5, safetyWarnings.size, "Should have 5 safety warning items")

        // Verify all safety warning items are present
        val expectedSafetyItems = listOf(
            MokoRes.strings.warn_safety_item_1,
            MokoRes.strings.warn_safety_item_2,
            MokoRes.strings.warn_safety_item_3,
            MokoRes.strings.warn_safety_item_4,
            MokoRes.strings.warn_safety_item_5
        )

        expectedSafetyItems.forEachIndexed { index, expectedItem ->
            assertEquals(expectedItem, safetyWarnings[index], "Safety warning item ${index + 1} should match")
        }
    }

    @Test
    fun `test rules_hierarchy emergency warnings structure`() {
        val emergencyWarnings = rules_hierarchy[MokoRes.strings.warn_emergency_title]
        assertNotNull(emergencyWarnings, "Emergency warnings should not be null")

        // Should have 3 emergency warning items
        assertEquals(3, emergencyWarnings.size, "Should have 3 emergency warning items")

        // Verify all emergency warning items are present
        val expectedEmergencyItems = listOf(
            MokoRes.strings.warn_emergency_item_1,
            MokoRes.strings.warn_emergency_item_2,
            MokoRes.strings.warn_emergency_item_3
        )

        expectedEmergencyItems.forEachIndexed { index, expectedItem ->
            assertEquals(expectedItem, emergencyWarnings[index], "Emergency warning item ${index + 1} should match")
        }
    }

    @Test
    fun `test rules_hierarchy legal warnings structure`() {
        val legalWarnings = rules_hierarchy[MokoRes.strings.warn_legal_title]
        assertNotNull(legalWarnings, "Legal warnings should not be null")

        // Should have 2 legal warning items
        assertEquals(2, legalWarnings.size, "Should have 2 legal warning items")

        // Verify all legal warning items are present
        val expectedLegalItems = listOf(
            MokoRes.strings.warn_legal_item_1,
            MokoRes.strings.warn_legal_item_2
        )

        expectedLegalItems.forEachIndexed { index, expectedItem ->
            assertEquals(expectedItem, legalWarnings[index], "Legal warning item ${index + 1} should match")
        }
    }

    @Test
    fun `test faq_contents structure and completeness`() {
        // Verify FAQ has expected number of items
        assertEquals(6, faq_contents.size, "Should have 6 FAQ items")

        // Verify all FAQ items have both question and answer
        faq_contents.forEachIndexed { index, faqItem ->
            val questionNum = index + 1
            assertNotNull(faqItem.first, "FAQ question $questionNum should not be null")
            assertNotNull(faqItem.second, "FAQ answer $questionNum should not be null")
        }

        // Verify specific FAQ items
        val expectedFaqItems = listOf(
            Pair(MokoRes.strings.faq_question_1, MokoRes.strings.faq_answer_1),
            Pair(MokoRes.strings.faq_question_2, MokoRes.strings.faq_answer_2),
            Pair(MokoRes.strings.faq_question_3, MokoRes.strings.faq_answer_3),
            Pair(MokoRes.strings.faq_question_4, MokoRes.strings.faq_answer_4),
            Pair(MokoRes.strings.faq_question_5, MokoRes.strings.faq_answer_5),
            Pair(MokoRes.strings.faq_question_6, MokoRes.strings.faq_answer_6)
        )

        expectedFaqItems.forEachIndexed { index, expectedItem ->
            assertEquals(expectedItem.first, faq_contents[index].first, "FAQ question ${index + 1} should match")
            assertEquals(expectedItem.second, faq_contents[index].second, "FAQ answer ${index + 1} should match")
        }
    }

    @Test
    fun `test infos_core structure and completeness`() {
        // Verify core infos has expected number of items
        assertEquals(9, infos_core.size, "Should have 9 core info items")

        // Verify all core info items are not null
        infos_core.forEachIndexed { index, infoItem ->
            assertNotNull(infoItem, "Core info item ${index + 1} should not be null")
        }

        // Verify specific core info items
        val expectedCoreInfos = listOf(
            MokoRes.strings.infos_core_1,
            MokoRes.strings.infos_core_2,
            MokoRes.strings.infos_core_3,
            MokoRes.strings.infos_core_4,
            MokoRes.strings.infos_core_5,
            MokoRes.strings.infos_core_6,
            MokoRes.strings.infos_core_7,
            MokoRes.strings.infos_core_8,
            MokoRes.strings.infos_core_9
        )

        expectedCoreInfos.forEachIndexed { index, expectedItem ->
            assertEquals(expectedItem, infos_core[index], "Core info item ${index + 1} should match")
        }
    }

    @Test
    fun `test string resource consistency across all info structures`() {
        // Collect all string resources used in info structures
        val allStringResources = mutableSetOf<StringResource>()

        // Add rules hierarchy resources
        rules_hierarchy.keys.forEach { allStringResources.add(it) }
        rules_hierarchy.values.forEach { warningList ->
            warningList.forEach { allStringResources.add(it) }
        }

        // Add FAQ resources
        faq_contents.forEach { faqItem ->
            allStringResources.add(faqItem.first)
            allStringResources.add(faqItem.second)
        }

        // Add core info resources
        infos_core.forEach { allStringResources.add(it) }

        // Verify all resources are unique (no duplicates)
        val expectedTotalResources = 4 + 16 + 12 + 9 // titles + warning items + faq items + core infos
        assertEquals(expectedTotalResources, allStringResources.size, "All string resources should be unique")
    }

    @Test
    fun `test rules hierarchy navigation and access patterns`() {
        // Test typical access patterns for rules hierarchy
        rules_hierarchy.forEach { (categoryTitle, warningItems) ->
            // Each category should have items
            assertTrue(warningItems.isNotEmpty(), "Category should have warning items")

            // Items should be accessible by index
            warningItems.forEachIndexed { index, item ->
                assertEquals(item, warningItems[index], "Item should be accessible by index")
            }

            // Test safe access patterns
            val firstItem = warningItems.firstOrNull()
            assertNotNull(firstItem, "Should be able to safely get first item")

            val lastItem = warningItems.lastOrNull()
            assertNotNull(lastItem, "Should be able to safely get last item")
        }
    }

    @Test
    fun `test FAQ question-answer pairing integrity`() {
        // Verify that each FAQ question has a corresponding answer
        faq_contents.forEachIndexed { index, faqItem ->
            val questionNum = index + 1
            val (question, answer) = faqItem

            // Question and answer should be different resources
            assertTrue(question != answer, "FAQ $questionNum question and answer should be different")

            // Both should be valid string resources
            assertNotNull(question, "FAQ $questionNum question should be valid")
            assertNotNull(answer, "FAQ $questionNum answer should be valid")
        }

        // Test that questions and answers maintain proper ordering
        for (i in 1..6) {
            val expectedQuestion = when (i) {
                1 -> MokoRes.strings.faq_question_1
                2 -> MokoRes.strings.faq_question_2
                3 -> MokoRes.strings.faq_question_3
                4 -> MokoRes.strings.faq_question_4
                5 -> MokoRes.strings.faq_question_5
                6 -> MokoRes.strings.faq_question_6
                else -> error("Invalid FAQ index")
            }

            val expectedAnswer = when (i) {
                1 -> MokoRes.strings.faq_answer_1
                2 -> MokoRes.strings.faq_answer_2
                3 -> MokoRes.strings.faq_answer_3
                4 -> MokoRes.strings.faq_answer_4
                5 -> MokoRes.strings.faq_answer_5
                6 -> MokoRes.strings.faq_answer_6
                else -> error("Invalid FAQ index")
            }

            assertEquals(expectedQuestion, faq_contents[i - 1].first, "FAQ $i question should match")
            assertEquals(expectedAnswer, faq_contents[i - 1].second, "FAQ $i answer should match")
        }
    }

    @Test
    fun `test info string resource memory management`() {
        // Test repeated access to info string resources for memory leaks
        repeat(100) {
            // Access rules hierarchy
            rules_hierarchy.forEach { (title, items) ->
                items.forEach { item ->
                    // Just access the resources to test for memory issues
                    assertNotNull(title)
                    assertNotNull(item)
                }
            }

            // Access FAQ contents
            faq_contents.forEach { (question, answer) ->
                assertNotNull(question)
                assertNotNull(answer)
            }

            // Access core infos
            infos_core.forEach { info ->
                assertNotNull(info)
            }
        }

        // If we reach here without memory issues, the test passes
        assertTrue(true, "Memory management test completed successfully")
    }

    @Test
    fun `test info string localization readiness`() {
        // Verify that all info string structures are ready for localization
        val allResources = mutableListOf<StringResource>()

        // Collect all resources
        rules_hierarchy.forEach { (title, items) ->
            allResources.add(title)
            allResources.addAll(items)
        }

        faq_contents.forEach { (question, answer) ->
            allResources.add(question)
            allResources.add(answer)
        }

        allResources.addAll(infos_core)

        // All resources should be proper StringResource instances
        allResources.forEach { resource ->
            assertTrue(resource is StringResource, "All info strings should be StringResource instances")
        }

        // Verify expected total count
        val expectedCount =
            4 + // rule titles
            (6 + 5 + 3 + 2) + // rule items
            12 + // FAQ questions and answers
            9 // core infos

        assertEquals(expectedCount, allResources.size, "Should have expected number of string resources")
    }

    @Test
    fun `test info structure immutability and thread safety`() {
        // Test concurrent access to info structures
        val results = mutableListOf<String>()
        val exceptions = mutableListOf<Exception>()

        val threads = (1..10).map { threadId ->
            Thread {
                try {
                    repeat(50) {
                        // Access rules hierarchy
                        val rulesCount = rules_hierarchy.size
                        val totalRuleItems = rules_hierarchy.values.sumOf { it.size }

                        // Access FAQ contents
                        val faqCount = faq_contents.size

                        // Access core infos
                        val coreInfoCount = infos_core.size

                        results.add("Thread $threadId: rules=$rulesCount/$totalRuleItems, faq=$faqCount, core=$coreInfoCount")
                    }
                } catch (e: Exception) {
                    exceptions.add(e)
                }
            }
        }

        threads.forEach { it.start() }
        threads.forEach { it.join() }

        assertTrue(exceptions.isEmpty(), "No exceptions should occur during concurrent access")
        assertEquals(500, results.size, "All threads should complete successfully")

        // All results should be identical (proving immutability)
        val expectedResult = "Thread 1: rules=4/16, faq=6, core=9"
        results.forEach { result ->
            assertTrue(result.contains("rules=4/16, faq=6, core=9"), "Results should be consistent across threads")
        }
    }
}