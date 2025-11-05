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
 * Confirmation dialog for map uninstall operation.
 * Shows a warning that the map will be removed and requires user confirmation.
 */
@Composable
fun AlertMapUninstall(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    StyledAlertDialog(
        onDismissRequest = onDismiss,
        title = stringResource(MokoRes.strings.events_uninstall_map_title),
        text = stringResource(MokoRes.strings.events_uninstall_map_confirmation),
        confirmButtonText = stringResource(MokoRes.strings.events_uninstall),
        onConfirm = onConfirm,
        dismissButtonText = stringResource(MokoRes.strings.map_cancel_download),
        onDismiss = onDismiss,
    )
}
