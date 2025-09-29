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

import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Tests for MIDI parsing functionality with edge cases and error handling.
 * These tests ensure robust parsing of MIDI files and proper handling of
 * malformed or edge case MIDI data.
 */
class MidiParserTest {
    @Test
    fun `should create valid MidiNote with correct properties`() {
        // GIVEN: Valid note parameters
        val pitch = 60 // Middle C
        val velocity = 100
        val startTime = 1.seconds
        val duration = 500.milliseconds

        // WHEN: Creating a MIDI note
        val note = MidiNote(pitch, velocity, startTime, duration)

        // THEN: All properties should be correctly set
        assertEquals(pitch, note.pitch, "Pitch should match constructor parameter")
        assertEquals(velocity, note.velocity, "Velocity should match constructor parameter")
        assertEquals(startTime, note.startTime, "Start time should match constructor parameter")
        assertEquals(duration, note.duration, "Duration should match constructor parameter")
    }

    @Test
    fun `should correctly determine note activity at different time positions`() {
        // GIVEN: A note that starts at 1s and lasts for 500ms
        val note =
            MidiNote(
                pitch = 60,
                velocity = 100,
                startTime = 1.seconds,
                duration = 500.milliseconds,
            )

        // WHEN/THEN: Testing various time positions
        assertFalse(note.isActiveAt(500.milliseconds), "Note should not be active before start time")
        assertFalse(note.isActiveAt(999.milliseconds), "Note should not be active just before start time")
        assertTrue(note.isActiveAt(1.seconds), "Note should be active at start time")
        assertTrue(note.isActiveAt(1250.milliseconds), "Note should be active in the middle")
        assertFalse(note.isActiveAt(1500.milliseconds), "Note should not be active at end time")
        assertFalse(note.isActiveAt(2.seconds), "Note should not be active after end time")
    }

    @Test
    fun `should handle edge case of zero duration note`() {
        // GIVEN: A note with zero duration
        val note =
            MidiNote(
                pitch = 60,
                velocity = 100,
                startTime = 1.seconds,
                duration = 0.milliseconds,
            )

        // WHEN/THEN: Zero duration note should not be active at any time
        assertFalse(note.isActiveAt(999.milliseconds), "Zero duration note should not be active before start")
        assertFalse(note.isActiveAt(1.seconds), "Zero duration note should not be active at start time")
        assertFalse(note.isActiveAt(1001.milliseconds), "Zero duration note should not be active after start")
    }

    @Test
    fun `should handle MIDI pitch and velocity boundary values`() {
        // GIVEN: Notes with boundary values
        val minPitchNote = MidiNote(0, 0, 0.seconds, 100.milliseconds)
        val maxPitchNote = MidiNote(127, 127, 0.seconds, 100.milliseconds)

        // WHEN/THEN: Boundary values should be handled correctly
        assertEquals(0, minPitchNote.pitch, "Minimum pitch should be 0")
        assertEquals(0, minPitchNote.velocity, "Minimum velocity should be 0")
        assertEquals(127, maxPitchNote.pitch, "Maximum pitch should be 127")
        assertEquals(127, maxPitchNote.velocity, "Maximum velocity should be 127")
    }

    @Test
    fun `should create valid MidiTrack with correct properties`() {
        // GIVEN: Track parameters and notes
        val trackName = "Test Track"
        val notes =
            listOf(
                MidiNote(60, 100, 0.seconds, 500.milliseconds),
                MidiNote(64, 80, 500.milliseconds, 500.milliseconds),
            )
        val totalDuration = 1.seconds
        val tempo = 120

        // WHEN: Creating a MIDI track
        val track = MidiTrack(trackName, notes, totalDuration, tempo)

        // THEN: All properties should be correctly set
        assertEquals(trackName, track.name, "Track name should match constructor parameter")
        assertEquals(notes, track.notes, "Notes should match constructor parameter")
        assertEquals(totalDuration, track.totalDuration, "Total duration should match constructor parameter")
        assertEquals(tempo, track.tempo, "Tempo should match constructor parameter")
    }

    @Test
    fun `should use default tempo when not specified`() {
        // GIVEN: Track created without explicit tempo
        val track =
            MidiTrack(
                name = "Test Track",
                notes = emptyList(),
                totalDuration = 1.seconds,
            )

        // WHEN/THEN: Default tempo should be 120 BPM
        assertEquals(120, track.tempo, "Default tempo should be 120 BPM")
    }

    @Test
    fun `should handle empty track creation`() {
        // GIVEN: Empty track parameters
        val track =
            MidiTrack(
                name = "Empty Track",
                notes = emptyList(),
                totalDuration = 0.seconds,
                tempo = 60,
            )

        // WHEN/THEN: Empty track should be valid
        assertTrue(track.notes.isEmpty(), "Empty track should have no notes")
        assertEquals(0.seconds, track.totalDuration, "Empty track should have zero duration")
    }

    @Test
    fun `should throw exception for invalid MIDI file header`() {
        // GIVEN: Invalid MIDI bytes (wrong header)
        val invalidHeader =
            byteArrayOf(
                // Wrong header "MXhd" instead of "MThd"
                0x4D,
                0x58,
                0x68,
                0x64, // "MXhd"
                0x00,
                0x00,
                0x00,
                0x06, // Header length
                0x00,
                0x00, // Format 0
                0x00,
                0x01, // 1 track
                0x00,
                0x60, // 96 ticks per quarter note
            )

        // WHEN/THEN: Should throw exception for invalid header
        assertFailsWith<Exception>("Should throw exception for invalid MIDI header") {
            MidiParser.parseMidiBytes(invalidHeader)
        }
    }

    @Test
    fun `should throw exception for invalid header length`() {
        // GIVEN: MIDI bytes with invalid header length
        val invalidHeaderLength =
            byteArrayOf(
                0x4D,
                0x54,
                0x68,
                0x64, // "MThd"
                0x00,
                0x00,
                0x00,
                0x08, // Wrong header length (8 instead of 6)
                0x00,
                0x00, // Format 0
                0x00,
                0x01, // 1 track
                0x00,
                0x60, // 96 ticks per quarter note
                0x00,
                0x00, // Extra bytes due to wrong length
            )

        // WHEN/THEN: Should throw exception for invalid header length
        assertFailsWith<Exception>("Should throw exception for invalid header length") {
            MidiParser.parseMidiBytes(invalidHeaderLength)
        }
    }

    @Test
    fun `should handle truncated MIDI file gracefully`() {
        // GIVEN: Truncated MIDI file (incomplete header)
        val truncatedMidi =
            byteArrayOf(
                0x4D,
                0x54,
                0x68, // Incomplete "MThd" header
            )

        // WHEN/THEN: Should throw exception for truncated file
        assertFailsWith<Exception>("Should throw exception for truncated MIDI file") {
            MidiParser.parseMidiBytes(truncatedMidi)
        }
    }

    @Test
    fun `should parse valid minimal MIDI file`() {
        // GIVEN: Minimal valid MIDI file with no notes
        val minimalMidi =
            byteArrayOf(
                // Header chunk
                0x4D,
                0x54,
                0x68,
                0x64, // "MThd"
                0x00,
                0x00,
                0x00,
                0x06, // Header length
                0x00,
                0x00, // Format 0
                0x00,
                0x01, // 1 track
                0x00,
                0x60, // 96 ticks per quarter note
                // Track chunk
                0x4D,
                0x54,
                0x72,
                0x6B, // "MTrk"
                0x00,
                0x00,
                0x00,
                0x04, // Track length (4 bytes)
                0x00, // Delta time 0
                0xFF.toByte(),
                0x2F,
                0x00, // End of track meta event
            )

        // WHEN: Parsing minimal MIDI file
        val track = MidiParser.parseMidiBytes(minimalMidi)

        // THEN: Should create valid empty track
        assertEquals("Parsed MIDI Track", track.name, "Should have default track name")
        assertTrue(track.notes.isEmpty(), "Should have no notes")
        assertEquals(0.seconds, track.totalDuration, "Should have zero duration")
        assertEquals(120, track.tempo, "Should have default tempo")
    }

    @Test
    fun `should handle MIDI file with single note`() {
        // GIVEN: MIDI file with one note on/off event
        val singleNoteMidi =
            byteArrayOf(
                // Header chunk
                0x4D,
                0x54,
                0x68,
                0x64, // "MThd"
                0x00,
                0x00,
                0x00,
                0x06, // Header length
                0x00,
                0x00, // Format 0
                0x00,
                0x01, // 1 track
                0x00,
                0x60, // 96 ticks per quarter note
                // Track chunk
                0x4D,
                0x54,
                0x72,
                0x6B, // "MTrk"
                0x00,
                0x00,
                0x00,
                0x0A, // Track length (10 bytes)
                // Note on event
                0x00, // Delta time 0
                0x90.toByte(),
                0x3C,
                0x40, // Note on, channel 0, pitch 60 (C4), velocity 64
                // Note off event
                0x60, // Delta time 96 (1 beat at 96 ticks per beat)
                0x80.toByte(),
                0x3C,
                0x40, // Note off, channel 0, pitch 60, velocity 64
                // End of track
                0x00, // Delta time 0
                0xFF.toByte(),
                0x2F,
                0x00, // End of track meta event
            )

        // WHEN: Parsing MIDI file with single note
        val track = MidiParser.parseMidiBytes(singleNoteMidi)

        // THEN: Should create track with one note
        assertEquals(1, track.notes.size, "Should have exactly one note")
        val note = track.notes.first()
        assertEquals(60, note.pitch, "Note pitch should be 60 (middle C)")
        assertEquals(64, note.velocity, "Note velocity should be 64")
        assertEquals(0.seconds, note.startTime, "Note should start at time 0")
        assertTrue(note.duration > 0.seconds, "Note should have positive duration")
    }

    @Test
    fun `should handle invalid track chunk ID`() {
        // GIVEN: MIDI file with invalid track chunk ID
        val invalidTrackMidi =
            byteArrayOf(
                // Valid header
                0x4D,
                0x54,
                0x68,
                0x64, // "MThd"
                0x00,
                0x00,
                0x00,
                0x06, // Header length
                0x00,
                0x00, // Format 0
                0x00,
                0x01, // 1 track
                0x00,
                0x60, // 96 ticks per quarter note
                // Invalid track chunk
                0x4D,
                0x58,
                0x72,
                0x6B, // "MXrk" instead of "MTrk"
                0x00,
                0x00,
                0x00,
                0x04, // Track length
                0x00,
                0xFF.toByte(),
                0x2F,
                0x00, // End of track
            )

        // WHEN/THEN: Should throw exception for invalid track chunk
        assertFailsWith<Exception>("Should throw exception for invalid track chunk ID") {
            MidiParser.parseMidiBytes(invalidTrackMidi)
        }
    }

    @Test
    fun `should handle note on with zero velocity as note off`() {
        // GIVEN: MIDI file with note on event having zero velocity
        val noteOnZeroVelocityMidi =
            byteArrayOf(
                // Header chunk
                0x4D,
                0x54,
                0x68,
                0x64, // "MThd"
                0x00,
                0x00,
                0x00,
                0x06, // Header length
                0x00,
                0x00, // Format 0
                0x00,
                0x01, // 1 track
                0x00,
                0x60, // 96 ticks per quarter note
                // Track chunk
                0x4D,
                0x54,
                0x72,
                0x6B, // "MTrk"
                0x00,
                0x00,
                0x00,
                0x0A, // Track length (10 bytes)
                // Note on event
                0x00, // Delta time 0
                0x90.toByte(),
                0x3C,
                0x40, // Note on, channel 0, pitch 60, velocity 64
                // Note on with zero velocity (equivalent to note off)
                0x60, // Delta time 96
                0x90.toByte(),
                0x3C,
                0x00, // Note on, channel 0, pitch 60, velocity 0
                // End of track
                0x00, // Delta time 0
                0xFF.toByte(),
                0x2F,
                0x00, // End of track meta event
            )

        // WHEN: Parsing MIDI file with note on zero velocity
        val track = MidiParser.parseMidiBytes(noteOnZeroVelocityMidi)

        // THEN: Should create track with one note (note on/off pair)
        assertEquals(1, track.notes.size, "Should have exactly one note")
        val note = track.notes.first()
        assertEquals(60, note.pitch, "Note pitch should be 60")
        assertEquals(64, note.velocity, "Note velocity should be 64 (from note on)")
        assertTrue(note.duration > 0.seconds, "Note should have positive duration")
    }

    @Test
    fun `should handle tempo change events correctly`() {
        // GIVEN: MIDI file with tempo change event
        val tempoChangeMidi =
            byteArrayOf(
                // Header chunk
                0x4D,
                0x54,
                0x68,
                0x64, // "MThd"
                0x00,
                0x00,
                0x00,
                0x06, // Header length
                0x00,
                0x00, // Format 0
                0x00,
                0x01, // 1 track
                0x00,
                0x60, // 96 ticks per quarter note
                // Track chunk
                0x4D,
                0x54,
                0x72,
                0x6B, // "MTrk"
                0x00,
                0x00,
                0x00,
                0x0A, // Track length (10 bytes)
                // Tempo change event (Set tempo to 500,000 microseconds per beat = 120 BPM)
                0x00, // Delta time 0
                0xFF.toByte(),
                0x51,
                0x03, // Meta event, tempo change, length 3
                0x07,
                0xA1.toByte(),
                0x20, // 500,000 microseconds per beat
                // End of track
                0x00, // Delta time 0
                0xFF.toByte(),
                0x2F,
                0x00, // End of track meta event
            )

        // WHEN: Parsing MIDI file with tempo change
        val track = MidiParser.parseMidiBytes(tempoChangeMidi)

        // THEN: Should handle tempo change without errors
        assertEquals(120, track.tempo, "Should have correct tempo (120 BPM)")
        assertTrue(track.notes.isEmpty(), "Should have no notes")
    }

    @Test
    fun `should handle overlapping notes on same channel and pitch`() {
        // GIVEN: MIDI file with overlapping notes (note on before previous note off)
        val overlappingNotesMidi =
            byteArrayOf(
                // Header chunk
                0x4D,
                0x54,
                0x68,
                0x64, // "MThd"
                0x00,
                0x00,
                0x00,
                0x06, // Header length
                0x00,
                0x00, // Format 0
                0x00,
                0x01, // 1 track
                0x00,
                0x60, // 96 ticks per quarter note
                // Track chunk
                0x4D,
                0x54,
                0x72,
                0x6B, // "MTrk"
                0x00,
                0x00,
                0x00,
                0x10, // Track length (16 bytes)
                // First note on
                0x00, // Delta time 0
                0x90.toByte(),
                0x3C,
                0x40, // Note on, channel 0, pitch 60, velocity 64
                // Second note on (same pitch, should replace first)
                0x30, // Delta time 48
                0x90.toByte(),
                0x3C,
                0x50, // Note on, channel 0, pitch 60, velocity 80
                // Note off
                0x30, // Delta time 48
                0x80.toByte(),
                0x3C,
                0x40, // Note off, channel 0, pitch 60
                // End of track
                0x00, // Delta time 0
                0xFF.toByte(),
                0x2F,
                0x00, // End of track meta event
            )

        // WHEN: Parsing MIDI file with overlapping notes
        val track = MidiParser.parseMidiBytes(overlappingNotesMidi)

        // THEN: Should handle overlapping notes correctly
        assertEquals(1, track.notes.size, "Should have one note (second note on to note off)")
        val note = track.notes.first()
        assertEquals(60, note.pitch, "Note pitch should be 60")
        assertEquals(80, note.velocity, "Note velocity should be 80 (from second note on)")
    }

    @Test
    fun `should handle SMPTE time division format`() {
        // GIVEN: MIDI file with SMPTE time division (bit 15 set)
        val smpteMidi =
            byteArrayOf(
                // Header chunk
                0x4D,
                0x54,
                0x68,
                0x64, // "MThd"
                0x00,
                0x00,
                0x00,
                0x06, // Header length
                0x00,
                0x00, // Format 0
                0x00,
                0x01, // 1 track
                0xE7.toByte(),
                0x28, // SMPTE format: -25 frames/sec, 40 ticks/frame
                // Track chunk
                0x4D,
                0x54,
                0x72,
                0x6B, // "MTrk"
                0x00,
                0x00,
                0x00,
                0x04, // Track length
                0x00,
                0xFF.toByte(),
                0x2F,
                0x00, // End of track
            )

        // WHEN: Parsing MIDI file with SMPTE time division
        val track = MidiParser.parseMidiBytes(smpteMidi)

        // THEN: Should handle SMPTE format and fall back to default ticks per beat
        assertEquals("Parsed MIDI Track", track.name, "Should have default track name")
        assertTrue(track.notes.isEmpty(), "Should have no notes")
    }

    @Test
    fun `should parse resource file successfully when mocked`() =
        runTest {
            // GIVEN: Clear any existing mocks and cache first
            clearAllMocks()
            MidiParser.clearCache() // Clear any cached results that might interfere

            val mockMidiBytes =
                byteArrayOf(
                    // Minimal valid MIDI
                    0x4D,
                    0x54,
                    0x68,
                    0x64, // "MThd"
                    0x00,
                    0x00,
                    0x00,
                    0x06, // Header length
                    0x00,
                    0x00, // Format 0
                    0x00,
                    0x01, // 1 track
                    0x00,
                    0x60, // 96 ticks per quarter note
                    0x4D,
                    0x54,
                    0x72,
                    0x6B, // "MTrk"
                    0x00,
                    0x00,
                    0x00,
                    0x04, // Track length
                    0x00,
                    0xFF.toByte(),
                    0x2F,
                    0x00, // End of track
                )

            // Test direct parsing first to ensure the bytes are valid
            val directTrack = MidiParser.parseMidiBytes(mockMidiBytes)
            assertTrue(directTrack != null, "Direct parsing should work with valid MIDI bytes")
            assertEquals("Parsed MIDI Track", directTrack.name, "Should have default track name")

            // GIVEN: Mock MidiResources for file-based parsing test
            mockkObject(MidiResources)
            try {
                coEvery { MidiResources.readMidiFile("test.mid") } returns mockMidiBytes

                // WHEN: Parsing MIDI file from resource
                val track = MidiParser.parseMidiFile("test.mid")

                // THEN: Should return valid track
                assertTrue(track != null, "Should return a valid track")
                assertEquals("Parsed MIDI Track", track!!.name, "Should have default track name")
                assertEquals(0.seconds, track.totalDuration, "Should have zero duration for empty track")
            } finally {
                unmockkObject(MidiResources)
            }
        }

    @Test
    fun `should return null when resource loading fails`() =
        runTest {
            // GIVEN: Mocked MidiResources that throws exception
            mockkObject(MidiResources)
            try {
                coEvery { MidiResources.readMidiFile(any()) } throws Exception("Resource not found")

                // WHEN: Resource loading fails
                val result = MidiParser.parseMidiFile("nonexistent.mid")

                // THEN: Should return null for graceful degradation
                assertNull(result, "Should return null when resource loading fails")
            } finally {
                unmockkObject(MidiResources)
            }
        }

    @Test
    fun `should handle multiple tracks correctly`() {
        // GIVEN: MIDI file with multiple tracks
        val multiTrackMidi =
            byteArrayOf(
                // Header chunk
                0x4D,
                0x54,
                0x68,
                0x64, // "MThd"
                0x00,
                0x00,
                0x00,
                0x06, // Header length
                0x00,
                0x01, // Format 1 (multiple tracks)
                0x00,
                0x02, // 2 tracks
                0x00,
                0x60, // 96 ticks per quarter note
                // First track
                0x4D,
                0x54,
                0x72,
                0x6B, // "MTrk"
                0x00,
                0x00,
                0x00,
                0x04, // Track length
                0x00,
                0xFF.toByte(),
                0x2F,
                0x00, // End of track
                // Second track
                0x4D,
                0x54,
                0x72,
                0x6B, // "MTrk"
                0x00,
                0x00,
                0x00,
                0x04, // Track length
                0x00,
                0xFF.toByte(),
                0x2F,
                0x00, // End of track
            )

        // WHEN: Parsing multi-track MIDI file
        val track = MidiParser.parseMidiBytes(multiTrackMidi)

        // THEN: Should handle multiple tracks without errors
        assertEquals("Parsed MIDI Track", track.name, "Should have default track name")
        assertTrue(track.notes.isEmpty(), "Should have no notes from empty tracks")
    }

    @Test
    fun `should skip unknown meta events correctly`() {
        // GIVEN: MIDI file with unknown meta event
        val unknownMetaMidi =
            byteArrayOf(
                // Header chunk
                0x4D,
                0x54,
                0x68,
                0x64, // "MThd"
                0x00,
                0x00,
                0x00,
                0x06, // Header length
                0x00,
                0x00, // Format 0
                0x00,
                0x01, // 1 track
                0x00,
                0x60, // 96 ticks per quarter note
                // Track chunk
                0x4D,
                0x54,
                0x72,
                0x6B, // "MTrk"
                0x00,
                0x00,
                0x00,
                0x0A, // Track length (10 bytes)
                // Unknown meta event
                0x00, // Delta time 0
                0xFF.toByte(),
                0x7F,
                0x03, // Meta event, unknown type 0x7F, length 3
                0x01,
                0x02,
                0x03, // Arbitrary data
                // End of track
                0x00, // Delta time 0
                0xFF.toByte(),
                0x2F,
                0x00, // End of track meta event
            )

        // WHEN: Parsing MIDI file with unknown meta event
        val track = MidiParser.parseMidiBytes(unknownMetaMidi)

        // THEN: Should skip unknown meta event and continue parsing
        assertEquals("Parsed MIDI Track", track.name, "Should have default track name")
        assertTrue(track.notes.isEmpty(), "Should have no notes")
    }
}
