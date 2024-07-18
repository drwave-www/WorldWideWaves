package com.worldwidewaves

import com.worldwidewaves.shared.WWWPlatform

expect fun getPlatform(): WWWPlatform
expect fun readEventsConfig(): String