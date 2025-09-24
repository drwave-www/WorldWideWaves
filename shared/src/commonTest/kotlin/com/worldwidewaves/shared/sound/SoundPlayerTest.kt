package com.worldwidewaves.shared.sound

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

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds

/**
 * Focused tests for SoundPlayer interface contract and basic functionality.
 *
 * Note: Comprehensive SoundPlayer behavior testing is covered in:
 * - SoundChoreographiesManagerTest (integration and behavior testing)
 * - WaveformGeneratorTest (waveform generation testing)
 * - InputValidationTest (parameter validation testing)
 *
 * This test focuses on essential interface contracts to avoid over-testing.
 */
class SoundPlayerTest {
    @Test
    fun `test SoundPlayer interface contract`() =
        runTest {
            // GIVEN: Mock SoundPlayer implementation
            val mockPlayer = mockk<SoundPlayer>(relaxed = true)

            // WHEN: Play a tone with all parameters
            mockPlayer.playTone(
                frequency = 440.0,
                amplitude = 0.8,
                duration = 500.milliseconds,
                waveform = SoundPlayer.Waveform.SINE,
            )

            // THEN: Interface should accept valid parameters without error
            coVerify(exactly = 1) {
                mockPlayer.playTone(440.0, 0.8, 500.milliseconds, SoundPlayer.Waveform.SINE)
            }
        }

    @Test
    fun `test SoundPlayer default waveform parameter`() =
        runTest {
            val mockPlayer = mockk<SoundPlayer>(relaxed = true)

            // WHEN: Play tone without specifying waveform (should use default)
            mockPlayer.playTone(
                frequency = 440.0,
                amplitude = 0.8,
                duration = 300.milliseconds,
            )

            // THEN: Should use default SINE waveform
            coVerify(exactly = 1) {
                mockPlayer.playTone(440.0, 0.8, 300.milliseconds, SoundPlayer.Waveform.SINE)
            }
        }

    @Test
    fun `test SoundPlayer release method`() {
        val mockPlayer = mockk<SoundPlayer>(relaxed = true)

        // WHEN: Release the player
        mockPlayer.release()

        // THEN: Should call release method
        verify(exactly = 1) { mockPlayer.release() }
    }

    @Test
    fun `test SoundPlayer waveform enum completeness`() {
        // GIVEN: All available waveforms
        val waveforms = SoundPlayer.Waveform.entries

        // THEN: Should have expected waveform types
        assert(waveforms.contains(SoundPlayer.Waveform.SINE))
        assert(waveforms.contains(SoundPlayer.Waveform.SQUARE))
        assert(waveforms.contains(SoundPlayer.Waveform.TRIANGLE))
        assert(waveforms.contains(SoundPlayer.Waveform.SAWTOOTH))

        // Should have exactly 4 waveform types
        assert(waveforms.size == 4)
    }

    @Test
    fun `test SoundPlayer error handling simulation`() =
        runTest {
            val mockPlayer = mockk<SoundPlayer>()

            // GIVEN: Player that throws exception
            coEvery {
                mockPlayer.playTone(any(), any(), any(), any())
            } throws RuntimeException("Audio system error")

            // WHEN: Try to play tone
            var exceptionThrown = false
            try {
                mockPlayer.playTone(440.0, 0.5, 100.milliseconds)
            } catch (e: RuntimeException) {
                exceptionThrown = true
            }

            // THEN: Should handle the exception appropriately
            assert(exceptionThrown)
            coVerify(exactly = 1) {
                mockPlayer.playTone(440.0, 0.5, 100.milliseconds, SoundPlayer.Waveform.SINE)
            }
        }
}
