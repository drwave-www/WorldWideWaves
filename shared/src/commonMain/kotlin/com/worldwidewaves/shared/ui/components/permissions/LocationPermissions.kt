package com.worldwidewaves.shared.ui.components.permissions

/*
 * Copyright 2025 DrWave
 *
 * WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and
 * countries. The project aims to transcend physical and cultural
 * boundaries, fostering unity, community, and shared human experience by leveraging real-time
 * coordination and location-based services.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.worldwidewaves.shared.MokoRes
import com.worldwidewaves.shared.utils.Log
import dev.icerock.moko.resources.compose.stringResource

/**
 * Shared location permission request component.
 * Handles location permission requests across platforms.
 * Android: Uses Android permission system
 * iOS: Uses iOS location authorization
 */
@Composable
fun SharedLocationPermissionRequest(
    onPermissionResult: (Boolean) -> Unit = {},
): Boolean {
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
                PlatformRequestLocationPermission()
            },
            onDenied = {
                showPermissionDialog = false
                permissionGranted = false
                onPermissionResult(false)
            }
        )
    }

    return permissionGranted
}

@Composable
private fun LocationPermissionDialog(
    onGranted: () -> Unit,
    onDenied: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDenied,
        title = { Text(stringResource(MokoRes.strings.geoloc_request_title)) },
        text = { Text(stringResource(MokoRes.strings.geoloc_request_message)) },
        confirmButton = {
            Button(onClick = onGranted) {
                Text(stringResource(MokoRes.strings.geoloc_request_allow))
            }
        },
        dismissButton = {
            TextButton(onClick = onDenied) {
                Text(stringResource(MokoRes.strings.geoloc_request_deny))
            }
        }
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
expect fun PlatformRequestLocationPermission()

/**
 * Shared GPS enable check component.
 * Prompts user to enable GPS/location services if disabled.
 */
@Composable
fun SharedGPSEnableCheck(
    onResult: (Boolean) -> Unit = {},
) {
    var showGPSDialog by remember { mutableStateOf(false) }

    val isGPSEnabled = PlatformGPSEnabledCheck()

    if (!isGPSEnabled && !showGPSDialog) {
        showGPSDialog = true
    }

    if (showGPSDialog && !isGPSEnabled) {
        GPSEnableDialog(
            onEnable = {
                showGPSDialog = false
                PlatformOpenLocationSettings()
                onResult(true)
            },
            onCancel = {
                showGPSDialog = false
                onResult(false)
            }
        )
    }
}

@Composable
private fun GPSEnableDialog(
    onEnable: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(MokoRes.strings.geoloc_enable_title)) },
        text = { Text(stringResource(MokoRes.strings.geoloc_enable_message)) },
        confirmButton = {
            Button(onClick = onEnable) {
                Text(stringResource(MokoRes.strings.geoloc_enable_settings))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(stringResource(MokoRes.strings.geoloc_enable_cancel))
            }
        }
    )
}

/**
 * Platform-specific GPS enabled check.
 * Android: Checks LocationManager GPS provider
 * iOS: Checks Core Location services enabled
 */
@Composable
expect fun PlatformGPSEnabledCheck(): Boolean

/**
 * Platform-specific open location settings.
 * Android: Opens location settings Intent
 * iOS: Opens iOS Settings app
 */
expect fun PlatformOpenLocationSettings()