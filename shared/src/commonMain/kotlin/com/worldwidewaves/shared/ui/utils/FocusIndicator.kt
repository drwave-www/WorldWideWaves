package com.worldwidewaves.shared.ui.utils

/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Adds visible focus indicator for keyboard/D-pad navigation.
 *
 * Provides WCAG 2.4.7 - Focus Visible compliance by displaying a
 * clear visual border around focused interactive elements.
 *
 * @param interactionSource The interaction source for tracking focus state
 * @param focusColor The color of the focus indicator border (default: primary theme color)
 * @param focusWidth The width of the focus indicator border (default: 2.dp)
 * @return A modifier that applies focus indicator styling
 */
@Composable
fun Modifier.focusIndicator(
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    focusColor: Color = MaterialTheme.colorScheme.primary,
    focusWidth: Dp = 2.dp,
): Modifier {
    val isFocused by interactionSource.collectIsFocusedAsState()

    return this
        .focusable(interactionSource = interactionSource)
        .border(
            width = if (isFocused) focusWidth else 0.dp,
            color = if (isFocused) focusColor else Color.Transparent,
        )
}
