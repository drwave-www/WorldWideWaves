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

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import com.worldwidewaves.shared.MokoRes
import com.worldwidewaves.shared.ui.theme.scrimLight
import com.worldwidewaves.shared.ui.theme.sharedCommonTextStyle
import dev.icerock.moko.resources.compose.stringResource

/**
 * Shared alert dialog for when simulation is attempted without downloaded map.
 * Works identically on both Android and iOS platforms.
 */
@Composable
fun AlertMapNotDownloadedOnSimulationLaunch(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(MokoRes.strings.simulation_map_required_title),
                style =
                    sharedCommonTextStyle().copy(
                        color = scrimLight,
                        fontWeight = FontWeight.Bold,
                    ),
            )
        },
        text = {
            Text(
                text = stringResource(MokoRes.strings.simulation_map_required_message),
                style =
                    sharedCommonTextStyle().copy(
                        color = scrimLight,
                    ),
            )
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(MokoRes.strings.ok))
            }
        },
    )
}
