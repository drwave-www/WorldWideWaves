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
import com.worldwidewaves.shared.PlatformEnabler
import com.worldwidewaves.shared.WWWPlatform
import com.worldwidewaves.shared.ui.screens.AboutScreen

/**
 * Cross-platform wrapper for SharedAboutScreen.
 * Uses PlatformEnabler for URL opening to maintain cross-platform compatibility.
 */
class AboutTabScreen(
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
            platformEnabler = platformEnabler,
            modifier = modifier,
            onUrlOpen = { url ->
                platformEnabler.openUrl(url)
            },
        )
    }
}
