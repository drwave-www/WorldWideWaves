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

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.worldwidewaves.shared.R

private val provider =
    GoogleFont.Provider(
        providerAuthority = "com.google.android.gms.fonts",
        providerPackage = "com.google.android.gms",
        certificates = R.array.com_google_android_gms_fonts_certs,
    )

actual val AppBodyFontFamily: FontFamily =
    FontFamily(
        Font(GoogleFont("Montserrat"), provider),
        Font(GoogleFont("Noto Sans"), provider),
        Font(GoogleFont("Noto Sans Arabic"), provider),
        Font(GoogleFont("Noto Naskh Arabic"), provider),
        Font(GoogleFont("Noto Sans Hebrew"), provider),
        Font(GoogleFont("Noto Sans Bengali"), provider),
        Font(GoogleFont("Noto Sans Devanagari"), provider),
        Font(GoogleFont("Noto Sans Thai"), provider),
        Font(GoogleFont("Noto Sans Ethiopic"), provider),
        Font(GoogleFont("Noto Sans SC"), provider),
        Font(GoogleFont("Noto Sans JP"), provider),
        Font(GoogleFont("Noto Sans KR"), provider),
        Font(GoogleFont("Noto Nastaliq Urdu"), provider),
    )

actual val AppDisplayFontFamily: FontFamily =
    FontFamily(
        Font(GoogleFont("Montserrat"), provider),
        Font(GoogleFont("Noto Sans"), provider),
        Font(GoogleFont("Noto Sans Arabic"), provider),
        Font(GoogleFont("Noto Naskh Arabic"), provider),
        Font(GoogleFont("Noto Sans Hebrew"), provider),
        Font(GoogleFont("Noto Sans Bengali"), provider),
        Font(GoogleFont("Noto Sans Devanagari"), provider),
        Font(GoogleFont("Noto Sans Thai"), provider),
        Font(GoogleFont("Noto Sans Ethiopic"), provider),
        Font(GoogleFont("Noto Sans SC"), provider),
        Font(GoogleFont("Noto Sans JP"), provider),
        Font(GoogleFont("Noto Sans KR"), provider),
        Font(GoogleFont("Noto Nastaliq Urdu"), provider),
    )

actual val AppExtraFontFamily: FontFamily =
    FontFamily(
        Font(GoogleFont("Montserrat Alternates"), provider),
        Font(GoogleFont("Noto Sans"), provider),
        Font(GoogleFont("Noto Sans Arabic"), provider),
        Font(GoogleFont("Noto Naskh Arabic"), provider),
        Font(GoogleFont("Noto Sans Hebrew"), provider),
        Font(GoogleFont("Noto Sans Bengali"), provider),
        Font(GoogleFont("Noto Sans Devanagari"), provider),
        Font(GoogleFont("Noto Sans Thai"), provider),
        Font(GoogleFont("Noto Sans Ethiopic"), provider),
        Font(GoogleFont("Noto Sans SC"), provider),
        Font(GoogleFont("Noto Sans JP"), provider),
        Font(GoogleFont("Noto Sans KR"), provider),
        Font(GoogleFont("Noto Nastaliq Urdu"), provider),
    )
