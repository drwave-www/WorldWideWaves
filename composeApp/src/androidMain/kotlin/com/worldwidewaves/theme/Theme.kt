package com.worldwidewaves.theme

/*
 * Copyright 2025 DrWave
 *
 * WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and
 * countries. The project aims to transcend physical and cultural
 * boundaries, fostering unity, community, and shared human experience by leveraging real-time
 * coordination and location-based services.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_DEFAULT_FONTSIZE

@Immutable
data class ExtendedColorScheme(
    val quaternary: ColorFamily,
    val quinary: ColorFamily,
)

private val lightScheme =
    lightColorScheme(
        primary = primaryLight,
        onPrimary = onPrimaryLight,
        primaryContainer = primaryContainerLight,
        onPrimaryContainer = onPrimaryContainerLight,
        secondary = secondaryLight,
        onSecondary = onSecondaryLight,
        secondaryContainer = secondaryContainerLight,
        onSecondaryContainer = onSecondaryContainerLight,
        tertiary = tertiaryLight,
        onTertiary = onTertiaryLight,
        tertiaryContainer = tertiaryContainerLight,
        onTertiaryContainer = onTertiaryContainerLight,
        error = errorLight,
        onError = onErrorLight,
        errorContainer = errorContainerLight,
        onErrorContainer = onErrorContainerLight,
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
    )

val extendedLight =
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

@Immutable
data class ColorFamily(
    val color: Color,
    val onColor: Color,
    val colorContainer: Color,
    val onColorContainer: Color,
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = lightScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content,
    )
}

// -- WWW Text presets --------------------------------------------------------

fun defaultTextStyle(fontSize: Int = DIM_DEFAULT_FONTSIZE): TextStyle = TextStyle(fontSize = fontSize.sp)

fun commonTextStyle(fontSize: Int = DIM_DEFAULT_FONTSIZE): TextStyle =
    defaultTextStyle(fontSize).copy(fontFamily = AppTypography.bodyMedium.fontFamily)

fun commonJustifiedTextStyle(fontSize: Int = DIM_DEFAULT_FONTSIZE): TextStyle =
    commonTextStyle(fontSize).copy(textAlign = TextAlign.Justify)

fun commonBoldStyle(fontSize: Int = DIM_DEFAULT_FONTSIZE): TextStyle = commonTextStyle(fontSize).copy(fontWeight = FontWeight.Bold)

fun primaryColoredTextStyle(fontSize: Int = DIM_DEFAULT_FONTSIZE): TextStyle = commonTextStyle(fontSize).copy(color = lightScheme.primary)

fun primaryColoredBoldTextStyle(fontSize: Int = DIM_DEFAULT_FONTSIZE): TextStyle =
    primaryColoredTextStyle(fontSize).copy(fontWeight = FontWeight.Bold)

fun quinaryColoredTextStyle(fontSize: Int = DIM_DEFAULT_FONTSIZE): TextStyle =
    commonTextStyle(fontSize).copy(color = extendedLight.quinary.color)

fun quaternaryColoredTextStyle(fontSize: Int = DIM_DEFAULT_FONTSIZE): TextStyle =
    commonTextStyle(fontSize).copy(color = extendedLight.quaternary.color)

fun quinaryColoredBoldTextStyle(fontSize: Int = DIM_DEFAULT_FONTSIZE): TextStyle =
    quinaryColoredTextStyle(fontSize).copy(fontWeight = FontWeight.Bold)

fun extraTextStyle(fontSize: Int = DIM_DEFAULT_FONTSIZE): TextStyle = defaultTextStyle(fontSize).copy(fontFamily = extraFontFamily)

fun extraBoldTextStyle(fontSize: Int = DIM_DEFAULT_FONTSIZE): TextStyle = extraTextStyle(fontSize).copy(fontWeight = FontWeight.Bold)

fun extraLightTextStyle(fontSize: Int = DIM_DEFAULT_FONTSIZE): TextStyle = extraTextStyle(fontSize).copy(fontWeight = FontWeight.Light)

fun extraPrimaryColoredTextStyle(fontSize: Int = DIM_DEFAULT_FONTSIZE): TextStyle =
    extraTextStyle(fontSize).copy(color = lightScheme.primary)

fun extraPrimaryColoredBoldTextStyle(fontSize: Int = DIM_DEFAULT_FONTSIZE): TextStyle =
    extraPrimaryColoredTextStyle(fontSize).copy(fontWeight = FontWeight.Bold)

fun extraQuinaryColoredBoldTextStyle(fontSize: Int = DIM_DEFAULT_FONTSIZE): TextStyle =
    extraTextStyle(fontSize).copy(color = extendedLight.quinary.color, fontWeight = FontWeight.Bold)
