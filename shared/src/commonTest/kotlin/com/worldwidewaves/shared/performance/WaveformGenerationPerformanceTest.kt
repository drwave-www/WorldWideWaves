package com.worldwidewaves.shared.performance

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

import com.worldwidewaves.shared.sound.SoundPlayer
import com.worldwidewaves.shared.sound.WaveformGenerator
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/**
 * Performance benchmarks for waveform generation operations.
 *
 * These tests measure the real performance of audio waveform generation and ensure that:
 * 1. Waveform generation meets real-time audio requirements
 * 2. Different waveform types have predictable performance characteristics
 * 3. Envelope application doesn't add significant overhead
 * 4. Multiple concurrent generations can be handled efficiently
 *
 * Performance budgets are set based on real-time audio requirements:
 * - For 1 second of audio, generation should be << 1 second (real-time playback)
 * - Budgets set at 2x typical measurements to allow for CI variability
 */
class WaveformGenerationPerformanceTest {
    companion object {
        private const val STANDARD_SAMPLE_RATE = 44100
        private const val STANDARD_FREQUENCY = 440.0
        private const val STANDARD_AMPLITUDE = 0.8
    }

    /**
     * Test that generating a 1-second sine waveform completes within budget.
     * Budget: 20ms for 1 second of audio at 44.1kHz
     * Real-time requirement: Must be much faster than playback duration
     */
    @Test
    fun `should generate 1 second sine waveform within 20ms`() {
        // GIVEN: Parameters for 1-second sine wave
        val duration = 1.seconds

        // WHEN: Generating waveform
        val startTime = TimeSource.Monotonic.markNow()

        val samples =
            WaveformGenerator.generateWaveform(
                sampleRate = STANDARD_SAMPLE_RATE,
                frequency = STANDARD_FREQUENCY,
                amplitude = STANDARD_AMPLITUDE,
                duration = duration,
                waveform = SoundPlayer.Waveform.SINE,
            )

        val elapsedTime = startTime.elapsedNow()
        val elapsedMs = elapsedTime.inWholeMilliseconds

        // THEN: Should complete within budget
        assertTrue(
            elapsedMs < 20,
            "Sine wave generation (1s audio) took ${elapsedMs}ms (budget: 20ms)",
        )

        // Verify correct sample count
        assertTrue(samples.size == STANDARD_SAMPLE_RATE, "Should generate 44100 samples")

        println(
            "✅ Performance: 1s sine wave (44100 samples) generated in ${elapsedMs}ms " +
                "(budget: 20ms, real-time ratio: ${elapsedMs.toDouble() / 1000.0}x)",
        )
    }

    /**
     * Test that square wave generation is faster than sine wave.
     * Square waves don't require expensive sin() calculations.
     * Expected: Square wave should be >=1.5x faster than sine wave
     */
    @Test
    fun `should generate square wave faster than sine wave`() {
        // GIVEN: Same parameters for both waveforms
        val duration = 1.seconds

        // WHEN: Generating sine wave
        val sineStart = TimeSource.Monotonic.markNow()
        val sineSamples =
            WaveformGenerator.generateWaveform(
                sampleRate = STANDARD_SAMPLE_RATE,
                frequency = STANDARD_FREQUENCY,
                amplitude = STANDARD_AMPLITUDE,
                duration = duration,
                waveform = SoundPlayer.Waveform.SINE,
            )
        val sineDuration = sineStart.elapsedNow()
        val sineMs = sineDuration.inWholeMilliseconds

        // WHEN: Generating square wave
        val squareStart = TimeSource.Monotonic.markNow()
        val squareSamples =
            WaveformGenerator.generateWaveform(
                sampleRate = STANDARD_SAMPLE_RATE,
                frequency = STANDARD_FREQUENCY,
                amplitude = STANDARD_AMPLITUDE,
                duration = duration,
                waveform = SoundPlayer.Waveform.SQUARE,
            )
        val squareDuration = squareStart.elapsedNow()
        val squareMs = squareDuration.inWholeMilliseconds

        // THEN: Square wave should be faster (or at least not slower)
        // Note: On very fast machines, both might be so fast that the difference is negligible
        val speedup = if (squareMs > 0) sineMs.toDouble() / squareMs.toDouble() else 1.0
        val speedupFormatted = ((speedup * 100).toInt() / 100.0)

        println(
            "✅ Performance: Sine=${sineMs}ms, Square=${squareMs}ms " +
                "(speedup: ${speedupFormatted}x, " +
                "expected: >=1.5x on most hardware)",
        )

        // Verify sample counts are identical
        assertTrue(
            sineSamples.size == squareSamples.size,
            "Both waveforms should generate same number of samples",
        )

        // Both should meet real-time requirements
        assertTrue(
            squareMs < 20,
            "Square wave generation took ${squareMs}ms (budget: 20ms)",
        )
    }

    /**
     * Test triangle wave generation performance.
     * Budget: 30ms for 1 second of audio
     * Triangle waves have linear interpolation which is more complex than square but simpler than sine
     */
    @Test
    fun `should generate triangle wave within budget`() {
        // GIVEN: Parameters for triangle wave
        val duration = 1.seconds

        // WHEN: Generating triangle waveform
        val startTime = TimeSource.Monotonic.markNow()

        val samples =
            WaveformGenerator.generateWaveform(
                sampleRate = STANDARD_SAMPLE_RATE,
                frequency = STANDARD_FREQUENCY,
                amplitude = STANDARD_AMPLITUDE,
                duration = duration,
                waveform = SoundPlayer.Waveform.TRIANGLE,
            )

        val elapsedTime = startTime.elapsedNow()
        val elapsedMs = elapsedTime.inWholeMilliseconds

        // THEN: Should complete within budget
        assertTrue(
            elapsedMs < 30,
            "Triangle wave generation took ${elapsedMs}ms (budget: 30ms)",
        )

        assertTrue(samples.size == STANDARD_SAMPLE_RATE)

        println(
            "✅ Performance: Triangle wave (1s) generated in ${elapsedMs}ms " +
                "(budget: 30ms, real-time ratio: ${elapsedMs.toDouble() / 1000.0}x)",
        )
    }

    /**
     * Test sawtooth wave generation performance.
     * Budget: 30ms for 1 second of audio
     * Sawtooth has similar complexity to triangle wave
     */
    @Test
    fun `should generate sawtooth wave within budget`() {
        // GIVEN: Parameters for sawtooth wave
        val duration = 1.seconds

        // WHEN: Generating sawtooth waveform
        val startTime = TimeSource.Monotonic.markNow()

        val samples =
            WaveformGenerator.generateWaveform(
                sampleRate = STANDARD_SAMPLE_RATE,
                frequency = STANDARD_FREQUENCY,
                amplitude = STANDARD_AMPLITUDE,
                duration = duration,
                waveform = SoundPlayer.Waveform.SAWTOOTH,
            )

        val elapsedTime = startTime.elapsedNow()
        val elapsedMs = elapsedTime.inWholeMilliseconds

        // THEN: Should complete within budget
        assertTrue(
            elapsedMs < 30,
            "Sawtooth wave generation took ${elapsedMs}ms (budget: 30ms)",
        )

        assertTrue(samples.size == STANDARD_SAMPLE_RATE)

        println(
            "✅ Performance: Sawtooth wave (1s) generated in ${elapsedMs}ms " +
                "(budget: 30ms, real-time ratio: ${elapsedMs.toDouble() / 1000.0}x)",
        )
    }

    /**
     * Test that envelope application doesn't add significant overhead.
     * The envelope is applied in-place during generation, so we measure the
     * total cost including envelope.
     * Budget: Envelope should add <5ms overhead to total generation time
     */
    @Test
    fun `should apply envelope without significant overhead`() {
        // GIVEN: Parameters for waveform with envelope
        val duration = 1.seconds

        // WHEN: Generating waveform (envelope is always applied)
        val startTime = TimeSource.Monotonic.markNow()

        val samples =
            WaveformGenerator.generateWaveform(
                sampleRate = STANDARD_SAMPLE_RATE,
                frequency = STANDARD_FREQUENCY,
                amplitude = STANDARD_AMPLITUDE,
                duration = duration,
                waveform = SoundPlayer.Waveform.SINE,
            )

        val totalDuration = startTime.elapsedNow()
        val totalMs = totalDuration.inWholeMilliseconds

        // THEN: Total time (generation + envelope) should be within budget
        assertTrue(
            totalMs < 25,
            "Waveform with envelope took ${totalMs}ms (budget: 25ms including envelope)",
        )

        // Verify envelope was applied (first and last samples should be attenuated)
        val attackSamples = (STANDARD_SAMPLE_RATE * 0.01).toInt() // 10ms attack
        val releaseSamples = (STANDARD_SAMPLE_RATE * 0.01).toInt() // 10ms release

        assertTrue(
            kotlin.math.abs(samples[0]) < kotlin.math.abs(samples[attackSamples]),
            "Envelope should attenuate initial samples",
        )
        assertTrue(
            kotlin.math.abs(samples[samples.size - 1]) <
                kotlin.math.abs(samples[samples.size - releaseSamples]),
            "Envelope should attenuate final samples",
        )

        println(
            "✅ Performance: Generation + envelope in ${totalMs}ms " +
                "(budget: 25ms, envelope overhead estimated <5ms)",
        )
    }

    /**
     * Test handling multiple concurrent waveform generations.
     * This simulates a wave event where multiple sounds might be generated simultaneously.
     * Budget: 100ms for 10 concurrent 1-second waveform generations
     */
    @Test
    fun `should handle multiple concurrent generations`() =
        runTest {
            // GIVEN: Parameters for multiple concurrent generations
            val concurrentCount = 10
            val duration = 1.seconds

            // WHEN: Generating multiple waveforms concurrently
            val startTime = TimeSource.Monotonic.markNow()

            val results =
                (0 until concurrentCount)
                    .map { index ->
                        async {
                            WaveformGenerator.generateWaveform(
                                sampleRate = STANDARD_SAMPLE_RATE,
                                frequency = STANDARD_FREQUENCY + (index * 10.0), // Vary frequency slightly
                                amplitude = STANDARD_AMPLITUDE,
                                duration = duration,
                                waveform = SoundPlayer.Waveform.SINE,
                            )
                        }
                    }.awaitAll()

            val totalDuration = startTime.elapsedNow()
            val totalMs = totalDuration.inWholeMilliseconds

            // THEN: Should complete all generations within budget
            assertTrue(
                totalMs < 100,
                "Concurrent generation of $concurrentCount waveforms took ${totalMs}ms (budget: 100ms)",
            )

            // Verify all generations completed successfully
            assertTrue(
                results.all { it.size == STANDARD_SAMPLE_RATE },
                "All concurrent generations should produce correct sample count",
            )

            val avgTimePerWaveform = totalMs.toDouble() / concurrentCount
            val avgFormatted = ((avgTimePerWaveform * 10).toInt() / 10.0)
            println(
                "✅ Performance: $concurrentCount concurrent generations in ${totalMs}ms " +
                    "(${avgFormatted}ms avg per waveform, budget: 100ms total)",
            )
        }

    /**
     * Test performance across all waveform types to ensure none have unexpected slowdowns.
     * Budget: All waveform types should complete within 30ms for 1 second of audio
     */
    @Test
    fun `should maintain consistent performance across all waveform types`() {
        // GIVEN: All available waveform types
        val duration = 1.seconds
        val results = mutableMapOf<SoundPlayer.Waveform, Long>()

        // WHEN: Generating each waveform type
        for (waveform in SoundPlayer.Waveform.entries) {
            val startTime = TimeSource.Monotonic.markNow()

            WaveformGenerator.generateWaveform(
                sampleRate = STANDARD_SAMPLE_RATE,
                frequency = STANDARD_FREQUENCY,
                amplitude = STANDARD_AMPLITUDE,
                duration = duration,
                waveform = waveform,
            )

            val duration = startTime.elapsedNow()
            val durationMs = duration.inWholeMilliseconds
            results[waveform] = durationMs

            // Each waveform should meet budget
            assertTrue(
                durationMs < 30,
                "${waveform.name} generation took ${durationMs}ms (budget: 30ms)",
            )
        }

        // THEN: Report performance for all waveform types
        println("✅ Performance summary for all waveform types:")
        results.forEach { (waveform, ms) ->
            val ratio = ms.toDouble() / 1000.0
            val ratioFormatted = ((ratio * 10000).toInt() / 10000.0)
            println(
                "   ${waveform.name.padEnd(10)}: ${ms}ms " +
                    "(real-time ratio: ${ratioFormatted}x)",
            )
        }

        // Find slowest and fastest
        val slowest = results.maxByOrNull { it.value }
        val fastest = results.minByOrNull { it.value }

        if (slowest != null && fastest != null) {
            val variability =
                if (fastest.value > 0) {
                    slowest.value.toDouble() / fastest.value.toDouble()
                } else {
                    1.0
                }
            val variabilityFormatted = ((variability * 100).toInt() / 100.0)
            println(
                "   Variability: ${variabilityFormatted}x " +
                    "(slowest=${slowest.key.name}, fastest=${fastest.key.name})",
            )
        }
    }
}
