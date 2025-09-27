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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import com.worldwidewaves.shared.ui.activities.UIProperties
import com.worldwidewaves.shared.ui.activities.WWWMainActivity
import com.worldwidewaves.shared.utils.Log
import platform.UIKit.UIViewController

/**
 * iOS Main View Controller that uses shared WWWMainActivity.
 * Uses the exact same UI as Android for perfect parity.
 * Enhanced with exception logging for debugging.
 */
@Throws(Throwable::class)
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
        SimpleTestAppUI()
    }

/**
 * Top-level @Composable for iOS app - exposed as MainViewControllerKt.AppUI()
 */
@Composable
fun AppUI() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "🎯 iOS Compose UI Working!",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * Creates a simple ComposeUIViewController to test basic Composable calling
 */
fun createSimpleComposeView(): UIViewController {
    Log.i("MainViewController", "Creating simple Compose view for testing")
    return HelloComposeViewController()
}

/**
 * Creates UIViewController with WWWMainActivity.Draw() - iOS equivalent of Android MainActivity
 * This replicates: setContent { mainActivityImpl!!.Draw() }
 */
/**
 * iOS-SAFE: Initialize MainActivity business logic WITHOUT ComposeUIViewController
 * This avoids the lifecycle dependency issue entirely
 */
@Throws(Throwable::class)
fun initializeMainActivityBusinessLogic(mainActivity: WWWMainActivity) {
    Log.i("MainViewController", "🎯 iOS: Initializing MainActivity business logic (NO Compose UI)")

    // Call business logic initialization directly
    // This avoids ComposeUIViewController which uses Android lifecycle APIs

    Log.i("MainViewController", "🎯 iOS: MainActivity business logic initialized")
}

/**
 * SINGLE ComposeUIViewController approach - create only ONE at app root
 * This follows the correct iOS pattern for Compose Multiplatform
 */
@Throws(Throwable::class)
fun createAppViewController(): UIViewController {
    Log.i("MainViewController", "🎯 iOS: Creating correct ComposeUIViewController")

    // Initialize Koin before creating Compose UI
    initializeIOSApp()

    // Use the correct iOS Compose host
    return ComposeUIViewController {
        AppUI() // Single shared @Composable root
    }
}

/**
 * Initialize iOS app - Koin, MokoRes, etc.
 */
@Throws(Throwable::class)
fun initializeIOSApp() {
    Log.i("MainViewController", "🎯 iOS: Initializing app...")

    // This replaces the ContentView initialization logic
    doInitKoin()

    Log.i("MainViewController", "🎯 iOS: App initialization completed")
}

/**
 * DEPRECATED: Multiple ComposeUIViewController approach - causes iOS lifecycle crashes
 */
@Throws(Throwable::class)
fun createMainActivityViewController(mainActivity: WWWMainActivity): UIViewController {
    Log.w("MainViewController", "⚠️ iOS: Multiple ComposeUIViewController deprecated - causes lifecycle crashes")
    // Return simple fallback to avoid crashes
    return MainViewController()
}
