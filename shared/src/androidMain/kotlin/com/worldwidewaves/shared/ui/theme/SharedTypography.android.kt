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

// EXACT historical Google Fonts setup restored
private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = com.worldwidewaves.shared.R.array.com_google_android_gms_fonts_certs,
)

// EXACT historical font families with Montserrat + Noto Sans
actual val SharedBodyFontFamily: FontFamily = FontFamily(
    Font(GoogleFont("Montserrat"), provider),
    Font(GoogleFont("Noto Sans"), provider),
)

actual val SharedDisplayFontFamily: FontFamily = FontFamily(
    Font(GoogleFont("Montserrat"), provider),
    Font(GoogleFont("Noto Sans"), provider),
)

actual val SharedExtraFontFamily: FontFamily = FontFamily(
    Font(GoogleFont("Montserrat Alternates"), provider),
    Font(GoogleFont("Noto Sans"), provider),
)