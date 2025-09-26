package com.worldwidewaves.theme

/*
 * Copyright 2025 DrWave
 */

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.worldwidewaves.R

// EXACT historical Google Fonts setup
val provider =
    GoogleFont.Provider(
        providerAuthority = "com.google.android.gms.fonts",
        providerPackage = "com.google.android.gms",
        certificates = R.array.com_google_android_gms_fonts_certs,
    )

val bodyFontFamily =
    FontFamily(
        Font(GoogleFont("Montserrat"), provider),
        Font(GoogleFont("Noto Sans"), provider),
    )

val displayFontFamily =
    FontFamily(
        Font(GoogleFont("Montserrat"), provider),
        Font(GoogleFont("Noto Sans"), provider),
    )

val extraFontFamily =
    FontFamily(
        Font(GoogleFont("Montserrat Alternates"), provider),
        Font(GoogleFont("Noto Sans"), provider),
    )

val AppTypography =
    Typography(
        bodyLarge = Typography().bodyLarge.copy(fontFamily = bodyFontFamily),
        bodyMedium = Typography().bodyMedium.copy(fontFamily = bodyFontFamily),
        bodySmall = Typography().bodySmall.copy(fontFamily = bodyFontFamily),
        headlineLarge = Typography().headlineLarge.copy(fontFamily = displayFontFamily),
        headlineMedium = Typography().headlineMedium.copy(fontFamily = displayFontFamily),
        titleLarge = Typography().titleLarge.copy(fontFamily = displayFontFamily),
        titleMedium = Typography().titleMedium.copy(fontFamily = displayFontFamily),
    )