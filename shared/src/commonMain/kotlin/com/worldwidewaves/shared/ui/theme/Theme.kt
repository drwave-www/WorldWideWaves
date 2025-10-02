package com.worldwidewaves.shared.ui.theme

/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * Shared theme system - IDENTICAL on both Android and iOS.
 *
 * This ensures perfect UI parity by using the exact same colors,
 * typography, and styling on both platforms.
 */
@Composable
fun WorldWideWavesTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppLightColorScheme,
        typography = AppTypography,
        content = content,
    )
}
