package com.worldwidewaves.shared.ios

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
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
        NSLog(">>> COMPOSE CONTENT LAMBDA ENTER - 2")
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFFD54F)), // visible amber background
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Hello from Compose (iOS)",
                color = Color.Black,
                fontSize = 28.sp
            )
        }
    }
