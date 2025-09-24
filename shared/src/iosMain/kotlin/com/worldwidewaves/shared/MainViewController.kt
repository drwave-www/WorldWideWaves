package com.worldwidewaves.shared

import androidx.compose.ui.window.ComposeUIViewController
import com.worldwidewaves.shared.ui.SharedApp
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController { SharedApp() }
