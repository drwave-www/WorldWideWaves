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

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily

/**
 * iOS font families using Google Fonts (bundled as TTF files in iOS app bundle).
 * These fonts match exactly with Android implementation for consistent branding.
 *
 * **MANUAL SETUP REQUIRED**: See iOS_FONT_SETUP_INSTRUCTIONS.md
 *
 * Fonts must be added to Xcode project with target membership for proper bundling.
 * Once added, UIAppFonts in Info.plist will register them with the system, making
 * them available as system fonts by their PostScript names.
 *
 * **Font Files** (in iosApp/worldwidewaves/Fonts/):
 * - Montserrat-Regular.ttf, Montserrat-Medium.ttf, Montserrat-Bold.ttf
 * - MontserratAlternates-Regular.ttf, MontserratAlternates-Medium.ttf, MontserratAlternates-Bold.ttf
 * - NotoSans-Regular.ttf, NotoSans-Medium.ttf, NotoSans-Bold.ttf
 *
 * **Current Status**: Using system font (FontFamily.Default) as fallback.
 * After Xcode setup, fonts will be registered via UIAppFonts and can be referenced by PostScript name.
 */

// NOTE: Requires Xcode setup - fonts must be added to Xcode project with target membership.
// After manual setup, implement font loading using platform.UIKit.UIFont APIs.
// See iOS_FONT_SETUP_INSTRUCTIONS.md for complete setup steps.
@Composable
actual fun AppBodyFontFamily(): FontFamily = FontFamily.Default

@Composable
actual fun AppDisplayFontFamily(): FontFamily = FontFamily.Default

@Composable
actual fun AppExtraFontFamily(): FontFamily = FontFamily.Default
