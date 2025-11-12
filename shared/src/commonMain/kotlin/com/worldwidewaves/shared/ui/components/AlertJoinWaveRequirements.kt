package com.worldwidewaves.shared.ui.components

/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

import androidx.compose.runtime.Composable
import com.worldwidewaves.shared.MokoRes
import dev.icerock.moko.resources.compose.stringResource

/**
 * Alert dialog shown when user clicks the inactive "Join the wave" button.
 * Explains the requirements to join a wave:
 * - Must be on the date of the event
 * - Must be located within the event's wave area
 * - Can check FAQ for simulation options
 *
 * Works identically on both Android and iOS platforms.
 *
 * @param onDismiss Callback when dialog is dismissed
 */
@Composable
fun AlertJoinWaveRequirements(onDismiss: () -> Unit) {
    StyledAlertDialog(
        onDismissRequest = onDismiss,
        title = stringResource(MokoRes.strings.join_wave_requirements_title),
        text = stringResource(MokoRes.strings.join_wave_requirements_message),
        confirmButtonText = stringResource(MokoRes.strings.ok),
        onConfirm = onDismiss,
    )
}
