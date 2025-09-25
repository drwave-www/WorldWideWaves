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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Shared typography system - IDENTICAL on both Android and iOS.
 *
 * This ensures perfect font matching across platforms using the same
 * typography specification as Android.
 */

// System font families for cross-platform compatibility
val SharedBodyFontFamily = FontFamily.Default // System font - works on both platforms
val SharedDisplayFontFamily = FontFamily.Default

/**
 * Shared Typography - exact same text styles on both platforms
 */
val SharedTypography = Typography(
    // Event-specific text styles matching Android exactly
    headlineLarge = TextStyle(
        fontFamily = SharedDisplayFontFamily,
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 40.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = SharedDisplayFontFamily,
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 36.sp
    ),
    titleLarge = TextStyle(
        fontFamily = SharedBodyFontFamily,
        fontSize = 26.sp, // EVENT_LOCATION_FONTSIZE = 26
        fontWeight = FontWeight.Medium,
        lineHeight = 32.sp
    ),
    titleMedium = TextStyle(
        fontFamily = SharedBodyFontFamily,
        fontSize = 30.sp, // EVENT_DATE_FONTSIZE = 30
        fontWeight = FontWeight.Bold,
        lineHeight = 36.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = SharedBodyFontFamily,
        fontSize = 18.sp, // EVENT_COUNTRY_FONTSIZE = 18 (MEDIUM)
        fontWeight = FontWeight.Normal,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = SharedBodyFontFamily,
        fontSize = 16.sp, // EVENT_COMMUNITY_FONTSIZE = 16 (DEFAULT)
        fontWeight = FontWeight.Normal,
        lineHeight = 20.sp
    ),
    labelSmall = TextStyle(
        fontFamily = SharedBodyFontFamily,
        fontSize = 12.sp, // SOONRUNNING_FONTSIZE for badges
        fontWeight = FontWeight.Bold,
        lineHeight = 16.sp
    )
)