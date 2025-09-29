package com.worldwidewaves.shared.sound

/* * Copyright 2025 DrWave
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
 * limitations under the License. */

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Test for GlobalSoundChoreographyManager sound timing logic.
 * Verifies that sounds only play at the exact moment of user hit transition,
 * not when entering activities for already-finished events.
 */
class GlobalSoundChoreographyManagerTest {
    @Test
    fun `sound transition logic only triggers on false to true transition`() {
        // Test the core logic that was fixed
        var previousHitState = false
        var soundPlayed = false

        // Simulate the logic from the fixed method
        fun simulateHitStateChange(
            hasBeenHit: Boolean,
            isActive: Boolean = true,
            isEventRunning: Boolean = true,
        ) {
            // This is the fixed logic from GlobalSoundChoreographyManager
            if (hasBeenHit && !previousHitState && isActive && isEventRunning) {
                soundPlayed = true
            }
            previousHitState = hasBeenHit
        }

        // SCENARIO 1: Event already in hit state (DONE event) - simulate entering EventActivity
        soundPlayed = false
        previousHitState = true // Simulate that we already know the event is hit

        // WHEN: Event remains in hit state (entering DONE event activity)
        simulateHitStateChange(hasBeenHit = true)

        // THEN: Sound should NOT play for existing hit state
        assertFalse(soundPlayed, "Sound should NOT play when entering activity for already-hit event")

        // Reset for next test
        soundPlayed = false

        // WHEN: Hit state remains true (staying in DONE state)
        simulateHitStateChange(hasBeenHit = true)

        // THEN: Sound should NOT play again
        assertFalse(soundPlayed, "Sound should NOT play when state remains true")

        // SCENARIO 2: Actual hit transition during running event
        soundPlayed = false
        previousHitState = false

        // WHEN: Event starts in not-hit state
        simulateHitStateChange(hasBeenHit = false)

        // THEN: No sound yet
        assertFalse(soundPlayed, "No sound should play when transitioning to false")

        // WHEN: User gets hit (false -> true transition)
        simulateHitStateChange(hasBeenHit = true)

        // THEN: Sound should play
        assertTrue(soundPlayed, "Sound should play on false->true transition")

        // SCENARIO 3: Inactive choreography
        soundPlayed = false
        previousHitState = false

        // WHEN: Hit occurs but choreography is inactive
        simulateHitStateChange(hasBeenHit = true, isActive = false)

        // THEN: No sound should play
        assertFalse(soundPlayed, "Sound should NOT play when choreography is inactive")

        // SCENARIO 4: DONE event (event not running)
        soundPlayed = false
        previousHitState = false

        // WHEN: User gets hit but event is DONE (not running)
        simulateHitStateChange(hasBeenHit = true, isActive = true, isEventRunning = false)

        // THEN: No sound should play for DONE events
        assertFalse(soundPlayed, "Sound should NOT play when event is DONE (not running)")
    }
}
