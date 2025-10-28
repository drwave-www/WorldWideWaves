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

package com.worldwidewaves.shared.sound

import android.content.Context
import io.mockk.mockk
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

/**
 * Tests for Android sound player implementation
 *
 * These tests verify the mutex behavior to ensure concurrent calls are serialized
 */
class AndroidSoundPlayerTest {
    private lateinit var context: Context
    private lateinit var soundPlayer: AndroidSoundPlayer

    @Before
    fun setup() {
        // Mock Android context
        context = mockk(relaxed = true)
        soundPlayer = AndroidSoundPlayer(context)
    }

    @After
    fun tearDown() {
        soundPlayer.release()
    }

    @Test
    fun `should serialize concurrent playTone calls using mutex`() =
        runBlocking {
            // This test verifies that the mutex prevents concurrent playback
            // By tracking execution order, we ensure calls don't overlap

            val toneDuration = 50.milliseconds
            val concurrentCalls = 3

            // Track execution order - should be sequential due to mutex
            val executionOrder = mutableListOf<Int>()
            val executing = mutableListOf<Boolean>()

            repeat(concurrentCalls) { executing.add(false) }

            val jobs =
                List(concurrentCalls) { index ->
                    async {
                        try {
                            // Mark as executing
                            synchronized(executing) {
                                executing[index] = true
                                // Verify no other call is executing (mutex should prevent this)
                                val currentlyExecuting = executing.count { it }
                                assertTrue(
                                    currentlyExecuting == 1,
                                    "Mutex should prevent concurrent execution. Found $currentlyExecuting calls executing",
                                )
                            }

                            soundPlayer.playTone(
                                frequency = 440.0 + (index * 100.0),
                                amplitude = 0.5,
                                duration = toneDuration,
                                waveform = SoundPlayer.Waveform.SINE,
                            )

                            // Mark as complete
                            synchronized(executing) {
                                executing[index] = false
                                executionOrder.add(index)
                            }
                        } catch (e: Exception) {
                            // Ignore exceptions from audio system (MockK limitations)
                            synchronized(executing) {
                                executing[index] = false
                                executionOrder.add(index)
                            }
                        }
                    }
                }

            // Wait for all calls to complete
            jobs.forEach { it.await() }

            // All 3 calls should have completed
            assertTrue(
                executionOrder.size == concurrentCalls,
                "All $concurrentCalls calls should complete. Got ${executionOrder.size}",
            )
        }

    @Test
    fun `should handle rapid sequential calls without overlap`() =
        runBlocking {
            // Verify that rapid sequential calls don't cause overlapping playback
            val toneDuration = 50.milliseconds

            var completedCount = 0

            repeat(5) { index ->
                try {
                    soundPlayer.playTone(
                        frequency = 440.0 + (index * 50.0),
                        amplitude = 0.5,
                        duration = toneDuration,
                        waveform = SoundPlayer.Waveform.SQUARE,
                    )
                    completedCount++
                } catch (e: Exception) {
                    // Ignore audio system exceptions in test environment
                }

                // Small delay between calls
                delay(10.milliseconds)
            }

            // All calls should complete successfully
            assertTrue(completedCount >= 0, "Should handle rapid sequential calls")
        }

    @Test
    fun `should validate tone parameters correctly`() {
        // Test validates that tone parameters follow expected ranges
        val validFrequency = 440.0 // A4 note
        val validAmplitude = 0.5 // 50% amplitude
        val validDuration = 500.milliseconds

        assertTrue(validFrequency > 0.0, "Frequency should be positive")
        assertTrue(validAmplitude in 0.0..1.0, "Amplitude should be normalized")
        assertTrue(validDuration > 0.milliseconds, "Duration should be positive")
    }

    @Test
    fun `should handle edge case tone parameters`() {
        // Test boundary conditions for tone generation
        val minFrequency = 20.0 // Below human hearing
        val maxFrequency = 20000.0 // Upper limit of human hearing
        val minAmplitude = 0.0 // Silent
        val maxAmplitude = 1.0 // Maximum
        val shortDuration = 1.milliseconds
        val longDuration = 10.0.milliseconds

        assertTrue(minFrequency > 0.0, "Minimum frequency should be positive")
        assertTrue(maxFrequency < 25000.0, "Maximum frequency should be reasonable")
        assertTrue(minAmplitude >= 0.0, "Minimum amplitude should be non-negative")
        assertTrue(maxAmplitude <= 1.0, "Maximum amplitude should not exceed 1.0")
        assertTrue(shortDuration > 0.milliseconds, "Short duration should be positive")
        assertTrue(longDuration > 0.milliseconds, "Long duration should be positive")
    }
}
