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

// Android font fallbacks - using distinctive system fonts
// TODO: Restore Google Fonts with proper dependency setup for Montserrat
actual val SharedBodyFontFamily: FontFamily = FontFamily.Serif // Distinctive serif font
actual val SharedDisplayFontFamily: FontFamily = FontFamily.Serif
actual val SharedExtraFontFamily: FontFamily = FontFamily.Monospace // Very distinctive for special text