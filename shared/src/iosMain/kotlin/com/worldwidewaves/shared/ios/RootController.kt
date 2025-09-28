package com.worldwidewaves.shared.ios

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.ComposeUIViewController
import platform.Foundation.NSLog
import platform.UIKit.UIViewController

@Composable
fun AppUI() {
    NSLog(">>> AppUI ENTER")
    Text("Hello from Compose on iOS")
}

fun MakeMainViewController(): UIViewController =
    ComposeUIViewController(configure = { enforceStrictPlistSanityCheck = false }) {
        NSLog(">>> COMPOSE CONTENT LAMBDA ENTER")
        AppUI()
    }
