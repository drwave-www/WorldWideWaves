
package com.worldwidewaves.compose.choreographies

/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

// Re-export shared choreography components for backward compatibility
import com.worldwidewaves.shared.ui.components.choreographies.WaveChoreographies
import com.worldwidewaves.shared.ui.components.choreographies.TimedSequenceDisplay
import com.worldwidewaves.shared.ui.components.choreographies.ChoreographyDisplay

// Constants for choreography display
private object ChoreographyConstants {
    // UI Dimensions
    const val CHOREOGRAPHY_PADDING = 24f
    const val CHOREOGRAPHY_TEXT_SIZE = 24f
}

/**
 * High-level choreography container displayed on the **Wave** screen.
 *
 * Behaviour:
 * • Subscribes to various flags exposed by [IWWWEvent.observer] to know whether
 *   the user is currently warming-up, about to be hit, or has just been hit.
 * • According to those flags it queries the proper `ChoreographyManager`
 *   (warming / waiting / hit) to obtain a [DisplayableSequence].
 * • The resulting animation is shown while leaving room at the
 *   bottom for the count-down timer that the parent screen overlays.
 */
@Composable
fun WaveChoreographies(
    event: IWWWEvent,
    clock: IClock,
    modifier: Modifier = Modifier,
) {
    val isWarmingInProgress by event.observer.isUserWarmingInProgress.collectAsState()
    val isGoingToBeHit by event.observer.userIsGoingToBeHit.collectAsState()
    val hasBeenHit by event.observer.userHasBeenHit.collectAsState()
    val hitDateTime by event.observer.hitDateTime.collectAsState()

    // Debug logging for choreography states
    androidx.compose.runtime.LaunchedEffect(isWarmingInProgress, isGoingToBeHit, hasBeenHit) {
        android.util.Log.v(
            "WaveChoreographies",
            "[CHOREO_DEBUG] State change for ${event.id}: warming=$isWarmingInProgress, " +
                "goingToBeHit=$isGoingToBeHit, hasBeenHit=$hasBeenHit",
        )
    }

    // State to track if we should show the hit sequence
    var showHitSequence by remember { mutableStateOf(false) }

    // For warming sequences - use recomposition to update sequence
    var warmingKey by remember { mutableIntStateOf(0) }

    // Calculate and schedule the hiding of the hit sequence
    LaunchedEffect(hasBeenHit, hitDateTime) {
        if (hasBeenHit) {
            val currentTime = clock.now()
            val secondsSinceHit = (currentTime - hitDateTime).inWholeSeconds

            if (secondsSinceHit in 0..WaveTiming.SHOW_HIT_SEQUENCE_SECONDS.inWholeSeconds) {
                showHitSequence = true

                // Calculate remaining time to show
                val remainingTimeMs =
                    maxOf(
                        0,
                        WaveTiming.SHOW_HIT_SEQUENCE_SECONDS.inWholeMilliseconds -
                            (currentTime - hitDateTime).inWholeMilliseconds,
                    )

                // Schedule hiding after the remaining time
                delay(remainingTimeMs)
                showHitSequence = false
            } else {
                showHitSequence = false
            }
        } else {
            showHitSequence = false
        }
    }

    // Only show choreography content in the center, leaving bottom space free
    when {
        // Show warming choreography with sequence refresh
        isWarmingInProgress -> {
            // Get the current sequence using suspend function
            var warmingSequence by remember { mutableStateOf<DisplayableSequence<DrawableResource>?>(null) }

            androidx.compose.runtime.LaunchedEffect(warmingKey) {
                val sequence = event.warming.getCurrentChoregraphySequence()
                android.util.Log.v("WaveChoreographies", "[CHOREO_DEBUG] Warming sequence for ${event.id}: $sequence")
                warmingSequence = sequence
            }

            // When this sequence ends, request a new one
            if (warmingSequence != null) {
                android.util.Log.v("WaveChoreographies", "[CHOREO_DEBUG] Showing warming sequence for ${event.id}")
                TimedSequenceDisplay(
                    sequence = warmingSequence,
                    clock = clock,
                    modifier =
                        modifier
                            .fillMaxWidth()
                            .padding(bottom = 120.dp),
                    // Leave space for counter
                    onSequenceComplete = { warmingKey++ },
                )
            } else {
                android.util.Log.v("WaveChoreographies", "[CHOREO_DEBUG] Warming sequence is NULL for ${event.id}")
            }
        }

        // Show waiting choreography when going to be hit
        isGoingToBeHit -> {
            ChoreographyDisplay(
                event.wave.waitingChoregraphySequence(),
                clock,
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 120.dp), // Leave space for counter
            )
        }

        // Show hit choreography when user has been hit and within time window
        showHitSequence -> {
            ChoreographyDisplay(
                event.wave.hitChoregraphySequence(),
                clock,
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 120.dp), // Leave space for counter
            )
        }
    }
}

/**
 * Helper that renders [sequence] for its remaining duration (or full
 * [DisplayableSequence.duration] when `remainingDuration == null`) and then
 * triggers [onSequenceComplete] so the caller can request the next sequence.
 */
@Composable
fun TimedSequenceDisplay(
    sequence: DisplayableSequence<DrawableResource>?,
    clock: IClock,
    modifier: Modifier = Modifier,
    onSequenceComplete: () -> Unit,
) {
    if (sequence == null) {
        android.util.Log.v("TimedSequenceDisplay", "[CHOREO_DEBUG] Sequence is NULL")
        return
    }

    android.util.Log.v(
        "TimedSequenceDisplay",
        "[CHOREO_DEBUG] Displaying sequence: duration=${sequence.duration}, frameCount=${sequence.frameCount}",
    )
    ChoreographyDisplay(sequence, clock, modifier)

    LaunchedEffect(sequence) {
        delay(sequence.remainingDuration ?: sequence.duration)
        onSequenceComplete()
    }
}

/**
 * Low-level renderer that plays a sprite-sheet based animation.
 *
 * Frames are laid out horizontally in a single bitmap; we translate the canvas
 * by `frameWidth × currentIndex` inside a clipped rect to reveal each frame.
 * The animation loops according to [DisplayableSequence.loop] and is wrapped in
 * a semi-transparent card with the sequence’s localized caption underneath.
 */
@Composable
fun ChoreographyDisplay(
    sequence: DisplayableSequence<DrawableResource>?,
    clock: IClock,
    modifier: Modifier = Modifier,
) {
    if (sequence == null || sequence.image == null) return

    var currentImageIndex by remember { mutableIntStateOf(0) }
    val remainingTime by remember(sequence) { mutableStateOf(sequence.remainingDuration) }

    // Get the painter and convert to ImageBitmap
    val painter = painterResource(sequence.image!!)

    // Create a timer to cycle through images
    LaunchedEffect(sequence) {
        val startTime = clock.now()

        while (this.isActive) {
            // Check if we should stop showing the sequence
            val elapsed = clock.now() - startTime
            if (remainingTime != null) {
                if (elapsed >= remainingTime!!) break
            } else if (elapsed >= sequence.duration) {
                break
            }

            delay(sequence.timing.inWholeMilliseconds)

            // Only advance frame if we haven't reached the last frame or if looping
            if (sequence.loop) {
                currentImageIndex = (currentImageIndex + 1) % sequence.frameCount
            } else if (currentImageIndex < sequence.frameCount - 1) {
                currentImageIndex++
            }
            // If not looping and at last frame, keep showing it until duration ends
        }
    }

    // Position the choreography in the center without taking full screen space
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .widthIn(max = 400.dp)
                    .heightIn(max = 600.dp)
                    .padding(ChoreographyConstants.CHOREOGRAPHY_PADDING.dp)
                    .shadow(8.dp)
                    .background(Color.Black.copy(alpha = 0.7f))
                    .border(2.dp, Color.White, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .padding(ChoreographyConstants.CHOREOGRAPHY_PADDING.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Canvas(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f), // Take available space but leave room for text
                ) {
                    val frameWidthPx = sequence.frameWidth.toFloat()
                    val frameHeightPx = sequence.frameHeight.toFloat()

                    // Calculate scale to fit the frame within canvas bounds
                    val scaleX = size.width / frameWidthPx
                    val scaleY = size.height / frameHeightPx
                    val scale = minOf(scaleX, scaleY)

                    // Calculate centered position
                    val scaledWidth = frameWidthPx * scale
                    val scaledHeight = frameHeightPx * scale
                    val offsetX = (size.width - scaledWidth) / 2f
                    val offsetY = (size.height - scaledHeight) / 2f

                    clipRect(
                        left = offsetX,
                        top = offsetY,
                        right = offsetX + scaledWidth,
                        bottom = offsetY + scaledHeight,
                    ) {
                        translate(
                            left = offsetX - (currentImageIndex * scaledWidth),
                            top = offsetY,
                        ) {
                            with(painter) {
                                draw(
                                    size =
                                        Size(
                                            width = scaledWidth * 4, // Always 4 frames per slide
                                            height = scaledHeight,
                                        ),
                                )
                            }
                        }
                    }
                }

                Text(
                    text = stringResource(sequence.text),
                    style = quinaryColoredBoldTextStyle(ChoreographyConstants.CHOREOGRAPHY_TEXT_SIZE.toInt()),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
