package com.worldwidewaves

import com.worldwidewaves.shared.WWWPlatform

actual fun getPlatform(): WWWPlatform = IOSPlatform()

actual fun readEventsConfig(): String {
    return "" // TODO
}