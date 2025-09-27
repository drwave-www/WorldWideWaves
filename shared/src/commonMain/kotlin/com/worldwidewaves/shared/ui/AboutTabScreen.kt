package com.worldwidewaves.shared.ui

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
import androidx.compose.ui.platform.LocalUriHandler
import com.worldwidewaves.shared.PlatformEnabler
import com.worldwidewaves.shared.WWWPlatform
import com.worldwidewaves.shared.ui.screens.AboutScreen
import com.worldwidewaves.shared.utils.Log

/**
 * Android wrapper for SharedAboutScreen.
 * Handles Android-specific URL opening while delegating UI to shared component.
 */
class AboutTabScreen(
    private val platform: WWWPlatform,
) : TabScreen {
    override val name = "Info"

    @Composable
    override fun Screen(platformEnabler: PlatformEnabler, modifier: Modifier) {
        val uriHandler = LocalUriHandler.current

        AboutScreen(
            platform = platform,
            platformEnabler = platformEnabler,
            modifier = modifier,
            onUrlOpen = { url ->
                try {
                    uriHandler.openUri(url)
                } catch (e: Exception) {
                    Log
                        .e("AboutScreen", "Failed to open URL: $url", throwable = e)
                }
            },
        )
    }
}
