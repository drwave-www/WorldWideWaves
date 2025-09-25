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

import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
actual fun PlatformLocationPermissionCheck(): Boolean {
    val context = LocalContext.current
    return ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}

actual fun PlatformRequestLocationPermission() {
    // This would typically trigger the Android permission request
    // Implementation depends on Activity context
}

@Composable
actual fun PlatformGPSEnabledCheck(): Boolean {
    val context = LocalContext.current
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
}

actual fun PlatformOpenLocationSettings() {
    // Would open Android location settings
    // Implementation depends on Activity context
}
