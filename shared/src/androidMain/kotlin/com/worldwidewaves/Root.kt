package com.worldwidewaves

import android.content.Context
import com.worldwidewaves.shared.R
import com.worldwidewaves.shared.WWWPlatform

actual fun getPlatform(): WWWPlatform = AndroidPlatform

actual fun readEventsConfig(): String {
    val context = getPlatform().getContext() as Context
    return context.resources.openRawResource(R.raw.events).bufferedReader().use { it.readText() }
}