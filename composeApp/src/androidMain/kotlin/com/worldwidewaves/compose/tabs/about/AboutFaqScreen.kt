package com.worldwidewaves.compose.tabs.about

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
import com.worldwidewaves.shared.ui.TabScreen
import com.worldwidewaves.shared.ui.screens.about.SharedAboutFaqScreen
import com.worldwidewaves.shared.WWWPlatform

/**
 * Android wrapper for SharedAboutFaqScreen.
 * Handles Android-specific URL opening and navigation while delegating UI to shared component.
 */
class AboutFaqScreen(
    private val platform: WWWPlatform,
) : TabScreen {
    override val name = "FAQ"

    @Composable
    override fun Screen(modifier: Modifier) {
        val uriHandler = LocalUriHandler.current

        SharedAboutFaqScreen(
            platform = platform,
            modifier = modifier,
            onUrlOpen = { url ->
                try {
                    uriHandler.openUri(url)
                } catch (e: Exception) {
                    com.worldwidewaves.shared.utils.Log.e("AboutFaqScreen", "Failed to open URL: $url", throwable = e)
                }
            },
            onSimulateClick = {
                platform.enableSimulationMode()
            }
        )
    }
}