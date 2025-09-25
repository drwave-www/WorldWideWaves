package com.worldwidewaves.shared

/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

import androidx.compose.ui.window.ComposeUIViewController
import com.worldwidewaves.shared.ui.SharedApp
import com.worldwidewaves.shared.debugBuild
import platform.UIKit.UIViewController

/**
 * iOS entry point for Compose Multiplatform.
 *
 * This creates a UIViewController that hosts the same Compose UI as Android,
 * ensuring perfect UI parity between platforms.
 */
fun MainViewController(): UIViewController {
    // NOTE: Logging and Koin are already initialized in ContentView.swift

    // Add platform-specific logging test
    platform.Foundation.NSLog("🚀 MainViewController: Starting iOS app")

    return ComposeUIViewController {
        platform.Foundation.NSLog("🎯 MainViewController: About to call SharedApp()")
        SharedApp() // Same Compose UI as Android with full DI support
    }
}