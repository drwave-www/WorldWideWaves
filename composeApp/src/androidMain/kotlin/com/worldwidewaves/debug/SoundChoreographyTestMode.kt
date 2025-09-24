package com.worldwidewaves.debug

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

// Show test-mode UI only in debug builds ------------------------------------
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.worldwidewaves.BuildConfig
import com.worldwidewaves.R
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.sound.SoundPlayer
import com.worldwidewaves.shared.sound.WaveformGenerator
import com.worldwidewaves.shared.utils.Log
import com.worldwidewaves.theme.onPrimaryLight
import com.worldwidewaves.theme.onQuinaryLight
import com.worldwidewaves.theme.primaryLight
import com.worldwidewaves.theme.quinaryLight
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

/**
 * Test mode for sound choreography in WorldWideWaves.
 *
 * This class provides tools for testing the sound choreography system, including:
 * - Manual note triggering
 * - Simulated wave progression with multiple users
 * - Visualization of currently playing notes
 */
class SoundChoreographyTestMode {
    companion object {
        internal const val TAG = "SoundChoreographyTestMode"

        // Global test mode state
        private var _isEnabled = mutableStateOf(false)

        /**
         * Check if test mode is currently enabled
         */
        fun isEnabled() = _isEnabled.value

        /**
         * Enable or disable test mode
         */
        fun setEnabled(enabled: Boolean) {
            _isEnabled.value = enabled
            Log.d(TAG, "Test mode ${if (enabled) "enabled" else "disabled"}")
        }

        /**
         * Toggle test mode state
         */
        fun toggle() {
            setEnabled(!isEnabled())
        }

        // Currently playing note information for visualization
        private var _lastPlayedNote = mutableIntStateOf(-1)
        private var _notePlayingTime = MutableLongStateOf(0L)

        /**
         * Record that a note was played for visualization
         */
        fun noteWasPlayed(midiNote: Int) {
            _lastPlayedNote.intValue = midiNote
            _notePlayingTime.longValue = System.currentTimeMillis()
            Log.d(TAG, "Note played: $midiNote")
        }

        /**
         * Get the last played note
         */
        fun getLastPlayedNote() = _lastPlayedNote.intValue

        /**
         * Get the time when the last note was played
         */
        fun getNotePlayingTime() = _notePlayingTime.longValue
    }
}

/**
 * Mutable long state holder for Compose
 */
private class MutableLongStateOf(
    initialValue: Long,
) {
    private val state = mutableLongStateOf(initialValue)
    var longValue: Long
        get() = state.longValue
        set(value) {
            state.longValue = value
        }
}

/**
 * Main Composable for the sound choreography test mode UI
 */
@Composable
fun SoundChoreographyTestModeOverlay(
    event: IWWWEvent,
    modifier: Modifier = Modifier,
    onClose: () -> Unit = {},
) {
    // Ensure this overlay is never shown in release builds
    if (!BuildConfig.DEBUG) return
    if (!SoundChoreographyTestMode.isEnabled()) return

    var expanded by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    var simulationJob by remember { mutableStateOf<Job?>(null) }
    var isSimulating by remember { mutableStateOf(false) }

    // Test parameters
    var userCount by remember { mutableIntStateOf(10000) }
    var waveDuration by remember { mutableFloatStateOf(5f) } // seconds
    var waveformType by remember { mutableStateOf(SoundPlayer.Waveform.SINE) }

    // Last played note visualization
    val lastPlayedNote = remember { mutableIntStateOf(SoundChoreographyTestMode.getLastPlayedNote()) }
    val notePlayingTime = remember { MutableLongStateOf(SoundChoreographyTestMode.getNotePlayingTime()) }

    // Update note visualization
    LaunchedEffect(Unit) {
        while (true) {
            delay(100)
            lastPlayedNote.intValue = SoundChoreographyTestMode.getLastPlayedNote()
            notePlayingTime.longValue = SoundChoreographyTestMode.getNotePlayingTime()
        }
    }

    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
            ),
    ) {
        // Header
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(primaryLight)
                    .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_music_note),
                    contentDescription = null,
                    tint = Color.White,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Sound Choreography Test Mode",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
            }

            Row {
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        painter =
                            painterResource(
                                if (expanded) {
                                    R.drawable.ic_keyboard_arrow_up
                                } else {
                                    R.drawable.ic_keyboard_arrow_down
                                },
                            ),
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = Color.White,
                    )
                }

                IconButton(onClick = onClose) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = "Close",
                        tint = Color.White,
                    )
                }
            }
        }

        // Content
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
            ) {
                // Note visualization
                NoteVisualization(lastPlayedNote.intValue, notePlayingTime.longValue)

                Spacer(modifier = Modifier.height(16.dp))

                // Manual note triggering
                Text(
                    text = "Manual Note Triggering",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )

                Spacer(modifier = Modifier.height(8.dp))

                ManualNoteTriggers(event)

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                // Simulation controls
                Text(
                    text = "Wave Simulation",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )

                Spacer(modifier = Modifier.height(8.dp))

                // User count slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Users:",
                        modifier = Modifier.width(80.dp),
                    )
                    Slider(
                        value = userCount.toFloat(),
                        onValueChange = { userCount = it.roundToInt() },
                        valueRange = 2f..20000f,
                        steps = 18,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "$userCount",
                        modifier = Modifier.width(30.dp),
                        textAlign = TextAlign.End,
                    )
                }

                // Wave duration slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Duration:",
                        modifier = Modifier.width(80.dp),
                    )
                    Slider(
                        value = waveDuration,
                        onValueChange = { waveDuration = it },
                        valueRange = 5f..30f,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${waveDuration.roundToInt()}s",
                        modifier = Modifier.width(30.dp),
                        textAlign = TextAlign.End,
                    )
                }

                // Waveform selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Waveform:",
                        modifier = Modifier.width(80.dp),
                    )

                    WaveformSelector(
                        selectedWaveform = waveformType,
                        onWaveformSelected = { waveformType = it },
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Simulation control buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    Button(
                        onClick = {
                            if (isSimulating) {
                                simulationJob?.cancel()
                                isSimulating = false
                            } else {
                                isSimulating = true
                                simulationJob =
                                    scope.launch {
                                        runWaveSimulation(
                                            event = event,
                                            userCount = userCount,
                                            durationSeconds = waveDuration.roundToInt(),
                                            waveform = waveformType,
                                        )
                                        isSimulating = false
                                    }
                            }
                        },
                    ) {
                        Icon(
                            painter =
                                painterResource(
                                    if (isSimulating) {
                                        R.drawable.ic_stop
                                    } else {
                                        R.drawable.ic_play_arrow
                                    },
                                ),
                            contentDescription = if (isSimulating) "Stop Simulation" else "Start Simulation",
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = if (isSimulating) "Stop Simulation" else "Start Simulation")
                    }
                }
            }
        }
    }
}

/**
 * Composable for visualizing the currently/last played note
 */
@Composable
private fun NoteVisualization(
    lastPlayedNote: Int,
    notePlayingTime: Long,
) {
    val timeSinceNotePlayed = System.currentTimeMillis() - notePlayingTime
    val isRecentlyPlayed = timeSinceNotePlayed < 1000

    val noteAlpha by animateFloatAsState(
        targetValue = if (isRecentlyPlayed) 1f else 0.3f,
        label = "Note Visualization Alpha",
    )

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (lastPlayedNote >= 0) {
                // Convert MIDI note to note name
                val noteName = getNoteName(lastPlayedNote)
                val octave = lastPlayedNote / 12 - 1

                Text(
                    text = "$noteName$octave",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.alpha(noteAlpha),
                )

                Text(
                    text = "MIDI: $lastPlayedNote • ${WaveformGenerator.midiPitchToFrequency(lastPlayedNote).roundToInt()} Hz",
                    fontSize = 12.sp,
                    modifier = Modifier.alpha(noteAlpha * com.worldwidewaves.constants.AndroidUIConstants.Audio.DEFAULT_VOLUME),
                )
            } else {
                Text(
                    text = "No note played yet",
                    fontSize = 16.sp,
                    color = Color.Gray,
                )
            }
        }
    }
}

/**
 * Convert MIDI note number to note name
 */
private fun getNoteName(midiNote: Int): String {
    val noteNames = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    return noteNames[midiNote % com.worldwidewaves.shared.WWWGlobals.Midi.OCTAVE_DIVISOR]
}

/**
 * Composable for manual note triggering
 */
@Composable
private fun ManualNoteTriggers(event: IWWWEvent) {
    val scope = rememberCoroutineScope()
    val clock: IClock by inject(IClock::class.java)

    // Piano keyboard-like layout
    Column(modifier = Modifier.fillMaxWidth()) {
        // C Major scale notes (C4 to C5)
        val baseNote = 60 // C4
        val cMajorScale = intArrayOf(0, 2, 4, 5, 7, 9, 11, 12) // Steps in a C major scale

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            cMajorScale.forEach { step ->
                val note = baseNote + step
                NoteButton(
                    note = note,
                    onClick = {
                        scope.launch {
                            // Override the event's start time to use current time for testing
                            val noteNumber = event.warming.playCurrentSoundChoreographyTone()
                            if (noteNumber != null) {
                                SoundChoreographyTestMode.noteWasPlayed(noteNumber)
                            }
                        }
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Random note button
        Button(
            onClick = {
                scope.launch {
                    val noteNumber = event.warming.playCurrentSoundChoreographyTone()
                    if (noteNumber != null) {
                        SoundChoreographyTestMode.noteWasPlayed(noteNumber)
                    }
                }
            },
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Text(text = "Play Random Note")
        }
    }
}

/**
 * Composable for a single note button in the manual triggers
 */
@Composable
private fun NoteButton(
    note: Int,
    onClick: () -> Unit,
) {
    val noteName = getNoteName(note)
    val octave = note / 12 - 1

    Box(
        modifier =
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(onQuinaryLight)
                .border(1.dp, quinaryLight, CircleShape)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "$noteName$octave",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
        )
    }
}

/**
 * Composable for selecting the waveform type
 */
@Composable
private fun WaveformSelector(
    selectedWaveform: SoundPlayer.Waveform,
    onWaveformSelected: (SoundPlayer.Waveform) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SoundPlayer.Waveform.entries.forEach { waveform ->
            val isSelected = waveform == selectedWaveform

            Box(
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isSelected) onPrimaryLight else Color.LightGray)
                        .clickable { onWaveformSelected(waveform) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = waveform.name,
                    color = if (isSelected) Color.White else Color.Black,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

/**
 * Run a simulation of multiple users being hit by a wave in sequence
 */
private suspend fun runWaveSimulation(
    event: IWWWEvent,
    userCount: Int,
    durationSeconds: Int,
    waveform: SoundPlayer.Waveform,
) {
    Log.d(SoundChoreographyTestMode.TAG, "Starting simulation with $userCount users over $durationSeconds seconds")

    // Calculate delay between users
    val totalDurationMs = durationSeconds * 1000
    val delayBetweenUsersMs = totalDurationMs / (userCount - 1)

    try {
        // Set the waveform for the sound - directly use the public property
        val manager = event.warming.soundChoreographyManager
        manager.setWaveform(waveform)

        // Simulate each user being hit
        for (i in 0 until userCount) {
            if (!isActive) break

            val noteNumber = event.warming.playCurrentSoundChoreographyTone()
            if (noteNumber != null) {
                SoundChoreographyTestMode.noteWasPlayed(noteNumber)
            }

            // Wait before next user (except for the last one)
            if (i < userCount - 1) {
                delay(delayBetweenUsersMs.milliseconds)
            }
        }
    } catch (ise: IllegalStateException) {
        Log.e(SoundChoreographyTestMode.TAG, "Invalid state during simulation: ${ise.message}", ise)
    } catch (iae: IllegalArgumentException) {
        Log.e(SoundChoreographyTestMode.TAG, "Invalid simulation parameters: ${iae.message}", iae)
    } catch (uoe: UnsupportedOperationException) {
        Log.e(SoundChoreographyTestMode.TAG, "Unsupported operation during simulation: ${uoe.message}", uoe)
    }

    Log.d(SoundChoreographyTestMode.TAG, "Simulation completed")
}

/**
 * Composable for toggling the test mode from anywhere in the app
 */
@Composable
fun SoundChoreographyTestModeToggle(
    modifier: Modifier = Modifier,
    onToggle: (Boolean) -> Unit = {},
) {
    // Hide the activation button in non-debug builds
    if (!BuildConfig.DEBUG) return

    var showDialog by remember { mutableStateOf(false) }
    val isEnabled = remember { mutableStateOf(SoundChoreographyTestMode.isEnabled()) }

    if (showDialog) {
        Dialog(onDismissRequest = { showDialog = false }) {
            Card(
                modifier = Modifier.padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Sound Choreography Test Mode",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Enable test mode to access tools for testing the sound choreography system.",
                        textAlign = TextAlign.Center,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(text = "Enable Test Mode")

                        Switch(
                            checked = isEnabled.value,
                            onCheckedChange = { enabled ->
                                isEnabled.value = enabled
                                SoundChoreographyTestMode.setEnabled(enabled)
                                onToggle(enabled)
                            },
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showDialog = false },
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Text(text = "Close")
                    }
                }
            }
        }
    }

    Box(
        modifier =
            modifier
                .clip(CircleShape)
                .clickable { showDialog = true }
                .background(if (isEnabled.value) onPrimaryLight else Color.Gray)
                .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_music_note),
            contentDescription = "Sound Test Mode",
            tint = Color.White,
        )
    }
}
