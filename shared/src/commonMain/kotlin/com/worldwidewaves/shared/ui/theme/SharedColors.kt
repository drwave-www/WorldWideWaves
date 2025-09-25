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

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Shared color system - EXACT same colors as Android for perfect UI parity.
 * These colors will be identical on both Android and iOS.
 */

// Primary colors - exact Android match
val primaryLight = Color(0xFF5DB075) // Green
val onPrimaryLight = Color.White
val secondaryLight = Color(0xFFD34E03) // Orange
val onSecondaryLight = Color.White
val tertiaryLight = Color(0xFFAC0000) // Red
val onTertiaryLight = Color.White
val quaternaryLight = Color(0xFFF6F6F6) // Gray
val onQuaternaryLight = Color(0xFFBDBDBD)
val quinaryLight = Color.White // White
val onQuinaryLight = Color(0xFF5DB075)

// Background colors - exact Android match
val backgroundLight = Color(0xFF011026)
val onBackgroundLight = Color.White
val surfaceLight = Color(0xFF011026)
val onSurfaceLight = Color.White

// Container colors - exact Android match
val primaryContainerLight = Color.White
val onPrimaryContainerLight = Color(0xFF575859)
val secondaryContainerLight = Color(0xFF69BD80)
val onSecondaryContainerLight = Color(0xFF002710)
val tertiaryContainerLight = Color(0xFFC21A0F)
val onTertiaryContainerLight = Color.White

// Error colors - exact Android match
val errorLight = Color(0xFFBA1A1A)
val onErrorLight = Color.White
val errorContainerLight = Color(0xFFFFDAD6)
val onErrorContainerLight = Color(0xFF410002)

// Surface variant colors - exact Android match
val surfaceVariantLight = Color(0xFFE1E2EA)
val onSurfaceVariantLight = Color(0xFF44474D)
val outlineLight = Color(0xFF75777E)
val outlineVariantLight = Color(0xFFC5C6CD)
val scrimLight = Color.Black
val inverseSurfaceLight = Color(0xFF303032)
val inverseOnSurfaceLight = Color(0xFFF2F0F2)
val inversePrimaryLight = Color(0xFFC6C6C7)
val surfaceDimLight = Color(0xFFDBD9DC)
val surfaceBrightLight = Color(0xFFFBF9FB)
val surfaceContainerLowestLight = Color.White
val surfaceContainerLowLight = Color(0xFFF5F3F5)
val surfaceContainerLight = Color(0xFFEFEDEF)
val surfaceContainerHighLight = Color(0xFFEAE7EA)
val surfaceContainerHighestLight = Color(0xFFE4E2E4)

// Extended container colors - exact Android match
val quaternaryContainerLight = Color(0xFFFFDAD4)
val onQuaternaryContainerLight = Color(0xFF3A0905)
val quinaryContainerLight = Color(0xFFFFDAD4)
val onQuinaryContainerLight = Color(0xFF3A0905)

/**
 * Shared ColorScheme - identical on both Android and iOS
 */
val SharedLightColorScheme = lightColorScheme(
    primary = primaryLight,
    onPrimary = onPrimaryLight,
    secondary = secondaryLight,
    onSecondary = onSecondaryLight,
    tertiary = tertiaryLight,
    onTertiary = onTertiaryLight,
    background = backgroundLight,
    onBackground = onBackgroundLight,
    surface = surfaceLight,
    onSurface = onSurfaceLight,
    surfaceVariant = surfaceVariantLight,
    onSurfaceVariant = onSurfaceVariantLight,
    outline = outlineLight,
    outlineVariant = outlineVariantLight,
    scrim = scrimLight,
    inverseSurface = inverseSurfaceLight,
    inverseOnSurface = inverseOnSurfaceLight,
    inversePrimary = inversePrimaryLight,
    surfaceDim = surfaceDimLight,
    surfaceBright = surfaceBrightLight,
    surfaceContainerLowest = surfaceContainerLowestLight,
    surfaceContainerLow = surfaceContainerLowLight,
    surfaceContainer = surfaceContainerLight,
    surfaceContainerHigh = surfaceContainerHighLight,
    surfaceContainerHighest = surfaceContainerHighestLight,
    primaryContainer = primaryContainerLight,
    onPrimaryContainer = onPrimaryContainerLight,
    secondaryContainer = secondaryContainerLight,
    onSecondaryContainer = onSecondaryContainerLight,
    tertiaryContainer = tertiaryContainerLight,
    onTertiaryContainer = onTertiaryContainerLight,
    error = errorLight,
    onError = onErrorLight,
    errorContainer = errorContainerLight,
    onErrorContainer = onErrorContainerLight
)