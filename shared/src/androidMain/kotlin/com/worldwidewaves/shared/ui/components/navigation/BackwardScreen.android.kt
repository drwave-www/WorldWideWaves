package com.worldwidewaves.shared.ui.components.navigation

/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Android implementation of back arrow icon.
 * Uses Material Design Icons ArrowBack.
 */
@Composable
actual fun getBackArrowIcon(): ImageVector {
    return Icons.AutoMirrored.Filled.ArrowBack
}