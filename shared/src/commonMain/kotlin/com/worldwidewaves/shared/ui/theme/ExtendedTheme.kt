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
data class ExtendedColorScheme(
    val quaternary: ColorFamily,
    val quinary: ColorFamily,
)

@Immutable
data class ColorFamily(
    val color: Color,
    val onColor: Color,
    val colorContainer: Color,
    val onColorContainer: Color,
)

/**
 * Shared extended theme - IDENTICAL to Android extendedLight
 */
val sharedExtendedLight =
    ExtendedColorScheme(
        quaternary =
            ColorFamily(
                quaternaryLight,
                onQuaternaryLight,
                quaternaryContainerLight,
                onQuaternaryContainerLight,
            ),
        quinary =
            ColorFamily(
                quinaryLight,
                onQuinaryLight,
                quinaryContainerLight,
                onQuinaryContainerLight,
            ),
    )

// -- Shared Text Presets - EXACT Android match --------------------------------------------------------

fun sharedDefaultTextStyle(fontSize: Int = Dimensions.FONTSIZE_DEFAULT): TextStyle = TextStyle(fontSize = fontSize.sp)

@Composable
fun sharedCommonTextStyle(fontSize: Int = Dimensions.FONTSIZE_DEFAULT): TextStyle =
    sharedDefaultTextStyle(fontSize).copy(fontFamily = AppBodyFontFamily())

@Composable
fun sharedCommonJustifiedTextStyle(fontSize: Int = Dimensions.FONTSIZE_DEFAULT): TextStyle =
    sharedCommonTextStyle(fontSize).copy(textAlign = TextAlign.Justify)

@Composable
fun sharedCommonBoldStyle(fontSize: Int = Dimensions.FONTSIZE_DEFAULT): TextStyle =
    sharedCommonTextStyle(fontSize).copy(fontWeight = FontWeight.Bold)

@Composable
fun sharedPrimaryColoredTextStyle(fontSize: Int = Dimensions.FONTSIZE_DEFAULT): TextStyle =
    sharedCommonTextStyle(fontSize).copy(color = AppLightColorScheme.primary)

@Composable
fun sharedPrimaryColoredBoldTextStyle(fontSize: Int = Dimensions.FONTSIZE_DEFAULT): TextStyle =
    sharedPrimaryColoredTextStyle(fontSize).copy(fontWeight = FontWeight.Bold)

@Composable
fun sharedQuinaryColoredTextStyle(fontSize: Int = Dimensions.FONTSIZE_DEFAULT): TextStyle =
    sharedCommonTextStyle(fontSize).copy(color = sharedExtendedLight.quinary.color)

@Composable
fun sharedQuaternaryColoredTextStyle(fontSize: Int = Dimensions.FONTSIZE_DEFAULT): TextStyle =
    sharedCommonTextStyle(fontSize).copy(color = sharedExtendedLight.quaternary.color)

@Composable
fun sharedQuinaryColoredBoldTextStyle(fontSize: Int = Dimensions.FONTSIZE_DEFAULT): TextStyle =
    sharedQuinaryColoredTextStyle(fontSize).copy(fontWeight = FontWeight.Bold)

@Composable
fun sharedExtraBoldTextStyle(fontSize: Int = Dimensions.FONTSIZE_DEFAULT): TextStyle =
    sharedExtraTextStyle(fontSize).copy(fontWeight = FontWeight.ExtraBold)

@Composable
fun sharedExtraTextStyle(fontSize: Int = Dimensions.FONTSIZE_DEFAULT): TextStyle =
    sharedDefaultTextStyle(fontSize).copy(fontFamily = AppExtraFontFamily())

@Composable
fun sharedExtraLightTextStyle(fontSize: Int = Dimensions.FONTSIZE_DEFAULT): TextStyle =
    sharedExtraTextStyle(fontSize).copy(fontWeight = FontWeight.Light)

@Composable
fun sharedExtraPrimaryColoredBoldTextStyle(fontSize: Int = Dimensions.FONTSIZE_DEFAULT): TextStyle =
    sharedPrimaryColoredBoldTextStyle(fontSize).copy(fontWeight = FontWeight.ExtraBold)

/**
 * Shared Material Theme with extended colors - IDENTICAL to Android
 * Includes iOS Dynamic Type support for accessibility.
 */
@Composable
fun SharedWorldWideWavesThemeWithExtended(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppLightColorScheme,
        typography = AppTypography(),
        content = content,
    )
}
