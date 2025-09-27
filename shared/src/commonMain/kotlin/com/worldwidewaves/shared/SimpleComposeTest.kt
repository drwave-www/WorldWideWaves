package com.worldwidewaves.shared

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Simple Compose UI following the iOS integration guidance
 */
@Composable
fun AppUI() {
    MaterialTheme {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Button(onClick = {}) {
                Text("🎯 Hello from KMP Compose on iOS!")
            }
        }
    }
}

/**
 * Simple test function that can be called from Swift
 */
fun testSimpleComposeFunction(): String = "✅ Simple Compose function accessible from iOS!"
