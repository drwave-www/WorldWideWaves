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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Comprehensive tests for ChoreographyResources functionality including
 * choreography text loading, sequence management, and error handling.
 */
class ChoreographyResourcesTest {

    @Test
    fun `test getChoreographyText returns correct warming sequences`() {
        // Test all valid warming sequence numbers
        val warmingText1 = getChoreographyText("warming", 1)
        assertEquals(MokoRes.strings.choreography_warming_seq_1, warmingText1)

        val warmingText2 = getChoreographyText("warming", 2)
        assertEquals(MokoRes.strings.choreography_warming_seq_2, warmingText2)

        val warmingText3 = getChoreographyText("warming", 3)
        assertEquals(MokoRes.strings.choreography_warming_seq_3, warmingText3)

        val warmingText4 = getChoreographyText("warming", 4)
        assertEquals(MokoRes.strings.choreography_warming_seq_4, warmingText4)

        val warmingText5 = getChoreographyText("warming", 5)
        assertEquals(MokoRes.strings.choreography_warming_seq_5, warmingText5)

        val warmingText6 = getChoreographyText("warming", 6)
        assertEquals(MokoRes.strings.choreography_warming_seq_6, warmingText6)
    }

    @Test
    fun `test getChoreographyText returns correct waiting text`() {
        val waitingText = getChoreographyText("waiting")
        assertEquals(MokoRes.strings.choreography_waiting, waitingText)

        val waitingTextWithNull = getChoreographyText("waiting", null)
        assertEquals(MokoRes.strings.choreography_waiting, waitingTextWithNull)

        val waitingTextWithNumber = getChoreographyText("waiting", 1)
        assertEquals(MokoRes.strings.choreography_waiting, waitingTextWithNumber)
    }

    @Test
    fun `test getChoreographyText returns correct hit text`() {
        val hitText = getChoreographyText("hit")
        assertEquals(MokoRes.strings.choreography_hit, hitText)

        val hitTextWithNull = getChoreographyText("hit", null)
        assertEquals(MokoRes.strings.choreography_hit, hitTextWithNull)

        val hitTextWithNumber = getChoreographyText("hit", 1)
        assertEquals(MokoRes.strings.choreography_hit, hitTextWithNumber)
    }

    @Test
    fun `test getChoreographyText throws exception for invalid sequence types`() {
        // Test that invalid sequence types throw exceptions
        assertFailsWith<Exception> {
            getChoreographyText("invalid_type", 1)
        }

        assertFailsWith<Exception> {
            getChoreographyText("", 1)
        }

        assertFailsWith<Exception> {
            getChoreographyText("unknown", 3)
        }

        assertFailsWith<Exception> {
            getChoreographyText("finish", 1)
        }
    }

    @Test
    fun `test getChoreographyWarmingText with valid sequence numbers`() {
        // Test direct warming text function
        val seq1 = getChoreographyWarmingText(1)
        assertEquals(MokoRes.strings.choreography_warming_seq_1, seq1)

        val seq2 = getChoreographyWarmingText(2)
        assertEquals(MokoRes.strings.choreography_warming_seq_2, seq2)

        val seq3 = getChoreographyWarmingText(3)
        assertEquals(MokoRes.strings.choreography_warming_seq_3, seq3)

        val seq4 = getChoreographyWarmingText(4)
        assertEquals(MokoRes.strings.choreography_warming_seq_4, seq4)

        val seq5 = getChoreographyWarmingText(5)
        assertEquals(MokoRes.strings.choreography_warming_seq_5, seq5)

        val seq6 = getChoreographyWarmingText(6)
        assertEquals(MokoRes.strings.choreography_warming_seq_6, seq6)
    }

    @Test
    fun `test getChoreographyWarmingText throws exception for invalid sequence numbers`() {
        // Test that invalid sequence numbers throw exceptions
        assertFailsWith<Exception> {
            getChoreographyWarmingText(0)
        }

        assertFailsWith<Exception> {
            getChoreographyWarmingText(-1)
        }

        assertFailsWith<Exception> {
            getChoreographyWarmingText(7)
        }

        assertFailsWith<Exception> {
            getChoreographyWarmingText(10)
        }

        assertFailsWith<Exception> {
            getChoreographyWarmingText(null)
        }
    }

    @Test
    fun `test getChoreographyWaitingText returns correct resource`() {
        val waitingText = getChoreographyWaitingText()
        assertEquals(MokoRes.strings.choreography_waiting, waitingText)
        assertNotNull(waitingText, "Waiting text should not be null")
    }

    @Test
    fun `test getChoreographyHitText returns correct resource`() {
        val hitText = getChoreographyHitText()
        assertEquals(MokoRes.strings.choreography_hit, hitText)
        assertNotNull(hitText, "Hit text should not be null")
    }

    @Test
    fun `test choreography sequence completeness`() {
        // Verify all warming sequences from 1-6 are available
        val warmingSequences = (1..6).map { seqNum ->
            getChoreographyWarmingText(seqNum)
        }

        // All sequences should be unique
        val uniqueSequences = warmingSequences.toSet()
        assertEquals(6, uniqueSequences.size, "All warming sequences should be unique")

        // All sequences should be non-null
        warmingSequences.forEach { sequence ->
            assertNotNull(sequence, "Warming sequence should not be null")
        }
    }

    @Test
    fun `test choreography type consistency`() {
        // Test that different choreography types return distinct resources
        val warmingText = getChoreographyText("warming", 1)
        val waitingText = getChoreographyText("waiting", 1)
        val hitText = getChoreographyText("hit", 1)

        // All should be different resources
        assertTrue(warmingText != waitingText, "Warming and waiting should be different")
        assertTrue(waitingText != hitText, "Waiting and hit should be different")
        assertTrue(warmingText != hitText, "Warming and hit should be different")
    }

    @Test
    fun `test choreography resource loading performance`() {
        val startTime = System.currentTimeMillis()

        // Test loading all choreography resources
        repeat(50) {
            // Load all warming sequences
            for (i in 1..6) {
                getChoreographyText("warming", i)
            }

            // Load waiting and hit texts
            getChoreographyText("waiting")
            getChoreographyText("hit")
        }

        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime

        // Resource loading should be fast (under 500ms for 50 iterations)
        assertTrue(duration < 500, "Choreography resource loading should be fast, took ${duration}ms")
    }

    @Test
    fun `test choreography resource memory consistency`() {
        // Test that repeated calls return the same resource instances
        val warming1a = getChoreographyText("warming", 1)
        val warming1b = getChoreographyText("warming", 1)
        val warming1c = getChoreographyText("warming", 1)

        assertEquals(warming1a, warming1b, "Repeated calls should return same resource")
        assertEquals(warming1b, warming1c, "Repeated calls should return same resource")

        val waitingA = getChoreographyText("waiting")
        val waitingB = getChoreographyText("waiting")
        assertEquals(waitingA, waitingB, "Repeated waiting calls should return same resource")

        val hitA = getChoreographyText("hit")
        val hitB = getChoreographyText("hit")
        assertEquals(hitA, hitB, "Repeated hit calls should return same resource")
    }

    @Test
    fun `test choreography sequence boundaries`() {
        // Test boundary conditions for warming sequences
        val firstSequence = getChoreographyText("warming", 1)
        assertNotNull(firstSequence, "First warming sequence should be available")

        val lastSequence = getChoreographyText("warming", 6)
        assertNotNull(lastSequence, "Last warming sequence should be available")

        // Test that sequences outside valid range throw exceptions
        assertFailsWith<Exception> {
            getChoreographyText("warming", 0)
        }

        assertFailsWith<Exception> {
            getChoreographyText("warming", 7)
        }
    }

    @Test
    fun `test choreography text consistency with direct function calls`() {
        // Verify that getChoreographyText and direct function calls return same resources
        for (i in 1..6) {
            val viaGeneral = getChoreographyText("warming", i)
            val viaDirect = getChoreographyWarmingText(i)
            assertEquals(viaGeneral, viaDirect, "General and direct calls should return same resource for sequence $i")
        }

        val waitingViaGeneral = getChoreographyText("waiting")
        val waitingViaDirect = getChoreographyWaitingText()
        assertEquals(waitingViaGeneral, waitingViaDirect, "General and direct waiting calls should return same resource")

        val hitViaGeneral = getChoreographyText("hit")
        val hitViaDirect = getChoreographyHitText()
        assertEquals(hitViaGeneral, hitViaDirect, "General and direct hit calls should return same resource")
    }

    @Test
    fun `test choreography error message quality`() {
        // Test that error messages are descriptive
        val invalidTypeException = assertFailsWith<Exception> {
            getChoreographyText("invalid_type", 1)
        }
        assertTrue(
            invalidTypeException.message?.contains("Invalid choreography type") == true,
            "Error message should mention invalid choreography type"
        )

        val invalidSeqException = assertFailsWith<Exception> {
            getChoreographyWarmingText(7)
        }
        assertTrue(
            invalidSeqException.message?.contains("Invalid choreography sequence number") == true,
            "Error message should mention invalid sequence number"
        )
    }

    @Test
    fun `test choreography resource thread safety`() {
        // Test concurrent access to choreography resources
        val results = mutableListOf<String>()
        val exceptions = mutableListOf<Exception>()

        val threads = (1..10).map { threadId ->
            Thread {
                try {
                    repeat(20) { iteration ->
                        val seqNum = (iteration % 6) + 1
                        val warmingText = getChoreographyText("warming", seqNum)
                        val waitingText = getChoreographyText("waiting")
                        val hitText = getChoreographyText("hit")

                        results.add("Thread $threadId: warming=$warmingText, waiting=$waitingText, hit=$hitText")
                    }
                } catch (e: Exception) {
                    exceptions.add(e)
                }
            }
        }

        threads.forEach { it.start() }
        threads.forEach { it.join() }

        assertTrue(exceptions.isEmpty(), "No exceptions should occur during concurrent access")
        assertEquals(200, results.size, "All threads should complete successfully")
    }
}