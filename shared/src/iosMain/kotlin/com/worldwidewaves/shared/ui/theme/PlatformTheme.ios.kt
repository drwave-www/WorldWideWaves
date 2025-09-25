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

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * iOS implementation using Material Design equivalent styling.
 */
actual fun platformExtraBoldTextStyle(fontSize: Int): TextStyle =
    TextStyle(fontSize = fontSize.sp, fontWeight = FontWeight.ExtraBold)

actual fun platformExtraQuinaryColoredBoldTextStyle(fontSize: Int): TextStyle =
    TextStyle(fontSize = fontSize.sp, fontWeight = FontWeight.Bold)

actual fun platformQuinaryColoredTextStyle(fontSize: Int): TextStyle =
    TextStyle(fontSize = fontSize.sp)

actual fun platformQuinaryColoredBoldTextStyle(fontSize: Int): TextStyle =
    TextStyle(fontSize = fontSize.sp, fontWeight = FontWeight.Bold)

actual fun platformPrimaryColoredBoldTextStyle(fontSize: Int): TextStyle =
    TextStyle(fontSize = fontSize.sp, fontWeight = FontWeight.Bold)

actual fun platformExtraLightTextStyle(fontSize: Int): TextStyle =
    TextStyle(fontSize = fontSize.sp, fontWeight = FontWeight.Light)

actual fun platformCommonTextStyle(): TextStyle =
    TextStyle()

actual val platformQuinaryLight: Color = Color(0xFFE0E0E0)
actual val platformOnPrimaryLight: Color = Color(0xFF1976D2)
actual val platformScrimLight: Color = Color(0xFF424242)
actual val platformQuaternaryColor: Color = Color(0xFF9E9E9E)
actual val platformOnQuinaryLightColor: Color = Color(0xFFBDBDBD)
actual val platformQuinaryLightColor: Color = Color(0xFFE0E0E0)

actual val platformDateMiter: Float = 4.0f
actual val platformDateStroke: Float = 2.0f