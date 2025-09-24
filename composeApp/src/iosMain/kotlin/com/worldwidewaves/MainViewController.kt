package com.worldwidewaves

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
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

/**
 * iOS entry point for WorldWideWaves Compose application.
 *
 * This function creates the main UIViewController that hosts the Compose application
 * for iOS. It serves as the bridge between iOS UIKit and Compose Multiplatform.
 *
 * Currently shows a placeholder screen. Full iOS implementation will be completed
 * in Phase 2 using the shared BaseViewModel and reactive patterns.
 *
 * Usage in iOS app:
 * ```swift
 * import ComposeApp
 *
 * class ViewController: UIViewController {
 *     override func viewDidLoad() {
 *         super.viewDidLoad()
 *
 *         let composeViewController = MainViewControllerKt.MainViewController()
 *         addChild(composeViewController)
 *         view.addSubview(composeViewController.view)
 *         composeViewController.didMove(toParent: self)
 *     }
 * }
 * ```
 */
fun MainViewController(): UIViewController =
    ComposeUIViewController {
        IOSApp()
    }

@Composable
private fun IOSApp() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("WorldWideWaves iOS - Phase 1 Complete!\n\nShared components ready:\n• BaseViewModel\n• TabManager\n• CoroutineHelpers\n• WWWLogger\n\nPhase 2: iOS UI implementation")
    }
}