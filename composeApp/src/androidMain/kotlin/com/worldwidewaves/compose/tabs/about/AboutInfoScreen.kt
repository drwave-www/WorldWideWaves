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
import com.worldwidewaves.shared.ui.screens.about.SharedAboutInfoScreen

/**
 * Android wrapper for SharedAboutInfoScreen.
 * Handles Android-specific URL opening while delegating UI to shared component.
 */
class AboutInfoScreen : TabScreen {
    override val name = "Infos"

    @Composable
    override fun Screen(modifier: Modifier) {
        val uriHandler = LocalUriHandler.current

        SharedAboutInfoScreen(
            modifier = modifier,
            onUrlOpen = { url ->
                try {
                    uriHandler.openUri(url)
                } catch (e: Exception) {
                    com.worldwidewaves.shared.utils.Log
                        .e("AboutInfoScreen", "Failed to open URL: $url", throwable = e)
                }
            },
        )
    }
}
