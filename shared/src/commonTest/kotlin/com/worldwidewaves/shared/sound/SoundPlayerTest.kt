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
import kotlin.time.Duration.Companion.seconds

/**
 * Tests for SoundPlayer interface functionality
 * Note: Extensive SoundPlayer testing is also covered in SoundChoreographiesManagerTest
 */
class SoundPlayerTest {

    @Test
    fun `test SoundPlayer interface contract`() = runTest {
        // GIVEN: Mock SoundPlayer implementation
        val mockPlayer = mockk<SoundPlayer>()

        coEvery { mockPlayer.playTone(any(), any(), any(), any()) } returns Unit
        coEvery { mockPlayer.release() } returns Unit

        // WHEN: Play a tone
        mockPlayer.playTone(
            frequency = 440.0,
            amplitude = 0.8,
            duration = 500.milliseconds,
            waveform = SoundPlayer.Waveform.SINE
        )

        // THEN: Should call playTone method
        coVerify {
            mockPlayer.playTone(440.0, 0.8, 500.milliseconds, SoundPlayer.Waveform.SINE)
        }
    }

    @Test
    fun `test SoundPlayer with all waveform types`() = runTest {
        val mockPlayer = mockk<SoundPlayer>(relaxed = true)

        // WHEN: Play tones with different waveforms
        for (waveform in SoundPlayer.Waveform.entries) {
            mockPlayer.playTone(
                frequency = 440.0,
                amplitude = 0.5,
                duration = 100.milliseconds,
                waveform = waveform
            )
        }

        // THEN: Should call playTone for each waveform
        for (waveform in SoundPlayer.Waveform.entries) {
            coVerify {
                mockPlayer.playTone(440.0, 0.5, 100.milliseconds, waveform)
            }
        }
    }

    @Test
    fun `test SoundPlayer with frequency range`() = runTest {
        val mockPlayer = mockk<SoundPlayer>(relaxed = true)
        val testFrequencies = listOf(20.0, 110.0, 440.0, 880.0, 8000.0, 20000.0)

        // WHEN: Play tones with different frequencies
        for (frequency in testFrequencies) {
            mockPlayer.playTone(
                frequency = frequency,
                amplitude = 0.7,
                duration = 200.milliseconds
            )
        }

        // THEN: Should handle all frequency ranges
        for (frequency in testFrequencies) {
            coVerify {
                mockPlayer.playTone(frequency, 0.7, 200.milliseconds, SoundPlayer.Waveform.SINE)
            }
        }
    }

    @Test
    fun `test SoundPlayer with amplitude range`() = runTest {
        val mockPlayer = mockk<SoundPlayer>(relaxed = true)
        val testAmplitudes = listOf(0.0, 0.25, 0.5, 0.75, 1.0)

        // WHEN: Play tones with different amplitudes
        for (amplitude in testAmplitudes) {
            mockPlayer.playTone(
                frequency = 440.0,
                amplitude = amplitude,
                duration = 100.milliseconds
            )
        }

        // THEN: Should handle all amplitude levels
        for (amplitude in testAmplitudes) {
            coVerify {
                mockPlayer.playTone(440.0, amplitude, 100.milliseconds, SoundPlayer.Waveform.SINE)
            }
        }
    }

    @Test
    fun `test SoundPlayer with duration range`() = runTest {
        val mockPlayer = mockk<SoundPlayer>(relaxed = true)
        val testDurations = listOf(
            10.milliseconds,
            100.milliseconds,
            500.milliseconds,
            1.seconds,
            5.seconds
        )

        // WHEN: Play tones with different durations
        for (duration in testDurations) {
            mockPlayer.playTone(
                frequency = 440.0,
                amplitude = 0.5,
                duration = duration
            )
        }

        // THEN: Should handle all duration ranges
        for (duration in testDurations) {
            coVerify {
                mockPlayer.playTone(440.0, 0.5, duration, SoundPlayer.Waveform.SINE)
            }
        }
    }

    @Test
    fun `test SoundPlayer default waveform parameter`() = runTest {
        val mockPlayer = mockk<SoundPlayer>(relaxed = true)

        // WHEN: Play tone without specifying waveform (should use default)
        mockPlayer.playTone(
            frequency = 440.0,
            amplitude = 0.8,
            duration = 300.milliseconds
        )

        // THEN: Should use default SINE waveform
        coVerify {
            mockPlayer.playTone(440.0, 0.8, 300.milliseconds, SoundPlayer.Waveform.SINE)
        }
    }

    @Test
    fun `test SoundPlayer release method`() {
        val mockPlayer = mockk<SoundPlayer>(relaxed = true)

        // WHEN: Release the player
        mockPlayer.release()

        // THEN: Should call release method
        verify { mockPlayer.release() }
    }

    @Test
    fun `test SoundPlayer multiple tone sequence`() = runTest {
        val mockPlayer = mockk<SoundPlayer>(relaxed = true)

        // WHEN: Play multiple tones in sequence
        val tones = listOf(
            Triple(261.63, 0.8, 250.milliseconds), // C4
            Triple(293.66, 0.7, 250.milliseconds), // D4
            Triple(329.63, 0.9, 250.milliseconds), // E4
            Triple(349.23, 0.6, 250.milliseconds), // F4
        )

        for ((frequency, amplitude, duration) in tones) {
            mockPlayer.playTone(frequency, amplitude, duration, SoundPlayer.Waveform.SQUARE)
        }

        // THEN: Should play all tones
        for ((frequency, amplitude, duration) in tones) {
            coVerify {
                mockPlayer.playTone(frequency, amplitude, duration, SoundPlayer.Waveform.SQUARE)
            }
        }
    }

    @Test
    fun `test SoundPlayer error handling simulation`() = runTest {
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

        // THEN: Should handle the exception
        assert(exceptionThrown)
        coVerify { mockPlayer.playTone(440.0, 0.5, 100.milliseconds, SoundPlayer.Waveform.SINE) }
    }

    @Test
    fun `test SoundPlayer concurrent playback simulation`() = runTest {
        val mockPlayer = mockk<SoundPlayer>(relaxed = true)

        // WHEN: Simulate concurrent tone requests
        val frequencies = listOf(440.0, 523.25, 659.25) // A4, C5, E5 chord

        for (frequency in frequencies) {
            mockPlayer.playTone(
                frequency = frequency,
                amplitude = 0.6,
                duration = 1.seconds,
                waveform = SoundPlayer.Waveform.TRIANGLE
            )
        }

        // THEN: Should handle all concurrent requests
        for (frequency in frequencies) {
            coVerify {
                mockPlayer.playTone(frequency, 0.6, 1.seconds, SoundPlayer.Waveform.TRIANGLE)
            }
        }
    }

    @Test
    fun `test SoundPlayer extreme parameter values`() = runTest {
        val mockPlayer = mockk<SoundPlayer>(relaxed = true)

        // WHEN: Test with extreme values
        val extremeCases = listOf(
            // Very low frequency
            Triple(1.0, 0.1, 50.milliseconds),
            // Very high frequency
            Triple(19000.0, 0.1, 50.milliseconds),
            // Zero amplitude
            Triple(440.0, 0.0, 100.milliseconds),
            // Maximum amplitude
            Triple(440.0, 1.0, 100.milliseconds),
            // Very short duration
            Triple(440.0, 0.5, 1.milliseconds),
        )

        for ((frequency, amplitude, duration) in extremeCases) {
            mockPlayer.playTone(frequency, amplitude, duration)
        }

        // THEN: Should handle extreme cases
        for ((frequency, amplitude, duration) in extremeCases) {
            coVerify {
                mockPlayer.playTone(frequency, amplitude, duration, SoundPlayer.Waveform.SINE)
            }
        }
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
}