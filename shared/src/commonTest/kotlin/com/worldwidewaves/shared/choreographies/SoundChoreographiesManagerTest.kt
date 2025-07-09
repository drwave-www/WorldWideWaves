package com.worldwidewaves.shared.choreographies

import com.worldwidewaves.shared.events.utils.CoroutineScopeProvider
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.sound.MidiNote
import com.worldwidewaves.shared.sound.MidiParser
import com.worldwidewaves.shared.sound.MidiTrack
import com.worldwidewaves.shared.sound.SoundPlayer
import com.worldwidewaves.shared.sound.WaveformGenerator
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import kotlin.math.abs
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class SoundChoreographyManagerTest : KoinTest {

    @MockK
    private lateinit var clock: IClock

    @RelaxedMockK
    private lateinit var soundPlayer: SoundPlayer

    @MockK
    private lateinit var coroutineScopeProvider: CoroutineScopeProvider

    private lateinit var manager: SoundChoreographyManager

    @BeforeTest
    fun setup() {
        MockKAnnotations.init(this)

        // Setup Koin DI
        startKoin {
            modules(
                module {
                    single { clock }
                    single { soundPlayer }
                }
            )
        }

        // Mock coroutineScopeProvider to return a mock Job without executing the lambda - FIXME
        every { coroutineScopeProvider.launchIO(any()) } returns Job()

        // Setup default clock behavior
        every { clock.now() } returns Instant.fromEpochMilliseconds(0)

        // Create manager with mocked dependencies
        manager = SoundChoreographyManager(coroutineScopeProvider)
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `test initializing SoundChoreographyManager triggers MIDI preload`() = runTest {
        // Verify that the coroutineScopeProvider was used to launch the preload
        verify { coroutineScopeProvider.launchIO(any()) }
    }

    @Test
    fun `test preloadMidiFile returns true when MIDI loads successfully`() = runTest {
        // Create a mocked MidiTrack
        val mockedTrack = MidiTrack(
            name = "Test Track",
            notes = listOf(
                MidiNote(60, 80, 0.milliseconds, 300.milliseconds)
            ),
            totalDuration = 500.milliseconds
        )

        // Use MockK's mockkObject to mock the singleton MidiParser object
        mockkObject(MidiParser)

        try {
            // Set up the mock behavior
            coEvery { MidiParser.parseMidiFile(any()) } returns mockedTrack

            // Test the preloadMidiFile function
            val result = manager.preloadMidiFile("test.mid")

            // Verify
            assertTrue(result, "preloadMidiFile should return true when successful")
            coVerify { MidiParser.parseMidiFile("test.mid") }
        } finally {
            // Always clean up after mocking an object
            unmockkObject(MidiParser)
        }
    }

    @Test
    fun `test preloadMidiFile returns false when MIDI load fails`() = runTest {
        // Create a spy on MidiParser to control its behavior
        val midiParserSpy = spyk(MidiParser)

        coEvery { midiParserSpy.parseMidiFile(any()) } throws Exception("Test exception")

        // Test the preloadMidiFile function
        val result = manager.preloadMidiFile("test.mid")

        assertFalse(result, "preloadMidiFile should return false when it fails")
    }

    @Test
    fun `test playCurrentSoundTone returns null when no track is loaded`() = runBlocking {
        // Setup
        val waveStartTime = Instant.fromEpochMilliseconds(0)

        // Test
        val result = manager.playCurrentSoundTone(waveStartTime)

        // Verify
        assertNull(result, "playCurrentSoundTone should return null when no track is loaded")
        coVerify(exactly = 0) { soundPlayer.playTone(any(), any(), any(), any()) }
    }

    @Test
    fun `test playCurrentSoundTone plays a tone when notes are active`() = runTest {
        // Setup a test MIDI track
        val testTrack = MidiTrack(
            name = "Test Track",
            notes = listOf(
                MidiNote(60, 80, 0.milliseconds, 300.milliseconds),
                MidiNote(62, 90, 0.milliseconds, 300.milliseconds)
            ),
            totalDuration = 500.milliseconds
        )

        manager.setCurrentTrack(testTrack)

        // Setup clock to return a time that would make notes active
        every { clock.now() } returns Instant.fromEpochMilliseconds(100)

        // Test
        val waveStartTime = Instant.fromEpochMilliseconds(0)
        val result = manager.playCurrentSoundTone(waveStartTime)

        // Verify
        assertNotNull(result, "playCurrentSoundTone should return a pitch value")
        coVerify { soundPlayer.playTone(any(), any(), any(), any()) }
    }

    @Test
    fun `test playCurrentSoundTone returns null when no notes are active`() = runTest {
        // Setup a test MIDI track with notes that start in the future
        val testTrack = MidiTrack(
            name = "Test Track",
            notes = listOf(
                MidiNote(60, 80, 1000.milliseconds, 300.milliseconds)
            ),
            totalDuration = 2000.milliseconds
        )

        manager.setCurrentTrack(testTrack)

        // Setup clock to return a time before any notes are active
        every { clock.now() } returns Instant.fromEpochMilliseconds(500)

        // Test
        val waveStartTime = Instant.fromEpochMilliseconds(0)
        val result = manager.playCurrentSoundTone(waveStartTime)

        // Verify
        assertNull(result, "playCurrentSoundTone should return null when no notes are active")
        coVerify(exactly = 0) { soundPlayer.playTone(any(), any(), any(), any()) }
    }

    @Test
    fun `test looping behavior in playCurrentSoundTone`() = runTest {
        // Setup a test MIDI track with clear active periods
        val testTrack = MidiTrack(
            name = "Test Track",
            notes = listOf(
                // A note active at the start of the track
                MidiNote(60, 80, 0.milliseconds, 300.milliseconds),
                // A note active in the middle of the track
                MidiNote(62, 80, 500.milliseconds, 300.milliseconds)
            ),
            totalDuration = 1000.milliseconds
        )

        manager.setCurrentTrack(testTrack)

        // Set looping to true
        manager.setLooping(true)

        // Setup wave start time
        val waveStartTime = Instant.fromEpochMilliseconds(0)

        // Test with time that would wrap around to the beginning (1500ms = 1.5 × track duration)
        // This should map to 500ms in the track (after wrapping)
        every { clock.now() } returns Instant.fromEpochMilliseconds(1500)

        // Since 500ms corresponds to our second note, we should get a result
        val result = manager.playCurrentSoundTone(waveStartTime)

        // Verify we get a pitch value when looping
        assertNotNull(result, "playCurrentSoundTone should return a pitch when looping")

        // Verify the sound player was called
        coVerify { soundPlayer.playTone(any(), any(), any(), any()) }

        // Now test with looping off
        manager.setLooping(false)

        // With the same time but looping off
        val result2 = manager.playCurrentSoundTone(waveStartTime)

        // With looping off, we should get null since we're past the track duration
        assertNull(result2, "playCurrentSoundTone should return null when past track duration with looping off")
    }

    @Test
    fun `test setWaveform changes the waveform used for playback`() = runBlocking {
        // Capture the waveform parameter passed to soundPlayer
        val waveformSlot = slot<SoundPlayer.Waveform>()

        // Setup a test MIDI track
        val testTrack = MidiTrack(
            name = "Test Track",
            notes = listOf(
                MidiNote(60, 80, 0.milliseconds, 300.milliseconds)
            ),
            totalDuration = 500.milliseconds
        )

        manager.setCurrentTrack(testTrack)

        // Setup for sound player to capture waveform parameter
        coEvery {
            soundPlayer.playTone(any(), any(), any(), capture(waveformSlot))
        } returns Unit

        // Default should be SINE
        val waveStartTime = Instant.fromEpochMilliseconds(0)
        manager.playCurrentSoundTone(waveStartTime)
        assertEquals(SoundPlayer.Waveform.SINE, waveformSlot.captured, "Default waveform should be SINE")

        // Change to SQUARE and verify
        manager.setWaveform(SoundPlayer.Waveform.SQUARE)
        manager.playCurrentSoundTone(waveStartTime)
        assertEquals(SoundPlayer.Waveform.SQUARE, waveformSlot.captured, "Waveform should be changed to SQUARE")
    }

    @Test
    fun `test getTotalDuration returns the correct duration`() = runTest {
        // When no track is loaded, duration should be ZERO
        assertEquals(0.seconds, manager.getTotalDuration(), "Duration should be ZERO when no track is loaded")

        // Load a track
        val testTrack = MidiTrack(
            name = "Test Track",
            notes = listOf(),
            totalDuration = 5.seconds
        )

        manager.setCurrentTrack(testTrack)

        // Now the duration should match the track
        assertEquals(5.seconds, manager.getTotalDuration(), "Duration should match the loaded track")
    }

    @Test
    fun `test release clears resources`() {
        // Setup a test track
        val testTrack = MidiTrack(
            name = "Test Track",
            notes = listOf(),
            totalDuration = 5.seconds
        )

        manager.setCurrentTrack(testTrack)

        // Call release
        manager.release()

        // Verify soundPlayer was released
        verify { soundPlayer.release() }

        // Verify currentTrack was cleared
        assertNull(manager.getTotalDuration().inWholeSeconds.takeIf { it > 0 },
            "Track should be cleared after release")
    }
}

/**
 * Tests for the WaveformGenerator utility class
 */
class WaveformGeneratorTest {

    @Test
    fun `test midiPitchToFrequency conversion`() {
        // Test with middle A (A4 = MIDI note 69 = 440Hz)
        val a4Frequency = WaveformGenerator.midiPitchToFrequency(69)
        assertEquals(440.0, a4Frequency, 0.01, "A4 should be 440Hz")

        // Test with middle C (C4 = MIDI note 60)
        val c4Frequency = WaveformGenerator.midiPitchToFrequency(60)
        assertEquals(261.63, c4Frequency, 0.01, "C4 should be ~261.63Hz")

        // Test with higher octave
        val c5Frequency = WaveformGenerator.midiPitchToFrequency(72)
        assertEquals(523.25, c5Frequency, 0.01, "C5 should be ~523.25Hz")
    }

    @Test
    fun `test midiVelocityToAmplitude conversion`() {
        // Test with full velocity
        val fullAmplitude = WaveformGenerator.midiVelocityToAmplitude(127)
        assertEquals(1.0, fullAmplitude, 0.01, "Velocity 127 should give amplitude 1.0")

        // Test with half velocity
        val halfAmplitude = WaveformGenerator.midiVelocityToAmplitude(64)
        assertEquals(0.5, halfAmplitude, 0.04, "Velocity 64 should give amplitude ~0.5")

        // Test with zero velocity
        val zeroAmplitude = WaveformGenerator.midiVelocityToAmplitude(0)
        assertEquals(0.0, zeroAmplitude, 0.01, "Velocity 0 should give amplitude 0.0")
    }

    @Test
    fun `test generateWaveform creates correct sample count`() {
        // Generate a 1-second sample at 44100Hz
        val samples = WaveformGenerator.generateWaveform(
            sampleRate = 44100,
            frequency = 440.0,
            amplitude = 1.0,
            duration = 1.seconds,
            waveform = SoundPlayer.Waveform.SINE
        )

        assertEquals(44100, samples.size, "1-second sample at 44100Hz should have 44100 samples")
    }

    @Test
    fun `test generateWaveform creates sine waveform with correct properties`() {
        // Generate a 1-second, 1Hz sine wave at 1000Hz sampling rate for easy testing
        val samples = WaveformGenerator.generateWaveform(
            sampleRate = 1000,
            frequency = 1.0, // 1 cycle per second
            amplitude = 1.0,
            duration = 1.seconds,
            waveform = SoundPlayer.Waveform.SINE
        )

        // Check sample count
        assertEquals(1000, samples.size, "Should have 1000 samples")

        // Check amplitude at key points
        assertTrue(abs(samples[0]) < 0.1, "Sample near start should be close to 0")
        assertTrue(abs(samples[250] - 1.0) < 0.1, "Sample at 1/4 should be close to 1 (peak)")
        assertTrue(abs(samples[500]) < 0.1, "Sample at 1/2 should be close to 0 (zero crossing)")
        assertTrue(abs(samples[750] + 1.0) < 0.1, "Sample at 3/4 should be close to -1 (trough)")
    }

    @Test
    fun `test all waveform types generate samples without errors`() {
        // Test that all waveform types can be generated
        for (waveform in SoundPlayer.Waveform.entries) {
            val samples = WaveformGenerator.generateWaveform(
                sampleRate = 44100,
                frequency = 440.0,
                amplitude = 0.8,
                duration = 100.milliseconds,
                waveform = waveform
            )

            // Check we have samples and they're within range
            assertTrue(samples.isNotEmpty(), "${waveform.name} should generate samples")
            assertTrue(samples.all { it in -0.8..0.8 },
                "${waveform.name} samples should be within amplitude range")
        }
    }
}

/**
 * Tests for the MidiNote class
 */
class MidiNoteTest {

    @Test
    fun `test isActiveAt returns true when time is within note duration`() {
        val note = MidiNote(
            pitch = 60,
            velocity = 80,
            startTime = 100.milliseconds,
            duration = 200.milliseconds
        )

        // Before note start
        assertFalse(note.isActiveAt(50.milliseconds), "Note should not be active before its start time")

        // At note start
        assertTrue(note.isActiveAt(100.milliseconds), "Note should be active at exactly its start time")

        // During note
        assertTrue(note.isActiveAt(200.milliseconds), "Note should be active during its duration")

        // At note end
        assertFalse(note.isActiveAt(300.milliseconds), "Note should not be active at exactly its end time")

        // After note end
        assertFalse(note.isActiveAt(350.milliseconds), "Note should not be active after its end time")
    }
}
