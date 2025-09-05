package com.worldwidewaves.utils

/*
 * Copyright 2025 DrWave
 *
 * WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and
 * countries, culminating in a global wave. The project aims to transcend physical and cultural
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

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import com.worldwidewaves.shared.WWWGlobals
import com.worldwidewaves.shared.MokoRes
import dev.icerock.moko.resources.compose.stringResource

/**
 * Session-scoped in-memory marker of the last time the user declined the
 * "enable GPS" system-settings dialog.  Not persisted; reset on process restart.
 */
@Volatile
private var lastGpsEnableDeclinedAtMillis: Long? = null

@Composable
fun requestLocationPermission(): Boolean {
    val context = LocalContext.current
    var permissionGranted by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissionGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true &&
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    LaunchedEffect(Unit) {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocationGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineLocationGranted && coarseLocationGranted) {
            permissionGranted = true
        } else {
            launcher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    return permissionGranted
}

// ------------------------------------

@Composable
fun CheckGPSEnable() {
    val context = LocalContext.current
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    // Check permission directly – avoid calling requestLocationPermission() here
    val permissionGranted = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    // Apply cooldown only for the GPS-enable dialog
    val lastDeclined = lastGpsEnableDeclinedAtMillis
    val withinCooldown = lastDeclined != null &&
            (System.currentTimeMillis() - lastDeclined) <
            WWWGlobals.CONST_GPS_PERMISSION_REASK_DELAY.inWholeMilliseconds

    if (permissionGranted &&
        !withinCooldown &&
        !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    ) {
        AlertDialog.Builder(context)
            .setMessage(stringResource(MokoRes.strings.ask_gps_enable))
            .setCancelable(false)
            .setPositiveButton(stringResource(MokoRes.strings.yes)) { _, _ ->
                startActivity(context, Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), null)
            }
            .setNegativeButton(stringResource(MokoRes.strings.no)) { dialog, _ ->
                lastGpsEnableDeclinedAtMillis = System.currentTimeMillis()
                dialog.cancel()
            }
            .create()
            .show()
    }
}

