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

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.worldwidewaves.R

val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val bodyFontFamily = FontFamily(
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

val displayFontFamily = FontFamily(
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

val extraFontFamily = FontFamily(
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

val AppTypography = Typography(
    displayLarge = Typography().displayLarge.copy(fontFamily = displayFontFamily),
    displayMedium = Typography().displayMedium.copy(fontFamily = displayFontFamily),
    displaySmall = Typography().displaySmall.copy(fontFamily = displayFontFamily),
    headlineLarge = Typography().headlineLarge.copy(fontFamily = displayFontFamily),
    headlineMedium = Typography().headlineMedium.copy(fontFamily = displayFontFamily),
    headlineSmall = Typography().headlineSmall.copy(fontFamily = displayFontFamily),
    titleLarge = Typography().titleLarge.copy(fontFamily = displayFontFamily),
    titleMedium = Typography().titleMedium.copy(fontFamily = displayFontFamily),
    titleSmall = Typography().titleSmall.copy(fontFamily = displayFontFamily),
    bodyLarge = Typography().bodyLarge.copy(fontFamily = bodyFontFamily),
    bodyMedium = Typography().bodyMedium.copy(fontFamily = bodyFontFamily),
    bodySmall = Typography().bodySmall.copy(fontFamily = bodyFontFamily),
    labelLarge = Typography().labelLarge.copy(fontFamily = bodyFontFamily),
    labelMedium = Typography().labelMedium.copy(fontFamily = bodyFontFamily),
    labelSmall = Typography().labelSmall.copy(fontFamily = bodyFontFamily),
)
