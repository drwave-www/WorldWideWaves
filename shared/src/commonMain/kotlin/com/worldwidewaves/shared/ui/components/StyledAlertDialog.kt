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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.worldwidewaves.shared.ui.theme.primaryLight
import com.worldwidewaves.shared.ui.theme.sharedCommonTextStyle
import com.worldwidewaves.shared.ui.theme.surfaceLight

/**
 * Styled alert dialog with modern glassmorphism design.
 *
 * Features:
 * - Semi-transparent dark blue background with blur effect
 * - White 2px border with rounded corners (14dp)
 * - White bold title and normal text
 * - Outlined green buttons
 * - Minimal, clean design
 *
 * Works identically on both Android and iOS platforms.
 */
@Composable
fun StyledAlertDialog(
    onDismissRequest: () -> Unit,
    title: String,
    text: String,
    confirmButtonText: String,
    onConfirm: () -> Unit,
    dismissButtonText: String? = null,
    onDismiss: (() -> Unit)? = null,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = title,
                style =
                    sharedCommonTextStyle().copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    ),
            )
        },
        text = {
            Text(
                text = text,
                style =
                    sharedCommonTextStyle().copy(
                        color = Color.White,
                    ),
            )
        },
        confirmButton = {
            OutlinedButton(
                onClick = onConfirm,
                border = BorderStroke(1.dp, primaryLight),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(
                    text = confirmButtonText,
                    color = primaryLight,
                )
            }
        },
        dismissButton =
            if (dismissButtonText != null && onDismiss != null) {
                {
                    OutlinedButton(
                        onClick = onDismiss,
                        border = BorderStroke(1.dp, primaryLight),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            text = dismissButtonText,
                            color = primaryLight,
                        )
                    }
                }
            } else {
                null
            },
        shape = RoundedCornerShape(14.dp),
        containerColor = surfaceLight.copy(alpha = 0.9f),
        modifier =
            Modifier
                .border(2.dp, Color.White, RoundedCornerShape(14.dp))
                .blur(12.dp)
                .padding(4.dp),
    )
}
