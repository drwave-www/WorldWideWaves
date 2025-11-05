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
 * Shared alert dialog for when simulation is attempted without downloaded map.
 * Works identically on both Android and iOS platforms.
 */
@Composable
fun AlertMapNotDownloadedOnSimulationLaunch(onDismiss: () -> Unit) {
    StyledAlertDialog(
        onDismissRequest = onDismiss,
        title = stringResource(MokoRes.strings.simulation_map_required_title),
        text = stringResource(MokoRes.strings.simulation_map_required_message),
        confirmButtonText = stringResource(MokoRes.strings.ok),
        onConfirm = onDismiss,
    )
}
