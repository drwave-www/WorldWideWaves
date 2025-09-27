package com.worldwidewaves.shared.ios

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.ComposeUIViewController
import com.worldwidewaves.shared.ui.activities.UIProperties
import com.worldwidewaves.shared.ui.activities.WWWMainActivity
import com.worldwidewaves.shared.utils.IOSPlatformEnabler
import com.worldwidewaves.shared.utils.Log
import platform.UIKit.UIViewController

@Composable
fun AppUI() {
    // Add direct platform logging to ensure we see this call
    platform.Foundation.NSLog("🎯 iOS: AppUI() called - Compose composition working!")

    // Use shared Log system now that Napier is initialized in SceneDelegate
    Log.i("AppUI", "🎯 iOS: AppUI() called - Compose is working!")

    // Create and call WWWMainActivity directly
    platform.Foundation.NSLog("🎯 iOS: Creating IOSPlatformEnabler...")
    Log.i("AppUI", "🎯 iOS: Creating IOSPlatformEnabler...")
    val platformEnabler = IOSPlatformEnabler()
    platform.Foundation.NSLog("✅ iOS: IOSPlatformEnabler created")
    Log.i("AppUI", "✅ iOS: IOSPlatformEnabler created")

    platform.Foundation.NSLog("🎯 iOS: Creating WWWMainActivity...")
    Log.i("AppUI", "🎯 iOS: Creating WWWMainActivity...")
    val mainActivity = WWWMainActivity(platformEnabler = platformEnabler, showSplash = false)
    platform.Foundation.NSLog("✅ iOS: WWWMainActivity created")
    Log.i("AppUI", "✅ iOS: WWWMainActivity created")

    platform.Foundation.NSLog("🎯 iOS: About to call mainActivity.Draw()...")
    Log.i("AppUI", "🎯 iOS: About to call mainActivity.Draw()...")
    // Call Draw() with iOS-safe parameters
    val iosUIProperties =
        UIProperties(
            densityScale = 3.0f,
            containerHeightPx = 2556,
            containerWidthPx = 1179,
        )
    mainActivity.Draw(uiProperties = iosUIProperties)
    platform.Foundation.NSLog("✅ iOS: mainActivity.Draw() completed")
    Log.i("AppUI", "✅ iOS: mainActivity.Draw() completed")
}

fun MakeMainViewController(): UIViewController =
    ComposeUIViewController(configure = { enforceStrictPlistSanityCheck = false }) {
        platform.Foundation.NSLog("🎯 AppUI ENTERED")
        androidx.compose.material3.Text("OK")
    }

/**
 * Fallback approach: Create simple UIViewController that attempts basic Compose rendering
 * This bypasses complex ComposeUIViewController setup
 */
fun createFallbackViewController(): UIViewController {
    platform.Foundation.NSLog("🎯 iOS: Creating fallback ViewController")
    Log.i("RootController", "🎯 iOS: Creating fallback ViewController")

    // Try a different approach: simple ComposeUIViewController without complex configuration
    return try {
        platform.Foundation.NSLog("🎯 iOS: Trying basic ComposeUIViewController in fallback")
        Log.i("RootController", "🎯 iOS: Trying basic ComposeUIViewController in fallback")

        ComposeUIViewController {
            platform.Foundation.NSLog("🎯 iOS: FALLBACK COMPOSE LAMBDA TRIGGERED!")
            Log.i("RootController", "🎯 iOS: FALLBACK COMPOSE LAMBDA TRIGGERED!")
            AppUI()
        }
    } catch (e: Exception) {
        platform.Foundation.NSLog("❌ iOS: Fallback ComposeUIViewController also failed: ${e.message}")
        Log.e("RootController", "❌ iOS: Fallback ComposeUIViewController also failed", e)

        // Last resort: return empty controller with log
        platform.Foundation.NSLog("🎯 iOS: Returning empty UIViewController as last resort")
        Log.i("RootController", "🎯 iOS: Returning empty UIViewController as last resort")
        platform.UIKit.UIViewController()
    }
}
