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
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.worldwidewaves.shared.WWWGlobals.Dimensions

/**
 * Extended color scheme for perfect Android parity
 */
@Immutable
data class SharedExtendedColorScheme(
    val quaternary: SharedColorFamily,
    val quinary: SharedColorFamily,
)

@Immutable
data class SharedColorFamily(
    val color: Color,
    val onColor: Color,
    val colorContainer: Color,
    val onColorContainer: Color,
)

/**
 * Shared extended theme - IDENTICAL to Android extendedLight
 */
val sharedExtendedLight = SharedExtendedColorScheme(
    quaternary = SharedColorFamily(
        quaternaryLight,
        onQuaternaryLight,
        quaternaryContainerLight,
        onQuaternaryContainerLight,
    ),
    quinary = SharedColorFamily(
        quinaryLight,
        onQuinaryLight,
        quinaryContainerLight,
        onQuinaryContainerLight,
    ),
)

// -- Shared Text Presets - EXACT Android match --------------------------------------------------------

fun sharedDefaultTextStyle(fontSize: Int = Dimensions.FONTSIZE_DEFAULT): TextStyle =
    TextStyle(fontSize = fontSize.sp)

fun sharedCommonTextStyle(fontSize: Int = Dimensions.FONTSIZE_DEFAULT): TextStyle =
    sharedDefaultTextStyle(fontSize).copy(fontFamily = SharedTypography.bodyMedium.fontFamily)

fun sharedCommonJustifiedTextStyle(fontSize: Int = Dimensions.FONTSIZE_DEFAULT): TextStyle =
    sharedCommonTextStyle(fontSize).copy(textAlign = TextAlign.Justify)

fun sharedCommonBoldStyle(fontSize: Int = Dimensions.FONTSIZE_DEFAULT): TextStyle =
    sharedCommonTextStyle(fontSize).copy(fontWeight = FontWeight.Bold)

fun sharedPrimaryColoredTextStyle(fontSize: Int = Dimensions.FONTSIZE_DEFAULT): TextStyle =
    sharedCommonTextStyle(fontSize).copy(color = SharedLightColorScheme.primary)

fun sharedPrimaryColoredBoldTextStyle(fontSize: Int = Dimensions.FONTSIZE_DEFAULT): TextStyle =
    sharedPrimaryColoredTextStyle(fontSize).copy(fontWeight = FontWeight.Bold)

fun sharedQuinaryColoredTextStyle(fontSize: Int = Dimensions.FONTSIZE_DEFAULT): TextStyle =
    sharedCommonTextStyle(fontSize).copy(color = sharedExtendedLight.quinary.color)

fun sharedQuaternaryColoredTextStyle(fontSize: Int = Dimensions.FONTSIZE_DEFAULT): TextStyle =
    sharedCommonTextStyle(fontSize).copy(color = sharedExtendedLight.quaternary.color)

fun sharedQuinaryColoredBoldTextStyle(fontSize: Int = Dimensions.FONTSIZE_DEFAULT): TextStyle =
    sharedQuinaryColoredTextStyle(fontSize).copy(fontWeight = FontWeight.Bold)

fun sharedExtraBoldTextStyle(fontSize: Int = Dimensions.FONTSIZE_DEFAULT): TextStyle =
    sharedCommonTextStyle(fontSize).copy(fontWeight = FontWeight.ExtraBold)

fun sharedExtraLightTextStyle(fontSize: Int = Dimensions.FONTSIZE_DEFAULT): TextStyle =
    sharedCommonTextStyle(fontSize).copy(fontWeight = FontWeight.Light)

fun sharedExtraPrimaryColoredBoldTextStyle(fontSize: Int = Dimensions.FONTSIZE_DEFAULT): TextStyle =
    sharedPrimaryColoredBoldTextStyle(fontSize).copy(fontWeight = FontWeight.ExtraBold)

/**
 * Shared Material Theme with extended colors - IDENTICAL to Android
 */
@Composable
fun SharedWorldWideWavesThemeWithExtended(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SharedLightColorScheme,
        typography = SharedTypography,
        content = content
    )
}