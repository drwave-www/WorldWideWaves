package com.worldwidewaves.shared.compose.tabs

/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.worldwidewaves.shared.PlatformEnabler
import com.worldwidewaves.shared.WWWPlatform
import com.worldwidewaves.shared.ui.TabScreen
import com.worldwidewaves.shared.ui.screens.AboutScreen
import com.worldwidewaves.shared.utils.Log
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

/**
 * iOS wrapper for SharedAboutScreen.
 * Handles iOS-specific URL opening while delegating UI to shared component.
 */
class AboutScreen(
    private val platform: WWWPlatform,
) : TabScreen {
    override val name = "Info"

    @Composable
    override fun Screen(
        platformEnabler: PlatformEnabler,
        modifier: Modifier,
    ) {
        AboutScreen(
            platform = platform,
            modifier = modifier,
            onUrlOpen = { url ->
                try {
                    val nsUrl = NSURL.URLWithString(url)
                    if (nsUrl != null && UIApplication.sharedApplication.canOpenURL(nsUrl)) {
                        UIApplication.sharedApplication.openURL(nsUrl)
                    } else {
                        Log.e("AboutScreen", "Cannot open URL: $url")
                    }
                } catch (e: Exception) {
                    Log.e("AboutScreen", "Failed to open URL: $url", throwable = e)
                }
            },
        )
    }
}
