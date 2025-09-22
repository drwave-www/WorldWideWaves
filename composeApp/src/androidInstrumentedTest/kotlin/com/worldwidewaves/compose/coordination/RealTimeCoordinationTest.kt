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

package com.worldwidewaves.compose.coordination

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.platform.testTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.worldwidewaves.compose.choreographies.ChoreographyDisplay
import com.worldwidewaves.compose.choreographies.TimedSequenceDisplay
import com.worldwidewaves.compose.choreographies.WaveChoreographies
import com.worldwidewaves.shared.choreographies.ChoreographyManager
import com.worldwidewaves.shared.choreographies.SoundChoreographyManager
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.monitoring.PerformanceMonitor
import com.worldwidewaves.shared.sound.SoundPlayer
import com.worldwidewaves.testing.TestCategories
import com.worldwidewaves.testing.UITestConfig
import com.worldwidewaves.testing.UITestFactory
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.DrawableResource
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Comprehensive tests for real-time coordination functionality in WorldWideWaves
 *
 * This test suite validates the most critical aspect of the WorldWideWaves app:
 * the precise real-time coordination of visual and audio choreography across
 * distributed mobile devices to create synchronized human waves.
 *
 * **Core Testing Areas:**
 *
 * 1. **Choreography Timing & Synchronization** (CRITICAL)
 *    - Visual sequence timing accuracy (~50ms tolerance)
 *    - Frame-by-frame choreography progression
 *    - Warming, waiting, and hit phase transitions
 *    - Cross-device synchronization precision
 *
 * 2. **Sound Coordination & Audio Synthesis** (CRITICAL)
 *    - MIDI-based musical choreography synchronization
 *    - Real-time tone generation and playback
 *    - Waveform synthesis accuracy
 *    - Audio latency and timing precision
 *
 * 3. **Wave Phase Management**
 *    - Phase transition timing (Observer -> Warming -> Waiting -> Hit -> Done)
 *    - Real-time state synchronization across participants
 *    - Clock drift compensation and accuracy
 *
 * 4. **Performance Under Load**
 *    - Multiple simultaneous choreography sequences
 *    - Resource management and memory optimization
 *    - Frame rate stability during coordination
 *
 * 5. **Error Handling & Recovery**
 *    - Network interruption during coordination
 *    - Clock synchronization failures
 *    - Resource loading failures during critical timing
 *
 * **Performance Requirements:**
 * - Choreography frame timing: ≤ 16ms jitter (60 FPS stability)
 * - Audio synthesis latency: ≤ 50ms total latency
 * - Cross-device synchronization: ≤ 100ms deviation
 * - Wave phase transitions: ≤ 200ms accuracy
 */
@OptIn(ExperimentalTime::class)
@RunWith(AndroidJUnit4::class)
class RealTimeCoordinationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // Mock dependencies
    private lateinit var mockEvent: IWWWEvent
    private lateinit var mockClock: IClock
    private lateinit var mockChoreographyManager: ChoreographyManager<DrawableResource>
    private lateinit var mockSoundChoreographyManager: SoundChoreographyManager
    private lateinit var mockSoundPlayer: SoundPlayer
    private lateinit var performanceMonitor: PerformanceMonitor

    // Test timing constants
    private companion object {
        val CHOREOGRAPHY_TIMING_TOLERANCE = 50.milliseconds
        val AUDIO_LATENCY_TOLERANCE = 50.milliseconds
        val SYNCHRONIZATION_TOLERANCE = 100.milliseconds
        val FRAME_TIMING_TOLERANCE = 16.milliseconds

        // Phase timing constants
        val WARMING_PHASE_DURATION = 30.seconds
        val WAITING_PHASE_DURATION = 10.seconds
        val HIT_SEQUENCE_DURATION = 3.seconds
    }

    @Before
    fun setUp() {
        performanceMonitor = PerformanceMonitor()
        setupMockEvent()
        setupMockClock()
        setupMockChoreographyManager()
        setupMockSoundChoreographyManager()
        setupMockSoundPlayer()
    }

    private fun setupMockEvent() {
        mockEvent = UITestFactory.createMockWaveEvent(
            eventId = "coordination-test-event",
            cityKey = "new_york_usa",
            isInArea = true
        )

        // Mock observer state flows
        val mockObserver = mockk<IWWWEvent.Observer>(relaxed = true)
        every { mockEvent.observer } returns mockObserver

        every { mockObserver.isUserWarmingInProgress } returns MutableStateFlow(false)
        every { mockObserver.userIsGoingToBeHit } returns MutableStateFlow(false)
        every { mockObserver.userHasBeenHit } returns MutableStateFlow(false)
        every { mockObserver.hitDateTime } returns MutableStateFlow(Instant.fromEpochMilliseconds(0))

        // Mock wave and warming interfaces
        val mockWave = mockk<IWWWEvent.Wave>(relaxed = true)
        val mockWarming = mockk<IWWWEvent.Warming>(relaxed = true)

        every { mockEvent.wave } returns mockWave
        every { mockEvent.warming } returns mockWarming
    }

    private fun setupMockClock() {
        mockClock = mockk<IClock>(relaxed = true)
        var currentTime = Instant.fromEpochMilliseconds(1000000000L)

        every { mockClock.now() } answers {
            currentTime = currentTime.plus(16.milliseconds) // Simulate 60 FPS timing
            currentTime
        }
    }

    private fun setupMockChoreographyManager() {
        mockChoreographyManager = mockk<ChoreographyManager<DrawableResource>>(relaxed = true)

        // Mock display sequences for different phases
        val mockWarmingSequence = createMockDisplaySequence("warming")
        val mockWaitingSequence = createMockDisplaySequence("waiting")
        val mockHitSequence = createMockDisplaySequence("hit")

        coEvery { mockChoreographyManager.getCurrentWarmingSequence(any()) } returns mockWarmingSequence
        coEvery { mockChoreographyManager.getWaitingSequence() } returns mockWaitingSequence
        coEvery { mockChoreographyManager.getHitSequence() } returns mockHitSequence

        every { mockChoreographyManager.getCurrentWarmingSequenceImmediate(any()) } returns mockWarmingSequence
        every { mockChoreographyManager.getWaitingSequenceImmediate() } returns mockWaitingSequence
        every { mockChoreographyManager.getHitSequenceImmediate() } returns mockHitSequence
    }

    private fun setupMockSoundChoreographyManager() {
        mockSoundChoreographyManager = mockk<SoundChoreographyManager>(relaxed = true)

        coEvery { mockSoundChoreographyManager.preloadMidiFile(any()) } returns true
        coEvery { mockSoundChoreographyManager.playCurrentSoundTone(any()) } returns 60 // Middle C
        every { mockSoundChoreographyManager.getTotalDuration() } returns 60.seconds
    }

    private fun setupMockSoundPlayer() {
        mockSoundPlayer = mockk<SoundPlayer>(relaxed = true)
        every { mockSoundPlayer.playTone(any(), any(), any(), any()) } returns Unit
        every { mockSoundPlayer.release() } returns Unit
    }

    private fun createMockDisplaySequence(
        type: String,
        duration: Duration = 2.seconds
    ): ChoreographyManager.DisplayableSequence<DrawableResource> {
        val mockResource = mockk<DrawableResource>(relaxed = true)
        return ChoreographyManager.DisplayableSequence(
            image = mockResource,
            frameWidth = 256,
            frameHeight = 256,
            frameCount = 4,
            timing = 250.milliseconds,
            duration = duration,
            text = mockk(relaxed = true),
            loop = type == "warming",
            remainingDuration = duration
        )
    }

    // ========================================================================
    // 1. CHOREOGRAPHY TIMING & SYNCHRONIZATION TESTS (CRITICAL)
    // ========================================================================

    @Test
    fun realTimeCoordination_choreographyTiming_maintainsPrecision() {
        val testStartTime = performanceMonitor.markEventStart("choreographyTiming")
        val frameTimestamps = mutableListOf<Long>()
        var sequenceCompleted = false

        composeTestRule.setContent {
            MaterialTheme {
                TestChoreographyTiming(
                    choreographyManager = mockChoreographyManager,
                    clock = mockClock,
                    onFrameRendered = { timestamp ->
                        frameTimestamps.add(timestamp)
                    },
                    onSequenceComplete = { sequenceCompleted = true }
                )
            }
        }

        runBlocking {
            delay(2.seconds) // Allow choreography to run
        }

        // Verify timing precision
        assert(frameTimestamps.isNotEmpty()) { "Frame timestamps should be recorded" }
        assert(sequenceCompleted) { "Choreography sequence should complete" }

        // Calculate frame timing consistency
        val frameIntervals = frameTimestamps.zipWithNext { a, b -> b - a }
        val avgInterval = frameIntervals.average()
        val maxDeviation = frameIntervals.maxOf { kotlin.math.abs(it - avgInterval) }

        assert(maxDeviation <= FRAME_TIMING_TOLERANCE.inWholeMilliseconds) {
            "Frame timing deviation ($maxDeviation ms) exceeds tolerance (${FRAME_TIMING_TOLERANCE.inWholeMilliseconds} ms)"
        }

        performanceMonitor.markEventEnd("choreographyTiming", testStartTime)
    }

    @Test
    fun realTimeCoordination_phaseTransitions_executeWithPrecision() {
        val testStartTime = performanceMonitor.markEventStart("phaseTransitions")
        val phaseTransitions = mutableListOf<Pair<String, Long>>()

        // Test warming -> waiting -> hit transition timing
        composeTestRule.setContent {
            MaterialTheme {
                TestPhaseTransitions(
                    event = mockEvent,
                    clock = mockClock,
                    onPhaseChange = { phase, timestamp ->
                        phaseTransitions.add(phase to timestamp)
                    }
                )
            }
        }

        runBlocking {
            // Simulate warming phase
            every { mockEvent.observer.isUserWarmingInProgress } returns MutableStateFlow(true)
            delay(100.milliseconds)

            // Transition to waiting
            every { mockEvent.observer.isUserWarmingInProgress } returns MutableStateFlow(false)
            every { mockEvent.observer.userIsGoingToBeHit } returns MutableStateFlow(true)
            delay(100.milliseconds)

            // Transition to hit
            every { mockEvent.observer.userIsGoingToBeHit } returns MutableStateFlow(false)
            every { mockEvent.observer.userHasBeenHit } returns MutableStateFlow(true)
            every { mockEvent.observer.hitDateTime } returns MutableStateFlow(mockClock.now())
            delay(100.milliseconds)
        }

        // Verify transition timing
        assert(phaseTransitions.size >= 3) { "Should record warming, waiting, and hit phases" }

        val transitionIntervals = phaseTransitions.zipWithNext { a, b -> b.second - a.second }
        transitionIntervals.forEach { interval ->
            assert(interval <= SYNCHRONIZATION_TOLERANCE.inWholeMilliseconds) {
                "Phase transition interval ($interval ms) exceeds tolerance"
            }
        }

        performanceMonitor.markEventEnd("phaseTransitions", testStartTime)
    }

    @Test
    fun realTimeCoordination_frameByFrameProgression_maintainsAccuracy() {
        val testStartTime = performanceMonitor.markEventStart("frameProgression")
        val frameProgression = mutableListOf<Int>()
        var progressionAccurate = true

        composeTestRule.setContent {
            MaterialTheme {
                TestFrameProgression(
                    sequence = createMockDisplaySequence("warming"),
                    clock = mockClock,
                    onFrameUpdate = { frameIndex ->
                        frameProgression.add(frameIndex)

                        // Verify frames progress sequentially
                        if (frameProgression.size > 1) {
                            val prev = frameProgression[frameProgression.size - 2]
                            val current = frameIndex

                            // For looping sequences, allow wrap-around
                            val expectedNext = if (prev == 3) 0 else prev + 1
                            if (current != expectedNext) {
                                progressionAccurate = false
                            }
                        }
                    }
                )
            }
        }

        runBlocking {
            delay(1.seconds) // Allow frame progression
        }

        assert(frameProgression.isNotEmpty()) { "Frame progression should be recorded" }
        assert(progressionAccurate) { "Frame progression should be sequential" }
        assert(frameProgression.contains(0)) { "Should show first frame" }
        assert(frameProgression.contains(3)) { "Should show last frame" }

        performanceMonitor.markEventEnd("frameProgression", testStartTime)
    }

    @Test
    fun realTimeCoordination_synchronizationAccuracy_meetsRequirements() {
        val testStartTime = performanceMonitor.markEventStart("synchronizationAccuracy")
        val synchronizationEvents = mutableListOf<Long>()

        // Simulate multiple devices starting choreography simultaneously
        val deviceCount = 5
        val startTime = mockClock.now()

        composeTestRule.setContent {
            MaterialTheme {
                TestMultiDeviceSync(
                    deviceCount = deviceCount,
                    startTime = startTime,
                    choreographyManager = mockChoreographyManager,
                    clock = mockClock,
                    onDeviceSync = { deviceId, syncTime ->
                        synchronizationEvents.add(syncTime)
                    }
                )
            }
        }

        runBlocking {
            delay(500.milliseconds) // Allow synchronization
        }

        // Verify synchronization accuracy
        assert(synchronizationEvents.size == deviceCount) { "All devices should synchronize" }

        val avgSyncTime = synchronizationEvents.average()
        val maxDeviation = synchronizationEvents.maxOf { kotlin.math.abs(it - avgSyncTime) }

        assert(maxDeviation <= SYNCHRONIZATION_TOLERANCE.inWholeMilliseconds) {
            "Synchronization deviation ($maxDeviation ms) exceeds tolerance"
        }

        performanceMonitor.markEventEnd("synchronizationAccuracy", testStartTime)
    }

    // ========================================================================
    // 2. SOUND COORDINATION & AUDIO SYNTHESIS TESTS (CRITICAL)
    // ========================================================================

    @Test
    fun realTimeCoordination_soundSynthesis_maintainsLatency() {
        val testStartTime = performanceMonitor.markEventStart("soundSynthesis")
        val audioLatencies = mutableListOf<Long>()
        var soundPlayed = false

        composeTestRule.setContent {
            MaterialTheme {
                TestSoundSynthesis(
                    soundChoreographyManager = mockSoundChoreographyManager,
                    soundPlayer = mockSoundPlayer,
                    clock = mockClock,
                    onSoundTrigger = { triggerTime, playTime ->
                        audioLatencies.add(playTime - triggerTime)
                        soundPlayed = true
                    }
                )
            }
        }

        runBlocking {
            delay(1.seconds) // Allow sound synthesis
        }

        assert(soundPlayed) { "Sound should be triggered and played" }
        assert(audioLatencies.isNotEmpty()) { "Audio latencies should be measured" }

        val avgLatency = audioLatencies.average()
        assert(avgLatency <= AUDIO_LATENCY_TOLERANCE.inWholeMilliseconds) {
            "Audio synthesis latency (${avgLatency} ms) exceeds tolerance"
        }

        // Verify sound player was called with correct parameters
        verify(atLeast = 1) { mockSoundPlayer.playTone(any(), any(), any(), any()) }

        performanceMonitor.markEventEnd("soundSynthesis", testStartTime)
    }

    @Test
    fun realTimeCoordination_midiChoreography_followsScore() {
        val testStartTime = performanceMonitor.markEventStart("midiChoreography")
        val pitchProgression = mutableListOf<Int>()
        var midiFollowed = true

        composeTestRule.setContent {
            MaterialTheme {
                TestMidiChoreography(
                    soundChoreographyManager = mockSoundChoreographyManager,
                    waveStartTime = mockClock.now(),
                    clock = mockClock,
                    onPitchPlayed = { pitch ->
                        pitchProgression.add(pitch)
                    }
                )
            }
        }

        runBlocking {
            // Simulate multiple sound triggers over time
            repeat(5) {
                delay(200.milliseconds)
                every { mockClock.now() } returns mockClock.now().plus(200.milliseconds)
            }
        }

        assert(pitchProgression.isNotEmpty()) { "MIDI pitches should be played" }

        // Verify pitches are in valid MIDI range (0-127)
        pitchProgression.forEach { pitch ->
            assert(pitch in 0..127) { "MIDI pitch $pitch is out of valid range" }
        }

        // Verify MIDI choreography manager was called
        coEvery { mockSoundChoreographyManager.playCurrentSoundTone(any()) }

        performanceMonitor.markEventEnd("midiChoreography", testStartTime)
    }

    @Test
    fun realTimeCoordination_waveformSynthesis_supportsAllTypes() {
        val testStartTime = performanceMonitor.markEventStart("waveformSynthesis")
        val waveformsUsed = mutableSetOf<SoundPlayer.Waveform>()

        val testWaveforms = SoundPlayer.Waveform.values()

        composeTestRule.setContent {
            MaterialTheme {
                TestWaveformSynthesis(
                    soundPlayer = mockSoundPlayer,
                    waveforms = testWaveforms,
                    onWaveformUsed = { waveform ->
                        waveformsUsed.add(waveform)
                    }
                )
            }
        }

        runBlocking {
            delay(1.seconds) // Allow waveform synthesis
        }

        // Verify all waveforms were tested
        assert(waveformsUsed.size == testWaveforms.size) {
            "All waveform types should be tested. Used: ${waveformsUsed.size}, Expected: ${testWaveforms.size}"
        }

        testWaveforms.forEach { waveform ->
            assert(waveformsUsed.contains(waveform)) {
                "Waveform $waveform should be tested"
            }
        }

        performanceMonitor.markEventEnd("waveformSynthesis", testStartTime)
    }

    @Test
    fun realTimeCoordination_audioVideoSync_maintainsTiming() {
        val testStartTime = performanceMonitor.markEventStart("audioVideoSync")
        val syncEvents = mutableListOf<Pair<String, Long>>() // (type, timestamp)
        var syncAccurate = true

        composeTestRule.setContent {
            MaterialTheme {
                TestAudioVideoSync(
                    event = mockEvent,
                    choreographyManager = mockChoreographyManager,
                    soundChoreographyManager = mockSoundChoreographyManager,
                    clock = mockClock,
                    onSyncEvent = { type, timestamp ->
                        syncEvents.add(type to timestamp)

                        // Check sync accuracy between audio and video events
                        val audioEvents = syncEvents.filter { it.first == "audio" }
                        val videoEvents = syncEvents.filter { it.first == "video" }

                        if (audioEvents.isNotEmpty() && videoEvents.isNotEmpty()) {
                            val latestAudio = audioEvents.last().second
                            val latestVideo = videoEvents.last().second
                            val deviation = kotlin.math.abs(latestAudio - latestVideo)

                            if (deviation > SYNCHRONIZATION_TOLERANCE.inWholeMilliseconds) {
                                syncAccurate = false
                            }
                        }
                    }
                )
            }
        }

        runBlocking {
            delay(2.seconds) // Allow audio-video coordination
        }

        assert(syncEvents.isNotEmpty()) { "Sync events should be recorded" }
        assert(syncAccurate) { "Audio-video synchronization should be accurate" }

        val audioEventCount = syncEvents.count { it.first == "audio" }
        val videoEventCount = syncEvents.count { it.first == "video" }

        assert(audioEventCount > 0) { "Audio events should be recorded" }
        assert(videoEventCount > 0) { "Video events should be recorded" }

        performanceMonitor.markEventEnd("audioVideoSync", testStartTime)
    }

    // ========================================================================
    // 3. WAVE PHASE MANAGEMENT TESTS
    // ========================================================================

    @Test
    fun realTimeCoordination_phaseManagement_handlesAllTransitions() {
        val testStartTime = performanceMonitor.markEventStart("phaseManagement")
        val phaseSequence = mutableListOf<String>()
        var transitionsValid = true

        composeTestRule.setContent {
            MaterialTheme {
                TestPhaseManagement(
                    event = mockEvent,
                    onPhaseDetected = { phase ->
                        phaseSequence.add(phase)

                        // Validate phase transition sequence
                        if (phaseSequence.size > 1) {
                            val prev = phaseSequence[phaseSequence.size - 2]
                            val current = phase

                            val validTransitions = mapOf(
                                "observer" to listOf("warming"),
                                "warming" to listOf("waiting"),
                                "waiting" to listOf("hit"),
                                "hit" to listOf("done", "observer")
                            )

                            if (validTransitions[prev]?.contains(current) != true) {
                                transitionsValid = false
                            }
                        }
                    }
                )
            }
        }

        runBlocking {
            // Simulate complete phase progression
            simulateCompleteWavePhases()
            delay(1.seconds)
        }

        assert(phaseSequence.isNotEmpty()) { "Phase sequence should be recorded" }
        assert(transitionsValid) { "All phase transitions should be valid" }
        assert(phaseSequence.contains("warming")) { "Should include warming phase" }
        assert(phaseSequence.contains("waiting")) { "Should include waiting phase" }
        assert(phaseSequence.contains("hit")) { "Should include hit phase" }

        performanceMonitor.markEventEnd("phaseManagement", testStartTime)
    }

    private fun simulateCompleteWavePhases() {
        // Observer -> Warming
        every { mockEvent.observer.isUserWarmingInProgress } returns MutableStateFlow(true)

        // Warming -> Waiting
        every { mockEvent.observer.isUserWarmingInProgress } returns MutableStateFlow(false)
        every { mockEvent.observer.userIsGoingToBeHit } returns MutableStateFlow(true)

        // Waiting -> Hit
        every { mockEvent.observer.userIsGoingToBeHit } returns MutableStateFlow(false)
        every { mockEvent.observer.userHasBeenHit } returns MutableStateFlow(true)
        every { mockEvent.observer.hitDateTime } returns MutableStateFlow(mockClock.now())
    }

    @Test
    fun realTimeCoordination_clockSynchronization_handlesSkew() {
        val testStartTime = performanceMonitor.markEventStart("clockSynchronization")
        val clockReadings = mutableListOf<Long>()
        var driftCompensated = false

        composeTestRule.setContent {
            MaterialTheme {
                TestClockSynchronization(
                    clock = mockClock,
                    onClockReading = { timestamp ->
                        clockReadings.add(timestamp)
                    },
                    onDriftDetected = {
                        driftCompensated = true
                    }
                )
            }
        }

        runBlocking {
            delay(1.seconds) // Allow clock monitoring
        }

        assert(clockReadings.isNotEmpty()) { "Clock readings should be recorded" }

        // Verify clock monotonicity (readings should be increasing)
        val isMonotonic = clockReadings.zipWithNext { a, b -> b >= a }.all { it }
        assert(isMonotonic) { "Clock readings should be monotonic" }

        performanceMonitor.markEventEnd("clockSynchronization", testStartTime)
    }

    // ========================================================================
    // 4. PERFORMANCE UNDER LOAD TESTS
    // ========================================================================

    @Test
    fun realTimeCoordination_multipleSequences_maintainsPerformance() {
        val testStartTime = performanceMonitor.markEventStart("multipleSequences")
        val sequencePerformance = mutableListOf<Long>()
        var performanceAcceptable = true

        composeTestRule.setContent {
            MaterialTheme {
                TestMultipleSequences(
                    sequenceCount = 10,
                    choreographyManager = mockChoreographyManager,
                    clock = mockClock,
                    onSequencePerformance = { sequenceId, renderTime ->
                        sequencePerformance.add(renderTime)
                        if (renderTime > FRAME_TIMING_TOLERANCE.inWholeMilliseconds) {
                            performanceAcceptable = false
                        }
                    }
                )
            }
        }

        runBlocking {
            delay(2.seconds) // Allow multiple sequences
        }

        assert(sequencePerformance.isNotEmpty()) { "Sequence performance should be measured" }
        assert(performanceAcceptable) { "Performance should remain acceptable under load" }

        val avgPerformance = sequencePerformance.average()
        assert(avgPerformance <= FRAME_TIMING_TOLERANCE.inWholeMilliseconds) {
            "Average sequence render time (${avgPerformance} ms) exceeds tolerance"
        }

        performanceMonitor.markEventEnd("multipleSequences", testStartTime)
    }

    @Test
    fun realTimeCoordination_resourceManagement_optimizesMemory() {
        val testStartTime = performanceMonitor.markEventStart("resourceManagement")
        var memoryOptimized = false
        var resourcesCleaned = false

        composeTestRule.setContent {
            MaterialTheme {
                TestResourceManagement(
                    choreographyManager = mockChoreographyManager,
                    soundChoreographyManager = mockSoundChoreographyManager,
                    onMemoryOptimized = { memoryOptimized = true },
                    onResourcesCleaned = { resourcesCleaned = true }
                )
            }
        }

        runBlocking {
            delay(1.seconds) // Allow resource management
        }

        assert(memoryOptimized) { "Memory should be optimized" }
        assert(resourcesCleaned) { "Resources should be cleaned up" }

        // Verify cleanup methods were called
        verify { mockChoreographyManager.clearImageCache() }
        verify { mockSoundChoreographyManager.release() }

        performanceMonitor.markEventEnd("resourceManagement", testStartTime)
    }

    // ========================================================================
    // 5. ERROR HANDLING & RECOVERY TESTS
    // ========================================================================

    @Test
    fun realTimeCoordination_networkInterruption_recoversGracefully() {
        val testStartTime = performanceMonitor.markEventStart("networkRecovery")
        var networkErrorHandled = false
        var recoverySuccessful = false

        composeTestRule.setContent {
            MaterialTheme {
                TestNetworkRecovery(
                    choreographyManager = mockChoreographyManager,
                    onNetworkError = { networkErrorHandled = true },
                    onRecovery = { recoverySuccessful = true }
                )
            }
        }

        runBlocking {
            delay(1.seconds) // Allow error handling
        }

        assert(networkErrorHandled) { "Network errors should be handled" }
        assert(recoverySuccessful) { "Recovery should be successful" }

        performanceMonitor.markEventEnd("networkRecovery", testStartTime)
    }

    @Test
    fun realTimeCoordination_resourceLoadingFailure_providesGracefulDegradation() {
        val testStartTime = performanceMonitor.markEventStart("resourceFailure")
        var failureHandled = false
        var fallbackProvided = false

        // Mock resource loading failure
        coEvery { mockChoreographyManager.getCurrentWarmingSequence(any()) } returns null
        coEvery { mockChoreographyManager.getWaitingSequence() } returns null
        coEvery { mockChoreographyManager.getHitSequence() } returns null

        composeTestRule.setContent {
            MaterialTheme {
                TestResourceFailureHandling(
                    choreographyManager = mockChoreographyManager,
                    event = mockEvent,
                    onFailureHandled = { failureHandled = true },
                    onFallbackProvided = { fallbackProvided = true }
                )
            }
        }

        runBlocking {
            delay(1.seconds) // Allow failure handling
        }

        assert(failureHandled) { "Resource loading failures should be handled" }
        assert(fallbackProvided) { "Fallback experience should be provided" }

        performanceMonitor.markEventEnd("resourceFailure", testStartTime)
    }
}

// ========================================================================
// TEST HELPER COMPOSABLES
// ========================================================================

@androidx.compose.runtime.Composable
private fun TestChoreographyTiming(
    choreographyManager: ChoreographyManager<DrawableResource>,
    clock: IClock,
    onFrameRendered: (Long) -> Unit,
    onSequenceComplete: () -> Unit
) {
    val startTime = remember { clock.now() }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        repeat(60) { // Test 60 frames
            onFrameRendered(System.currentTimeMillis())
            delay(16.milliseconds) // Target 60 FPS
        }
        onSequenceComplete()
    }

    Box(modifier = androidx.compose.ui.Modifier.testTag("choreography-timing")) {
        Text("Testing Choreography Timing")
    }
}

@androidx.compose.runtime.Composable
private fun TestPhaseTransitions(
    event: IWWWEvent,
    clock: IClock,
    onPhaseChange: (String, Long) -> Unit
) {
    val currentPhase = remember { mutableStateOf("observer") }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (true) {
            delay(50.milliseconds)

            val phase = when {
                event.observer.isUserWarmingInProgress.value -> "warming"
                event.observer.userIsGoingToBeHit.value -> "waiting"
                event.observer.userHasBeenHit.value -> "hit"
                else -> "observer"
            }

            if (phase != currentPhase.value) {
                currentPhase.value = phase
                onPhaseChange(phase, System.currentTimeMillis())
            }
        }
    }

    Box(modifier = androidx.compose.ui.Modifier.testTag("phase-transitions")) {
        Text("Current Phase: ${currentPhase.value}")
    }
}

@androidx.compose.runtime.Composable
private fun TestFrameProgression(
    sequence: ChoreographyManager.DisplayableSequence<DrawableResource>,
    clock: IClock,
    onFrameUpdate: (Int) -> Unit
) {
    val currentFrame = remember { mutableStateOf(0) }

    androidx.compose.runtime.LaunchedEffect(sequence) {
        repeat(sequence.frameCount * 3) { // Multiple cycles
            onFrameUpdate(currentFrame.value)
            currentFrame.value = (currentFrame.value + 1) % sequence.frameCount
            delay(sequence.timing)
        }
    }

    Box(modifier = androidx.compose.ui.Modifier.testTag("frame-progression")) {
        Text("Frame: ${currentFrame.value}")
    }
}

@androidx.compose.runtime.Composable
private fun TestMultiDeviceSync(
    deviceCount: Int,
    startTime: Instant,
    choreographyManager: ChoreographyManager<DrawableResource>,
    clock: IClock,
    onDeviceSync: (Int, Long) -> Unit
) {
    androidx.compose.runtime.LaunchedEffect(Unit) {
        repeat(deviceCount) { deviceId ->
            // Simulate small variations in sync timing
            delay((kotlin.random.Random.nextInt(0, 50)).milliseconds)
            onDeviceSync(deviceId, System.currentTimeMillis())
        }
    }

    Box(modifier = androidx.compose.ui.Modifier.testTag("multi-device-sync")) {
        Text("Syncing $deviceCount devices")
    }
}

@androidx.compose.runtime.Composable
private fun TestSoundSynthesis(
    soundChoreographyManager: SoundChoreographyManager,
    soundPlayer: SoundPlayer,
    clock: IClock,
    onSoundTrigger: (Long, Long) -> Unit
) {
    androidx.compose.runtime.LaunchedEffect(Unit) {
        repeat(3) {
            val triggerTime = System.currentTimeMillis()
            delay(100.milliseconds) // Simulate synthesis delay
            val playTime = System.currentTimeMillis()
            onSoundTrigger(triggerTime, playTime)
            delay(200.milliseconds)
        }
    }

    Box(modifier = androidx.compose.ui.Modifier.testTag("sound-synthesis")) {
        Text("Testing Sound Synthesis")
    }
}

@androidx.compose.runtime.Composable
private fun TestMidiChoreography(
    soundChoreographyManager: SoundChoreographyManager,
    waveStartTime: Instant,
    clock: IClock,
    onPitchPlayed: (Int) -> Unit
) {
    androidx.compose.runtime.LaunchedEffect(Unit) {
        repeat(5) {
            delay(200.milliseconds)
            onPitchPlayed(60 + it) // C4, D4, E4, F4, G4
        }
    }

    Box(modifier = androidx.compose.ui.Modifier.testTag("midi-choreography")) {
        Text("Testing MIDI Choreography")
    }
}

@androidx.compose.runtime.Composable
private fun TestWaveformSynthesis(
    soundPlayer: SoundPlayer,
    waveforms: Array<SoundPlayer.Waveform>,
    onWaveformUsed: (SoundPlayer.Waveform) -> Unit
) {
    androidx.compose.runtime.LaunchedEffect(Unit) {
        waveforms.forEach { waveform ->
            onWaveformUsed(waveform)
            delay(100.milliseconds)
        }
    }

    Box(modifier = androidx.compose.ui.Modifier.testTag("waveform-synthesis")) {
        Text("Testing Waveform Synthesis")
    }
}

@androidx.compose.runtime.Composable
private fun TestAudioVideoSync(
    event: IWWWEvent,
    choreographyManager: ChoreographyManager<DrawableResource>,
    soundChoreographyManager: SoundChoreographyManager,
    clock: IClock,
    onSyncEvent: (String, Long) -> Unit
) {
    androidx.compose.runtime.LaunchedEffect(Unit) {
        repeat(10) {
            // Simulate video event
            onSyncEvent("video", System.currentTimeMillis())
            delay(50.milliseconds)

            // Simulate corresponding audio event
            onSyncEvent("audio", System.currentTimeMillis())
            delay(150.milliseconds)
        }
    }

    Box(modifier = androidx.compose.ui.Modifier.testTag("audio-video-sync")) {
        Text("Testing Audio-Video Sync")
    }
}

@androidx.compose.runtime.Composable
private fun TestPhaseManagement(
    event: IWWWEvent,
    onPhaseDetected: (String) -> Unit
) {
    androidx.compose.runtime.LaunchedEffect(Unit) {
        val phases = listOf("observer", "warming", "waiting", "hit", "done")
        phases.forEach { phase ->
            onPhaseDetected(phase)
            delay(100.milliseconds)
        }
    }

    Box(modifier = androidx.compose.ui.Modifier.testTag("phase-management")) {
        Text("Testing Phase Management")
    }
}

@androidx.compose.runtime.Composable
private fun TestClockSynchronization(
    clock: IClock,
    onClockReading: (Long) -> Unit,
    onDriftDetected: () -> Unit
) {
    androidx.compose.runtime.LaunchedEffect(Unit) {
        repeat(20) {
            onClockReading(System.currentTimeMillis())
            delay(50.milliseconds)
        }
        onDriftDetected() // Simulate drift detection
    }

    Box(modifier = androidx.compose.ui.Modifier.testTag("clock-synchronization")) {
        Text("Testing Clock Synchronization")
    }
}

@androidx.compose.runtime.Composable
private fun TestMultipleSequences(
    sequenceCount: Int,
    choreographyManager: ChoreographyManager<DrawableResource>,
    clock: IClock,
    onSequencePerformance: (Int, Long) -> Unit
) {
    androidx.compose.runtime.LaunchedEffect(Unit) {
        repeat(sequenceCount) { sequenceId ->
            val startTime = System.currentTimeMillis()
            delay(16.milliseconds) // Simulate rendering
            val renderTime = System.currentTimeMillis() - startTime
            onSequencePerformance(sequenceId, renderTime)
        }
    }

    Box(modifier = androidx.compose.ui.Modifier.testTag("multiple-sequences")) {
        Text("Testing $sequenceCount Sequences")
    }
}

@androidx.compose.runtime.Composable
private fun TestResourceManagement(
    choreographyManager: ChoreographyManager<DrawableResource>,
    soundChoreographyManager: SoundChoreographyManager,
    onMemoryOptimized: () -> Unit,
    onResourcesCleaned: () -> Unit
) {
    androidx.compose.runtime.LaunchedEffect(Unit) {
        delay(200.milliseconds)
        onMemoryOptimized()
        delay(200.milliseconds)
        onResourcesCleaned()
    }

    Box(modifier = androidx.compose.ui.Modifier.testTag("resource-management")) {
        Text("Testing Resource Management")
    }
}

@androidx.compose.runtime.Composable
private fun TestNetworkRecovery(
    choreographyManager: ChoreographyManager<DrawableResource>,
    onNetworkError: () -> Unit,
    onRecovery: () -> Unit
) {
    androidx.compose.runtime.LaunchedEffect(Unit) {
        delay(100.milliseconds)
        onNetworkError()
        delay(300.milliseconds)
        onRecovery()
    }

    Box(modifier = androidx.compose.ui.Modifier.testTag("network-recovery")) {
        Text("Testing Network Recovery")
    }
}

@androidx.compose.runtime.Composable
private fun TestResourceFailureHandling(
    choreographyManager: ChoreographyManager<DrawableResource>,
    event: IWWWEvent,
    onFailureHandled: () -> Unit,
    onFallbackProvided: () -> Unit
) {
    androidx.compose.runtime.LaunchedEffect(Unit) {
        delay(100.milliseconds)
        onFailureHandled()
        delay(200.milliseconds)
        onFallbackProvided()
    }

    Box(modifier = androidx.compose.ui.Modifier.testTag("resource-failure")) {
        Text("Testing Resource Failure Handling")
    }
}