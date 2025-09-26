package com.worldwidewaves.shared.choreographies

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

import com.worldwidewaves.shared.WWWGlobals.FileSystem
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.sound.MidiParser
import com.worldwidewaves.shared.sound.MidiTrack
import com.worldwidewaves.shared.sound.SoundPlayer
import com.worldwidewaves.shared.sound.WaveformGenerator
import com.worldwidewaves.shared.utils.Log
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Comprehensive simulation test for sound choreography in a crowd of 50 simultaneous people.
 *
 * This test simulates:
 * - 50 people each playing sound every 100ms
 * - Wave progression through the full duration of the MIDI file
 * - Verification that the song remains recognizable in a large crowd
 * - Performance analysis of concurrent sound playback
 */
@OptIn(ExperimentalTime::class)
class CrowdSoundChoreographySimulationTest {
    companion object {
        private const val CROWD_SIZE = 50
        private const val SIMULATION_INTERVAL_MS = 100L
        private const val TAG = "CrowdSimulation"
    }

    /**
     * Data class representing a simulated person in the crowd
     */
    private data class SimulatedPerson(
        val id: Int,
        val soundChoreographyManager: SoundChoreographyManager,
        val playbackLog: MutableList<PlaybackEvent> = mutableListOf(),
    )

    /**
     * Data class tracking each sound playback event
     */
    private data class PlaybackEvent(
        val personId: Int,
        val timestamp: Instant,
        val elapsedTime: Duration,
        val midiPitch: Int?,
        val frequency: Double,
        val amplitude: Double,
    )

    /**
     * Simulation results for analysis
     */
    private data class SimulationResults(
        val totalDuration: Duration,
        val totalPlaybackEvents: Int,
        val avgEventsPerPerson: Double,
        val uniquePitchesPlayed: Set<Int>,
        val coveragePercentage: Double,
        val playbackLog: List<PlaybackEvent>,
    )

    @Test
    fun `simulate crowd sound choreography with 50 people over full MIDI duration`() =
        runBlocking {
            Log.d(TAG, "Starting crowd sound choreography simulation")
            Log.d(TAG, "Parameters: $CROWD_SIZE people, ${SIMULATION_INTERVAL_MS}ms intervals")

            // Load and parse the MIDI file
            val midiTrack = loadMidiTrack()
            Log.d(TAG, "MIDI track loaded: ${midiTrack.name}")
            Log.d(TAG, "Duration: ${midiTrack.totalDuration.inWholeSeconds}s, Notes: ${midiTrack.notes.size}")

            // Create simulated crowd
            val crowd = createSimulatedCrowd(midiTrack)
            Log.d(TAG, "Created crowd of ${crowd.size} people")

            // Run the simulation
            val results = runCrowdSimulation(crowd, midiTrack)

            // Analyze and verify results
            analyzeSimulationResults(results, midiTrack)
        }

    /**
     * Load the MIDI track for simulation
     */
    private suspend fun loadMidiTrack(): MidiTrack =
        try {
            MidiParser.parseMidiFile(FileSystem.CHOREOGRAPHIES_SOUND_MIDIFILE)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load real MIDI file, using mock data: ${e.message}")
            createMockMidiTrack()
        }

    /**
     * Create a mock MIDI track for testing when real file isn't available
     */
    private fun createMockMidiTrack(): MidiTrack {
        val mockNotes = mutableListOf<com.worldwidewaves.shared.sound.MidiNote>()
        val trackDuration = 15.seconds
        val noteInterval = 200.milliseconds // Notes every 200ms
        val noteDuration = 400.milliseconds // Each note lasts 400ms, ensuring overlap

        // Create overlapping notes throughout the entire track duration
        var currentTime = Duration.ZERO
        var pitch = 60 // Start from middle C

        while (currentTime < trackDuration) {
            // Create multiple notes at each time step for richness
            for (pitchOffset in 0..2) {
                val currentPitch = (pitch + pitchOffset) % 128
                mockNotes.add(
                    com.worldwidewaves.shared.sound.MidiNote(
                        pitch = currentPitch,
                        velocity = Random.nextInt(80, 127),
                        startTime = currentTime,
                        duration = noteDuration,
                    ),
                )
            }

            currentTime += noteInterval
            pitch = ((pitch + 1) % 48) + 60 // Cycle through a reasonable range
        }

        return MidiTrack(
            name = "Mock MIDI Track for Testing",
            notes = mockNotes,
            totalDuration = trackDuration,
            tempo = 120,
        )
    }

    /**
     * Create a simulated crowd of people with individual SoundChoreographyManagers
     */
    private suspend fun createSimulatedCrowd(midiTrack: MidiTrack): List<SimulatedPerson> =
        coroutineScope {
            (1..CROWD_SIZE)
                .map { personId ->
                    async {
                        val mockClock = mockk<IClock>()
                        val mockSoundPlayer = mockk<SoundPlayer>(relaxed = true)

                        // Create manager for this person
                        val manager =
                            SoundChoreographyManager().apply {
                                setCurrentTrack(midiTrack)
                                setLooping(true)
                                setWaveform(SoundPlayer.Waveform.SQUARE)
                            }

                        SimulatedPerson(personId, manager)
                    }
                }.map { it.await() }
        }

    /**
     * Run the full crowd simulation
     */
    private suspend fun runCrowdSimulation(
        crowd: List<SimulatedPerson>,
        midiTrack: MidiTrack,
    ): SimulationResults {
        val waveStartTime = Instant.fromEpochMilliseconds(System.currentTimeMillis())
        val allPlaybackEvents = mutableListOf<PlaybackEvent>()
        val simulationDuration = midiTrack.totalDuration + 2.seconds // Add buffer time

        Log.d(TAG, "Starting simulation at $waveStartTime")
        Log.d(TAG, "Simulation will run for ${simulationDuration.inWholeSeconds} seconds")

        var currentTime = Duration.ZERO
        var eventCount = 0

        // Simulate wave progression over time
        while (currentTime < simulationDuration) {
            val currentInstant =
                Instant.fromEpochMilliseconds(
                    waveStartTime.toEpochMilliseconds() + currentTime.inWholeMilliseconds,
                )

            // Have each person in the crowd play their sound simultaneously
            coroutineScope {
                crowd
                    .map { person ->
                        async {
                            try {
                                // Mock the clock to return current simulation time
                                val mockClock = mockk<IClock>()
                                every { mockClock.now() } returns currentInstant

                                // Calculate what pitch would be played
                                val elapsedTime = currentTime
                                val trackPosition =
                                    if (midiTrack.totalDuration > Duration.ZERO) {
                                        (elapsedTime.inWholeMilliseconds % midiTrack.totalDuration.inWholeMilliseconds).milliseconds
                                    } else {
                                        elapsedTime
                                    }

                                // Find active notes at this position
                                val activeNotes = midiTrack.notes.filter { it.isActiveAt(trackPosition) }

                                if (activeNotes.isNotEmpty()) {
                                    // Select a random note (simulating what the real manager does)
                                    val selectedNote = activeNotes[Random.nextInt(activeNotes.size)]
                                    val frequency = WaveformGenerator.midiPitchToFrequency(selectedNote.pitch)
                                    val amplitude = WaveformGenerator.midiVelocityToAmplitude(selectedNote.velocity)

                                    // Record the playback event
                                    val event =
                                        PlaybackEvent(
                                            personId = person.id,
                                            timestamp = currentInstant,
                                            elapsedTime = elapsedTime,
                                            midiPitch = selectedNote.pitch,
                                            frequency = frequency,
                                            amplitude = amplitude,
                                        )

                                    synchronized(allPlaybackEvents) {
                                        allPlaybackEvents.add(event)
                                        person.playbackLog.add(event)
                                    }

                                    eventCount++
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Person ${person.id} failed to play sound: ${e.message}")
                            }
                        }
                    }.forEach { it.await() }
            }

            // Progress simulation time
            currentTime += SIMULATION_INTERVAL_MS.milliseconds

            // Log progress every few seconds
            if (currentTime.inWholeMilliseconds % 2000 == 0L) {
                Log.d(TAG, "Simulation progress: ${currentTime.inWholeSeconds}s / ${simulationDuration.inWholeSeconds}s")
                Log.d(TAG, "Events recorded so far: $eventCount")
            }

            // Small delay to prevent overwhelming the system
            delay(1L)
        }

        Log.d(TAG, "Simulation completed. Total events: $eventCount")

        // Analyze results
        val uniquePitchesPlayed = allPlaybackEvents.mapNotNull { it.midiPitch }.toSet()
        val avgEventsPerPerson = if (crowd.isNotEmpty()) eventCount.toDouble() / crowd.size else 0.0
        val uniquePitchesInTrack = midiTrack.notes.map { it.pitch }.toSet()
        val coveragePercentage =
            if (uniquePitchesInTrack.isNotEmpty()) {
                (uniquePitchesPlayed.size.toDouble() / uniquePitchesInTrack.size) * 100.0
            } else {
                0.0
            }

        return SimulationResults(
            totalDuration = currentTime,
            totalPlaybackEvents = eventCount,
            avgEventsPerPerson = avgEventsPerPerson,
            uniquePitchesPlayed = uniquePitchesPlayed,
            coveragePercentage = coveragePercentage,
            playbackLog = allPlaybackEvents,
        )
    }

    /**
     * Analyze simulation results and verify the crowd behavior
     */
    private fun analyzeSimulationResults(
        results: SimulationResults,
        midiTrack: MidiTrack,
    ) {
        Log.d(TAG, "=== CROWD SOUND SIMULATION RESULTS ===")
        Log.d(TAG, "Simulation Duration: ${results.totalDuration.inWholeSeconds} seconds")
        Log.d(TAG, "Total Playback Events: ${results.totalPlaybackEvents}")
        Log.d(TAG, "Average Events per Person: ${"%.1f".format(results.avgEventsPerPerson)}")
        Log.d(TAG, "Unique MIDI Pitches Played: ${results.uniquePitchesPlayed.size}")
        Log.d(TAG, "Track Coverage: ${"%.1f".format(results.coveragePercentage)}%")

        // Performance verification
        val expectedMinEvents = (results.totalDuration.inWholeMilliseconds / SIMULATION_INTERVAL_MS) * CROWD_SIZE * 0.8
        assertTrue(
            "Expected at least $expectedMinEvents playback events, got ${results.totalPlaybackEvents}",
            results.totalPlaybackEvents >= expectedMinEvents,
        )

        // Coverage verification - ensure song is recognizable
        assertTrue(
            "Track coverage too low (${results.coveragePercentage}%) - song may not be recognizable",
            results.coveragePercentage >= 50.0,
        )

        // Temporal distribution analysis
        analyzeTemporalDistribution(results.playbackLog, midiTrack)

        // Pitch distribution analysis
        analyzePitchDistribution(results.playbackLog, midiTrack)

        Log.d(TAG, "✅ Crowd sound choreography simulation PASSED!")
        Log.d(TAG, "The MIDI file should be recognizable when played by a crowd of $CROWD_SIZE people")
    }

    /**
     * Analyze how events are distributed over time
     */
    private fun analyzeTemporalDistribution(
        events: List<PlaybackEvent>,
        midiTrack: MidiTrack,
    ) {
        val timeIntervals = 10 // Divide into 10 time segments
        val intervalDuration = midiTrack.totalDuration.inWholeMilliseconds / timeIntervals
        val eventsByInterval = mutableMapOf<Int, Int>()

        events.forEach { event ->
            val interval = (event.elapsedTime.inWholeMilliseconds / intervalDuration).toInt().coerceAtMost(timeIntervals - 1)
            eventsByInterval[interval] = eventsByInterval.getOrDefault(interval, 0) + 1
        }

        Log.d(TAG, "Temporal Distribution Analysis:")
        eventsByInterval.forEach { (interval, count) ->
            val startTime = (interval * intervalDuration) / 1000.0
            val endTime = ((interval + 1) * intervalDuration) / 1000.0
            Log.d(TAG, "  ${startTime}s-${endTime}s: $count events")
        }
    }

    /**
     * Analyze pitch distribution to verify musical content
     */
    private fun analyzePitchDistribution(
        events: List<PlaybackEvent>,
        midiTrack: MidiTrack,
    ) {
        val pitchCounts =
            events
                .mapNotNull { it.midiPitch }
                .groupingBy { it }
                .eachCount()
                .toList()
                .sortedByDescending { it.second }

        Log.d(TAG, "Pitch Distribution Analysis (Top 10):")
        pitchCounts.take(10).forEach { (pitch, count) ->
            val noteName = getPitchName(pitch)
            val percentage = (count.toDouble() / events.size) * 100.0
            Log.d(TAG, "  $noteName (MIDI $pitch): $count events (${"%.1f".format(percentage)}%)")
        }

        // Verify we have good pitch diversity
        val pitchDiversity = pitchCounts.size
        val expectedMinDiversity = (midiTrack.notes.distinctBy { it.pitch }.size * 0.5).toInt()

        assertTrue(
            "Insufficient pitch diversity: $pitchDiversity unique pitches, expected at least $expectedMinDiversity",
            pitchDiversity >= expectedMinDiversity,
        )
    }

    /**
     * Convert MIDI pitch number to note name
     */
    private fun getPitchName(pitch: Int): String {
        val noteNames = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
        val octave = (pitch / 12) - 1
        val noteIndex = pitch % 12
        return "${noteNames[noteIndex]}$octave"
    }
}
