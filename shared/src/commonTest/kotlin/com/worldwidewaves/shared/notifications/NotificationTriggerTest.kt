package com.worldwidewaves.shared.notifications

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
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Tests for NotificationTrigger sealed class.
 *
 * ## Coverage
 * - Trigger ID generation for all trigger types
 * - EventStarting with different durations
 * - EventFinished and WaveHit singleton behavior
 * - ID uniqueness guarantees
 */
class NotificationTriggerTest {
    // ========================================
    // Test 1: EventStarting - 1 Hour Trigger ID
    // ========================================
    @Test
    fun `EventStarting should generate correct ID for 1 hour duration`() {
        // ARRANGE: 1 hour duration
        val trigger = NotificationTrigger.EventStarting(1.hours)

        // ACT: Get trigger ID
        val id = trigger.id

        // ASSERT: ID should be "start_60m"
        assertEquals("start_60m", id)
    }

    // ========================================
    // Test 2: EventStarting - 30 Minutes Trigger ID
    // ========================================
    @Test
    fun `EventStarting should generate correct ID for 30 minutes duration`() {
        // ARRANGE: 30 minutes duration
        val trigger = NotificationTrigger.EventStarting(30.minutes)

        // ACT: Get trigger ID
        val id = trigger.id

        // ASSERT: ID should be "start_30m"
        assertEquals("start_30m", id)
    }

    // ========================================
    // Test 3: EventStarting - 10 Minutes Trigger ID
    // ========================================
    @Test
    fun `EventStarting should generate correct ID for 10 minutes duration`() {
        // ARRANGE: 10 minutes duration
        val trigger = NotificationTrigger.EventStarting(10.minutes)

        // ACT: Get trigger ID
        val id = trigger.id

        // ASSERT: ID should be "start_10m"
        assertEquals("start_10m", id)
    }

    // ========================================
    // Test 4: EventStarting - 5 Minutes Trigger ID
    // ========================================
    @Test
    fun `EventStarting should generate correct ID for 5 minutes duration`() {
        // ARRANGE: 5 minutes duration
        val trigger = NotificationTrigger.EventStarting(5.minutes)

        // ACT: Get trigger ID
        val id = trigger.id

        // ASSERT: ID should be "start_5m"
        assertEquals("start_5m", id)
    }

    // ========================================
    // Test 5: EventStarting - 1 Minute Trigger ID
    // ========================================
    @Test
    fun `EventStarting should generate correct ID for 1 minute duration`() {
        // ARRANGE: 1 minute duration
        val trigger = NotificationTrigger.EventStarting(1.minutes)

        // ACT: Get trigger ID
        val id = trigger.id

        // ASSERT: ID should be "start_1m"
        assertEquals("start_1m", id)
    }

    // ========================================
    // Test 6: EventFinished - Singleton ID
    // ========================================
    @Test
    fun `EventFinished should have correct ID`() {
        // ARRANGE: EventFinished singleton
        val trigger = NotificationTrigger.EventFinished

        // ACT: Get trigger ID
        val id = trigger.id

        // ASSERT: ID should be "finished"
        assertEquals("finished", id)
    }

    // ========================================
    // Test 7: WaveHit - Singleton ID
    // ========================================
    @Test
    fun `WaveHit should have correct ID`() {
        // ARRANGE: WaveHit singleton
        val trigger = NotificationTrigger.WaveHit

        // ACT: Get trigger ID
        val id = trigger.id

        // ASSERT: ID should be "wave_hit"
        assertEquals("wave_hit", id)
    }

    // ========================================
    // Test 8: EventStarting - Data Class Equality
    // ========================================
    @Test
    fun `EventStarting instances with same duration should be equal`() {
        // ARRANGE: Two triggers with same duration
        val trigger1 = NotificationTrigger.EventStarting(1.hours)
        val trigger2 = NotificationTrigger.EventStarting(1.hours)

        // ACT & ASSERT: Should be equal
        assertEquals(trigger1, trigger2)
        assertEquals(trigger1.id, trigger2.id)
    }

    // ========================================
    // Test 9: EventStarting - Data Class Inequality
    // ========================================
    @Test
    fun `EventStarting instances with different durations should not be equal`() {
        // ARRANGE: Two triggers with different durations
        val trigger1 = NotificationTrigger.EventStarting(1.hours)
        val trigger2 = NotificationTrigger.EventStarting(30.minutes)

        // ACT & ASSERT: Should not be equal
        assert(trigger1 != trigger2)
        assert(trigger1.id != trigger2.id)
    }

    // ========================================
    // Test 10: EventFinished - Singleton Consistency
    // ========================================
    @Test
    fun `EventFinished should always return the same instance`() {
        // ARRANGE: Multiple references to EventFinished
        val trigger1 = NotificationTrigger.EventFinished
        val trigger2 = NotificationTrigger.EventFinished

        // ACT & ASSERT: Should be the same instance
        assert(trigger1 === trigger2)
        assertEquals(trigger1.id, trigger2.id)
    }

    // ========================================
    // Test 11: WaveHit - Singleton Consistency
    // ========================================
    @Test
    fun `WaveHit should always return the same instance`() {
        // ARRANGE: Multiple references to WaveHit
        val trigger1 = NotificationTrigger.WaveHit
        val trigger2 = NotificationTrigger.WaveHit

        // ACT & ASSERT: Should be the same instance
        assert(trigger1 === trigger2)
        assertEquals(trigger1.id, trigger2.id)
    }

    // ========================================
    // Test 12: ID Uniqueness - All Standard Triggers
    // ========================================
    @Test
    fun `all standard notification triggers should have unique IDs`() {
        // ARRANGE: All standard triggers
        val triggers =
            listOf(
                NotificationTrigger.EventStarting(1.hours),
                NotificationTrigger.EventStarting(30.minutes),
                NotificationTrigger.EventStarting(10.minutes),
                NotificationTrigger.EventStarting(5.minutes),
                NotificationTrigger.EventStarting(1.minutes),
                NotificationTrigger.EventFinished,
                NotificationTrigger.WaveHit,
            )

        // ACT: Extract IDs
        val ids = triggers.map { it.id }

        // ASSERT: All IDs should be unique
        assertEquals(ids.size, ids.toSet().size, "All trigger IDs should be unique")
    }

    // ========================================
    // Test 13: EventStarting - Custom Duration
    // ========================================
    @Test
    fun `EventStarting should support custom durations`() {
        // ARRANGE: Custom duration (2 hours)
        val trigger = NotificationTrigger.EventStarting(2.hours)

        // ACT: Get trigger ID
        val id = trigger.id

        // ASSERT: ID should be "start_120m"
        assertEquals("start_120m", id)
    }

    // ========================================
    // Test 14: EventStarting - Duration Property Access
    // ========================================
    @Test
    fun `EventStarting should expose duration property`() {
        // ARRANGE: Trigger with specific duration
        val duration = 45.minutes
        val trigger = NotificationTrigger.EventStarting(duration)

        // ACT: Access duration property
        val exposedDuration = trigger.duration

        // ASSERT: Duration should match
        assertEquals(duration, exposedDuration)
    }
}
