package com.worldwidewaves.shared.ui.components

/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

import android.text.BidiFormatter

/**
 * Android implementation using BidiFormatter for proper internationalization.
 */
actual fun PlatformBidiWrap(text: String): String {
    return BidiFormatter.getInstance().unicodeWrap(text)
}
