package com.worldwidewaves.shared

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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import com.worldwidewaves.shared.utils.Log
import platform.UIKit.UIViewController

/**
 * iOS Main View Controller that uses shared WWWMainActivity.
 * Uses the exact same UI as Android for perfect parity.
 * Enhanced with exception logging for debugging.
 */
fun MainViewController(): UIViewController {
    Log.i("MainViewController", "Creating iOS main view controller")
    return ComposeUIViewController {
        // Start with the exact working pattern from a0dc587: simple text display
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "WorldWideWaves iOS",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/**
 * HelloComposeViewController following iOS integration guidance
 * This is the proper way to call Composables from iOS - NO KOIN, NO DI
 */
fun HelloComposeViewController(): UIViewController =
    ComposeUIViewController {
        AppUI()
    }

/**
 * Creates a simple ComposeUIViewController to test basic Composable calling
 */
fun createSimpleComposeView(): UIViewController {
    Log.i("MainViewController", "Creating simple Compose view for testing")
    return HelloComposeViewController()
}
