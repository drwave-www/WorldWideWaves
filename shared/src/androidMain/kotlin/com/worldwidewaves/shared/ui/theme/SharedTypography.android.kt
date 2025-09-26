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

// Fallback to historical fonts until Google Fonts provider is properly configured
// Using distinctive system fonts that match the historical style better
actual val SharedBodyFontFamily: FontFamily = FontFamily.Serif // Closer to Montserrat style
actual val SharedDisplayFontFamily: FontFamily = FontFamily.Serif // Matches body for consistency
actual val SharedExtraFontFamily: FontFamily = FontFamily.Monospace // Distinctive for special elements

// TODO: Restore full Google Fonts setup with proper KMP configuration
// The certificates and provider setup need to be configured differently for shared modules