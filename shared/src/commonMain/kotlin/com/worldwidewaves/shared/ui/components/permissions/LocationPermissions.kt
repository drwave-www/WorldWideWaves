package com.worldwidewaves.shared.ui.components.permissions

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.worldwidewaves.shared.MokoRes
import com.worldwidewaves.shared.ui.components.StyledAlertDialog
import dev.icerock.moko.resources.compose.stringResource

/**
 * Shared location permission request component.
 * Handles location permission requests across platforms.
 * Android: Uses Android permission system
 * iOS: Uses iOS location authorization
 */
@Composable
fun SharedLocationPermissionRequest(onPermissionResult: (Boolean) -> Unit = {}): Boolean {
    var showPermissionDialog by remember { mutableStateOf(false) }
    var permissionGranted by remember { mutableStateOf(false) }

    // Platform-specific permission check
    val hasPermission = PlatformLocationPermissionCheck()

    if (hasPermission) {
        permissionGranted = true
        onPermissionResult(true)
    } else if (!showPermissionDialog) {
        showPermissionDialog = true
    }

    if (showPermissionDialog && !hasPermission) {
        LocationPermissionDialog(
            onGranted = {
                showPermissionDialog = false
                permissionGranted = true
                onPermissionResult(true)
                platformRequestLocationPermission()
            },
            onDenied = {
                showPermissionDialog = false
                permissionGranted = false
                onPermissionResult(false)
            },
        )
    }

    return permissionGranted
}

@Composable
private fun LocationPermissionDialog(
    onGranted: () -> Unit,
    onDenied: () -> Unit,
) {
    StyledAlertDialog(
        onDismissRequest = onDenied,
        title = stringResource(MokoRes.strings.geoloc_yourein),
        text = stringResource(MokoRes.strings.geoloc_yourenotin),
        confirmButtonText = stringResource(MokoRes.strings.ok),
        onConfirm = onGranted,
        dismissButtonText = stringResource(MokoRes.strings.map_cancel_download),
        onDismiss = onDenied,
    )
}

/**
 * Platform-specific location permission check.
 * Android: Checks ACCESS_FINE_LOCATION permission
 * iOS: Checks location authorization status
 */
@Composable
expect fun PlatformLocationPermissionCheck(): Boolean

/**
 * Platform-specific location permission request.
 * Android: Launches Android permission request
 * iOS: Requests iOS location authorization
 */
expect fun platformRequestLocationPermission()

/**
 * Shared GPS enable check component.
 * Prompts user to enable GPS/location services if disabled.
 */
@Composable
fun SharedGPSEnableCheck(onResult: (Boolean) -> Unit = {}) {
    var showGPSDialog by remember { mutableStateOf(false) }

    val isGPSEnabled = platformGPSEnabledCheck()

    if (!isGPSEnabled && !showGPSDialog) {
        showGPSDialog = true
    }

    if (showGPSDialog && !isGPSEnabled) {
        GPSEnableDialog(
            onEnable = {
                showGPSDialog = false
                platformOpenLocationSettings()
                onResult(true)
            },
            onCancel = {
                showGPSDialog = false
                onResult(false)
            },
        )
    }
}

@Composable
private fun GPSEnableDialog(
    onEnable: () -> Unit,
    onCancel: () -> Unit,
) {
    StyledAlertDialog(
        onDismissRequest = onCancel,
        title = stringResource(MokoRes.strings.ask_gps_enable),
        text = stringResource(MokoRes.strings.geoloc_yourenotin),
        confirmButtonText = stringResource(MokoRes.strings.yes),
        onConfirm = onEnable,
        dismissButtonText = stringResource(MokoRes.strings.no),
        onDismiss = onCancel,
    )
}

/**
 * Platform-specific GPS enabled check.
 * Android: Checks LocationManager GPS provider
 * iOS: Checks Core Location services enabled
 */
@Composable
expect fun platformGPSEnabledCheck(): Boolean

/**
 * Platform-specific open location settings.
 * Android: Opens location settings Intent
 * iOS: Opens iOS Settings app
 */
expect fun platformOpenLocationSettings()
