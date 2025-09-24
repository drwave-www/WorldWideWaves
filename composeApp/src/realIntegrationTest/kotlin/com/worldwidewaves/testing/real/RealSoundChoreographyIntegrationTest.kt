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

package com.worldwidewaves.testing.real

import androidx.test.filters.LargeTest
import com.worldwidewaves.shared.sound.SoundPlayer
import com.worldwidewaves.shared.sound.WaveformGenerator
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.math.abs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

/**
 * Real integration tests for Sound Choreography functionality.
 *
 * These tests validate the core sound coordination features that make WorldWideWaves unique:
 * - Sound timing accuracy for wave synchronization
 * - Audio choreography sequence execution during wave events
 * - Sound coordination between multiple simulated participants
 * - Audio performance under load scenarios
 *
 * CRITICAL: These tests validate the app's primary feature - synchronized sound-based human waves.
 */
@OptIn(ExperimentalTime::class)
@LargeTest
class RealSoundChoreographyIntegrationTest : BaseRealIntegrationTest() {

    /**
     * Test sound timing accuracy meets the ±50ms synchronization requirement.
     * This is critical for coordinated wave experiences across multiple participants.
     */
    @Test
    fun realSoundChoreography_timingAccuracy_meetsSync50msRequirement() = runTest {
        println("🎵 Testing sound timing accuracy for ±50ms synchronization requirement...")

        // Verify device audio capabilities
        assertTrue(
            deviceStateManager.hasAudioCapability(),
            "Device must support audio output for sound choreography tests"
        )

        // Create test event with known timing
        val testEvent = testDataManager.createTestEvent(
            eventId = "sound_timing_test",
            startTimeOffset = 30.seconds
        )

        val performanceTrace = performanceMonitor.startPerformanceTrace("sound_timing_accuracy")

        try {
            // Test multiple sound triggers to measure timing consistency
            val timingMeasurements = mutableListOf<Long>()
            val expectedIntervals = listOf(100L, 200L, 300L, 500L, 1000L) // milliseconds

            expectedIntervals.forEach { expectedInterval ->
                val startTime = System.currentTimeMillis()

                // Trigger sound with forced start time
                val customStartTime = clock.now() - expectedInterval.milliseconds
                val noteNumber = testEvent.warming.playCurrentSoundChoreographyTone(customStartTime)

                val actualTime = System.currentTimeMillis()
                val measuredInterval = actualTime - startTime

                timingMeasurements.add(measuredInterval)

                assertNotNull(noteNumber, "Sound should be triggered for timing test")
                println("📊 Expected: ${expectedInterval}ms, Measured: ${measuredInterval}ms, Note: $noteNumber")

                // Allow time for sound to complete
                delay(100.milliseconds)
            }

            // Analyze timing accuracy
            val averageLatency = timingMeasurements.average()
            val maxLatency = timingMeasurements.maxOrNull() ?: 0L
            val timingVariation = calculateStandardDeviation(timingMeasurements.map { it.toDouble() })

            println("📈 Sound Timing Analysis:")
            println("   Average Latency: ${averageLatency.toLong()}ms")
            println("   Maximum Latency: ${maxLatency}ms")
            println("   Timing Variation (σ): ${timingVariation.toLong()}ms")

            // Validate timing requirements
            assertTrue(
                maxLatency < 50,
                "Maximum sound latency must be under 50ms for synchronization (measured: ${maxLatency}ms)"
            )

            assertTrue(
                timingVariation < 25.0,
                "Sound timing variation must be under 25ms for consistent experience (measured: ${timingVariation.toLong()}ms)"
            )

            performanceTrace.recordMetric("average_sound_latency_ms", averageLatency)
            performanceTrace.recordMetric("max_sound_latency_ms", maxLatency.toDouble())
            performanceTrace.recordMetric("timing_variation_ms", timingVariation)

        } finally {
            performanceTrace.stop()
            testDataManager.cleanupTestEvent(testEvent.id)
        }

        println("✅ Sound timing accuracy test completed - meets ±50ms requirement")
    }

    /**
     * Test audio choreography sequence execution during wave events.
     * Validates that sound choreography progresses correctly throughout wave duration.
     */
    @Test
    fun realSoundChoreography_sequenceExecution_progressesCorrectlyDuringWave() = runTest {
        println("🎼 Testing audio choreography sequence execution during wave events...")

        assertTrue(
            deviceStateManager.hasAudioCapability(),
            "Device must support audio for choreography sequence tests"
        )

        val testEvent = testDataManager.createTestEvent(
            eventId = "choreography_sequence_test",
            startTimeOffset = 10.seconds
        )

        val performanceTrace = performanceMonitor.startPerformanceTrace("choreography_sequence")

        try {
            // Test choreography progression over multiple time points
            val choreographyTimeline = mutableListOf<ChoreographyTimePoint>()
            val testDuration = 8.seconds
            val sampleInterval = 500.milliseconds
            val totalSamples = (testDuration.inWholeMilliseconds / sampleInterval.inWholeMilliseconds).toInt()

            repeat(totalSamples) { sample ->
                val timeOffset = sample * sampleInterval.inWholeMilliseconds
                val customStartTime = clock.now() - timeOffset.milliseconds

                val noteNumber = testEvent.warming.playCurrentSoundChoreographyTone(customStartTime)
                val frequency = noteNumber?.let { WaveformGenerator.midiPitchToFrequency(it) }

                choreographyTimeline.add(
                    ChoreographyTimePoint(
                        timeOffset = timeOffset,
                        noteNumber = noteNumber,
                        frequency = frequency
                    )
                )

                delay(50.milliseconds) // Brief delay between samples
            }

            // Analyze choreography progression
            val totalNotes = choreographyTimeline.count { it.noteNumber != null }
            val uniqueNotes = choreographyTimeline.mapNotNull { it.noteNumber }.toSet()
            val frequencyRange = choreographyTimeline.mapNotNull { it.frequency }.let { frequencies ->
                if (frequencies.isNotEmpty()) frequencies.maxOrNull()!! - frequencies.minOrNull()!! else 0.0
            }

            println("🎹 Choreography Analysis:")
            println("   Total Samples: ${choreographyTimeline.size}")
            println("   Notes Played: $totalNotes")
            println("   Unique Notes: ${uniqueNotes.size}")
            println("   Frequency Range: ${frequencyRange.toInt()} Hz")

            // Validate choreography quality
            assertTrue(
                totalNotes >= choreographyTimeline.size * 0.3,
                "At least 30% of timeline should have active notes (measured: ${totalNotes}/${choreographyTimeline.size})"
            )

            assertTrue(
                uniqueNotes.size >= 3,
                "Choreography should use at least 3 unique notes for musical variety (measured: ${uniqueNotes.size})"
            )

            assertTrue(
                frequencyRange > 100.0,
                "Choreography should span at least 100Hz frequency range (measured: ${frequencyRange.toInt()}Hz)"
            )

            performanceTrace.recordMetric("total_notes_played", totalNotes.toDouble())
            performanceTrace.recordMetric("unique_notes_count", uniqueNotes.size.toDouble())
            performanceTrace.recordMetric("frequency_range_hz", frequencyRange)

        } finally {
            performanceTrace.stop()
            testDataManager.cleanupTestEvent(testEvent.id)
        }

        println("✅ Audio choreography sequence test completed successfully")
    }

    /**
     * Test sound coordination between multiple simulated participants.
     * Validates that multiple participants create a harmonious chord experience.
     */
    @Test
    fun realSoundChoreography_participantCoordination_createsHarmoniousChords() = runTest {
        println("👥 Testing sound coordination between simulated participants...")

        assertTrue(
            deviceStateManager.hasAudioCapability(),
            "Device must support audio for participant coordination tests"
        )

        val testEvent = testDataManager.createTestEvent(
            eventId = "participant_coordination_test",
            startTimeOffset = 15.seconds
        )

        val performanceTrace = performanceMonitor.startPerformanceTrace("participant_coordination")

        try {
            // Simulate multiple participants being hit at nearly the same time
            val participantCount = 25
            val simultaneousWindow = 100.milliseconds // Participants hit within 100ms
            val participantNotes = mutableListOf<ParticipantNote>()

            // Simulate participants
            repeat(participantCount) { participant ->
                val participantOffset = (participant * 4).milliseconds // Slight stagger
                val customStartTime = clock.now() - participantOffset

                val noteNumber = testEvent.warming.playCurrentSoundChoreographyTone(customStartTime)
                val frequency = noteNumber?.let { WaveformGenerator.midiPitchToFrequency(it) }
                val amplitude = noteNumber?.let { WaveformGenerator.midiVelocityToAmplitude(127) } // Max velocity

                participantNotes.add(
                    ParticipantNote(
                        participantId = participant,
                        noteNumber = noteNumber,
                        frequency = frequency,
                        amplitude = amplitude,
                        timestamp = System.currentTimeMillis()
                    )
                )

                delay(4.milliseconds) // Brief stagger between participants
            }

            // Analyze participant coordination
            val activeParticipants = participantNotes.filter { it.noteNumber != null }
            val uniqueNotesInChord = activeParticipants.mapNotNull { it.noteNumber }.toSet()
            val harmonicSpread = analyzeHarmonicSpread(activeParticipants.mapNotNull { it.frequency })
            val timingSpread = analyzeTimingSpread(activeParticipants.map { it.timestamp })

            println("🎶 Participant Coordination Analysis:")
            println("   Total Participants: $participantCount")
            println("   Active Participants: ${activeParticipants.size}")
            println("   Notes in Chord: ${uniqueNotesInChord.size}")
            println("   Harmonic Spread: ${harmonicSpread.toInt()} Hz")
            println("   Timing Spread: ${timingSpread}ms")

            // Validate coordination quality
            assertTrue(
                activeParticipants.size >= participantCount * 0.7,
                "At least 70% of participants should produce sound (measured: ${activeParticipants.size}/$participantCount)"
            )

            assertTrue(
                uniqueNotesInChord.size in 2..8,
                "Chord should contain 2-8 different notes for harmony (measured: ${uniqueNotesInChord.size})"
            )

            assertTrue(
                timingSpread < 50,
                "Participant timing should be synchronized within 50ms (measured: ${timingSpread}ms)"
            )

            assertTrue(
                harmonicSpread > 50.0 && harmonicSpread < 1000.0,
                "Harmonic spread should be musical but not excessive (measured: ${harmonicSpread.toInt()}Hz)"
            )

            performanceTrace.recordMetric("active_participants", activeParticipants.size.toDouble())
            performanceTrace.recordMetric("chord_notes_count", uniqueNotesInChord.size.toDouble())
            performanceTrace.recordMetric("harmonic_spread_hz", harmonicSpread)
            performanceTrace.recordMetric("timing_spread_ms", timingSpread.toDouble())

        } finally {
            performanceTrace.stop()
            testDataManager.cleanupTestEvent(testEvent.id)
        }

        println("✅ Participant coordination test completed - harmonious chords achieved")
    }

    /**
     * Test audio performance under load scenarios.
     * Validates that sound system maintains performance with many participants.
     */
    @Test
    fun realSoundChoreography_performanceUnderLoad_maintainsQualityWithManyParticipants() = runTest {
        println("⚡ Testing audio performance under load scenarios...")

        assertTrue(
            deviceStateManager.hasAudioCapability(),
            "Device must support audio for performance load tests"
        )

        val testEvent = testDataManager.createTestEvent(
            eventId = "audio_performance_load_test",
            startTimeOffset = 20.seconds
        )

        val performanceTrace = performanceMonitor.startPerformanceTrace("audio_performance_load")

        try {
            // Test increasing load scenarios
            val loadScenarios = listOf(50, 100, 200, 500)
            val performanceResults = mutableListOf<LoadTestResult>()

            loadScenarios.forEach { participantCount ->
                println("🔄 Testing with $participantCount participants...")

                val startMemory = deviceStateManager.getMemoryUsage()
                val startTime = System.currentTimeMillis()
                val notes = mutableListOf<Int>()

                // Rapid-fire sound generation simulating many participants
                repeat(participantCount) { participant ->
                    val noteNumber = testEvent.warming.playCurrentSoundChoreographyTone()
                    noteNumber?.let { notes.add(it) }

                    // Minimal delay to simulate near-simultaneous participants
                    if (participant % 10 == 0) {
                        delay(1.milliseconds)
                    }
                }

                val endTime = System.currentTimeMillis()
                val endMemory = deviceStateManager.getMemoryUsage()

                val executionTime = endTime - startTime
                val memoryIncrease = endMemory.usedMemoryMB - startMemory.usedMemoryMB
                val successRate = notes.size.toDouble() / participantCount.toDouble()

                performanceResults.add(
                    LoadTestResult(
                        participantCount = participantCount,
                        executionTimeMs = executionTime,
                        memoryIncreaseMB = memoryIncrease,
                        successRate = successRate,
                        notesGenerated = notes.size
                    )
                )

                println("📊 $participantCount participants: ${executionTime}ms, ${memoryIncrease}MB memory, ${(successRate * 100).toInt()}% success")

                // Allow memory/audio system to stabilize
                delay(1.seconds)
            }

            // Analyze performance results
            val maxExecutionTime = performanceResults.maxByOrNull { it.executionTimeMs }?.executionTimeMs ?: 0
            val maxMemoryIncrease = performanceResults.maxByOrNull { it.memoryIncreaseMB }?.memoryIncreaseMB ?: 0f
            val minSuccessRate = performanceResults.minByOrNull { it.successRate }?.successRate ?: 1.0
            val avgNotesPerSecond = performanceResults.map {
                if (it.executionTimeMs > 0) it.notesGenerated.toDouble() / (it.executionTimeMs / 1000.0) else 0.0
            }.average()

            println("🎯 Performance Under Load Analysis:")
            println("   Max Execution Time: ${maxExecutionTime}ms")
            println("   Max Memory Increase: ${maxMemoryIncrease}MB")
            println("   Min Success Rate: ${(minSuccessRate * 100).toInt()}%")
            println("   Avg Notes/Second: ${avgNotesPerSecond.toInt()}")

            // Validate performance requirements
            assertTrue(
                maxExecutionTime < 2000,
                "Audio load test should complete within 2 seconds (measured: ${maxExecutionTime}ms)"
            )

            assertTrue(
                maxMemoryIncrease < 50f,
                "Memory increase should be under 50MB during load test (measured: ${maxMemoryIncrease}MB)"
            )

            assertTrue(
                minSuccessRate > 0.8,
                "Success rate should remain above 80% under load (measured: ${(minSuccessRate * 100).toInt()}%)"
            )

            assertTrue(
                avgNotesPerSecond > 100.0,
                "System should generate at least 100 notes/second (measured: ${avgNotesPerSecond.toInt()})"
            )

            performanceTrace.recordMetric("max_execution_time_ms", maxExecutionTime.toDouble())
            performanceTrace.recordMetric("max_memory_increase_mb", maxMemoryIncrease.toDouble())
            performanceTrace.recordMetric("min_success_rate", minSuccessRate)
            performanceTrace.recordMetric("avg_notes_per_second", avgNotesPerSecond)

        } finally {
            performanceTrace.stop()
            testDataManager.cleanupTestEvent(testEvent.id)
        }

        println("✅ Audio performance under load test completed - quality maintained")
    }

    // Helper methods and data classes

    private fun calculateStandardDeviation(values: List<Double>): Double {
        val mean = values.average()
        val variance = values.map { (it - mean) * (it - mean) }.average()
        return kotlin.math.sqrt(variance)
    }

    private fun analyzeHarmonicSpread(frequencies: List<Double>): Double {
        return if (frequencies.size >= 2) {
            frequencies.maxOrNull()!! - frequencies.minOrNull()!!
        } else {
            0.0
        }
    }

    private fun analyzeTimingSpread(timestamps: List<Long>): Long {
        return if (timestamps.size >= 2) {
            timestamps.maxOrNull()!! - timestamps.minOrNull()!!
        } else {
            0L
        }
    }

    private data class ChoreographyTimePoint(
        val timeOffset: Long,
        val noteNumber: Int?,
        val frequency: Double?
    )

    private data class ParticipantNote(
        val participantId: Int,
        val noteNumber: Int?,
        val frequency: Double?,
        val amplitude: Double?,
        val timestamp: Long
    )

    private data class LoadTestResult(
        val participantCount: Int,
        val executionTimeMs: Long,
        val memoryIncreaseMB: Float,
        val successRate: Double,
        val notesGenerated: Int
    )
}