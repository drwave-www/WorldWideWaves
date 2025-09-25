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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.worldwidewaves.shared.ui.TabManager
import com.worldwidewaves.shared.ui.TabScreen
import org.koin.compose.koinInject

/**
 * Shared Compose App - Identical UI on both Android and iOS.
 *
 * This composable provides the exact same UI experience on both platforms,
 * using shared business logic and identical styling.
 */
@Composable
fun SharedApp() {
    MaterialTheme {
        val tabManager = koinInject<TabManager>()
        val activeTab by tabManager.activeTab.collectAsState()

        Scaffold(
            bottomBar = {
                SharedBottomTabBar(
                    tabManager = tabManager,
                    activeTab = activeTab
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (activeTab) {
                    TabScreen.EVENTS -> {
                        Text("Shared Events Screen - Coming Soon")
                    }
                    TabScreen.ABOUT -> SharedAboutScreen()
                    TabScreen.DEBUG -> SharedDebugScreen()
                }
            }
        }
    }
}

/**
 * Shared bottom tab bar - identical on both platforms
 */
@Composable
private fun SharedBottomTabBar(
    tabManager: TabManager,
    activeTab: TabScreen
) {
    // TODO: Implement shared bottom tab bar matching Android design
    Box(
        modifier = Modifier.padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("Shared Tab Bar - Events | About | Debug")
    }
}

/**
 * Placeholder for shared About screen
 */
@Composable
private fun SharedAboutScreen() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("About WorldWideWaves", style = MaterialTheme.typography.headlineMedium)
        Text("Shared about screen - identical on both platforms")
    }
}

/**
 * Placeholder for shared Debug screen
 */
@Composable
private fun SharedDebugScreen() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Debug Settings", style = MaterialTheme.typography.headlineMedium)
        Text("Shared debug screen - identical on both platforms")
    }
}