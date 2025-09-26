package com.worldwidewaves.shared.ui.components.navigation

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
import com.worldwidewaves.shared.generated.resources.Res
import com.worldwidewaves.shared.generated.resources.about_icon
import com.worldwidewaves.shared.generated.resources.about_icon_selected
import com.worldwidewaves.shared.generated.resources.debug_icon
import com.worldwidewaves.shared.generated.resources.debug_icon_selected
import com.worldwidewaves.shared.generated.resources.waves_icon
import com.worldwidewaves.shared.generated.resources.waves_icon_selected
import org.jetbrains.compose.resources.DrawableResource

/**
 * Shared tab configuration data and logic.
 * Provides consistent tab structure and icon management across platforms.
 */
object TabConfiguration {
    /**
     * Gets tab information (icons) based on configuration.
     * iOS and Android will use identical tab structure.
     */
    fun getTabInfo(includeDebug: Boolean): List<Pair<DrawableResource, DrawableResource>> =
        if (includeDebug) {
            listOf(
                Res.drawable.waves_icon to Res.drawable.waves_icon_selected,
                Res.drawable.about_icon to Res.drawable.about_icon_selected,
                Res.drawable.debug_icon to Res.drawable.debug_icon_selected,
            )
        } else {
            listOf(
                Res.drawable.waves_icon to Res.drawable.waves_icon_selected,
                Res.drawable.about_icon to Res.drawable.about_icon_selected,
            )
        }
}

/**
 * Shared tab bar item with automatic icon selection.
 * Uses shared tab configuration to determine appropriate icons.
 * This eliminates platform-specific wrapper logic.
 */
@Composable
fun ConfigurableTabBarItem(
    isSelected: Boolean,
    tabIndex: Int,
    contentDescription: String?,
    totalTabs: Int,
    modifier: Modifier = Modifier,
) {
    val tabInfo = TabConfiguration.getTabInfo(totalTabs > 2)
    TabBarItem(
        isSelected = isSelected,
        selectedIcon = tabInfo[tabIndex].second,
        unselectedIcon = tabInfo[tabIndex].first,
        contentDescription = contentDescription,
        modifier = modifier,
    )
}
