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

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Shared typography system supporting iOS Dynamic Type accessibility.
 *
 * On iOS: Scales text based on UIContentSizeCategory (12 levels from 0.8x to 3.0x)
 * On Android: Uses native sp scaling (automatic via system font size settings)
 *
 * This ensures perfect font matching across platforms with proper accessibility support.
 */

// Font families - expect/actual for platform-specific font loading
// These are now @Composable functions to support Compose Multiplatform Font() API on iOS
@Composable
expect fun AppBodyFontFamily(): FontFamily

@Composable
expect fun AppDisplayFontFamily(): FontFamily

@Composable
expect fun AppExtraFontFamily(): FontFamily

/**
 * Creates typography with dynamic scaling for accessibility.
 *
 * Base sizes match Android specification exactly, then scaled by platform:
 * - iOS: Multiplies by Dynamic Type scale factor (0.8x - 3.0x)
 * - Android: Returns base sizes (sp units handle scaling automatically)
 */
@Composable
fun AppTypography(): Typography {
    val scale = rememberDynamicTypeScale()
    val bodyFont = AppBodyFontFamily()
    val displayFont = AppDisplayFontFamily()

    return Typography(
        // Event-specific text styles matching Android exactly
        headlineLarge =
            TextStyle(
                fontFamily = displayFont,
                fontSize = (32 * scale).sp,
                fontWeight = FontWeight.Bold,
                lineHeight = (40 * scale).sp,
            ),
        headlineMedium =
            TextStyle(
                fontFamily = displayFont,
                fontSize = (28 * scale).sp,
                fontWeight = FontWeight.Bold,
                lineHeight = (36 * scale).sp,
            ),
        titleLarge =
            TextStyle(
                fontFamily = bodyFont,
                fontSize = (26 * scale).sp, // EVENT_LOCATION_FONTSIZE = 26
                fontWeight = FontWeight.Medium,
                lineHeight = (32 * scale).sp,
            ),
        titleMedium =
            TextStyle(
                fontFamily = bodyFont,
                fontSize = (30 * scale).sp, // EVENT_DATE_FONTSIZE = 30
                fontWeight = FontWeight.Bold,
                lineHeight = (36 * scale).sp,
            ),
        bodyLarge =
            TextStyle(
                fontFamily = bodyFont,
                fontSize = (18 * scale).sp, // EVENT_COUNTRY_FONTSIZE = 18 (MEDIUM)
                fontWeight = FontWeight.Normal,
                lineHeight = (24 * scale).sp,
            ),
        bodyMedium =
            TextStyle(
                fontFamily = bodyFont,
                fontSize = (16 * scale).sp, // EVENT_COMMUNITY_FONTSIZE = 16 (DEFAULT)
                fontWeight = FontWeight.Normal,
                lineHeight = (20 * scale).sp,
            ),
        labelSmall =
            TextStyle(
                fontFamily = bodyFont,
                fontSize = (12 * scale).sp, // SOONRUNNING_FONTSIZE for badges
                fontWeight = FontWeight.Bold,
                lineHeight = (16 * scale).sp,
            ),
    )
}
