package com.worldwidewaves.shared.ios

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

@Composable
fun AppUI() {
    // Minimal test to isolate crash - just Text("OK")
    Text("OK")
}

fun MakeMainViewController(): UIViewController =
    ComposeUIViewController(configure = { enforceStrictPlistSanityCheck = false }) { AppUI() }