package com.worldwidewaves.compose

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.worldwidewaves.shared.events.WWWEvent
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.InputStream

/*
 * Copyright 2024 DrWave
 *
 * WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and countries,
 * culminating in a global wave. The project aims to transcend physical and cultural boundaries, fostering unity,
 * community, and shared human experience by leveraging real-time coordination and location-based services.
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

class WWWEventMap(private val event : WWWEvent) {

    @Composable
    fun Screen(modifier: Modifier) {
        val mapView = rememberMapViewWithLifecycle()
        val context = LocalContext.current

        val styleUri= getStyleUri(context, event.id)

        val bounds = LatLngBounds.Builder()
            .include(LatLng(48.812848, 2.242911)) // Southwest corner
            .include(LatLng(48.905836, 2.418073)) // Northeast corner
            .build()

        AndroidView(
            modifier = modifier.width(300.dp).height(300.dp),
            factory = { mapView },
            update = { mapView ->
                mapView.getMapAsync { map ->
//                        map.setStyle(
//                            "https://api.maptiler.com/maps/58e6cfd1-716a-4195-b99e-8a6896e51812/style.json?key=Pe7kSmGevTDugaLmtlc9"
//                        ) // TODO
//                        {
//                            map.uiSettings.setAttributionMargins(15, 0, 0, 15)
//                            // Set the map view center
//                            map.cameraPosition = CameraPosition.Builder()
//                                .target(LatLng( 48.8619, 2.3417))
//                                .zoom(14.0)
//                                .bearing(2.0)
//                                .build()
//                        }

                    val styleFileContents = File(styleUri.path).readText() // TODO debug

                    map.setStyle(Style.Builder().fromUri(styleUri.toString()))
                    {
                        map.uiSettings.setAttributionMargins(15, 0, 0, 15)
                        // Set the map view center
                        map.cameraPosition = CameraPosition.Builder()
                            .target(LatLng( 48.8619, 2.3417))
                            .zoom(14.0)
                            .bearing(2.0)
                            .build()
                    }

                    map.addOnMapClickListener {
                        // Handle map click events
                        val t = map
                        true
                    }

                    map.animateCamera(
                        CameraUpdateFactory.newLatLngBounds(
                            bounds, 50
                        )
                    )
                }
            }
        )
    }

}

// -- Use the MapLibre MapView as a composable --------------------------------

/**
 * Remembers a MapView and gives it the lifecycle of the current LifecycleOwner
 * source : https://gist.github.com/PiotrPrus/d65378c36b0a0c744e647946f344103c
 */
@Composable
fun rememberMapViewWithLifecycle(): MapView {
    val context = LocalContext.current

    val maplibreMapOptions = MapLibreMapOptions.createFromAttributes(context)
    maplibreMapOptions.apply {
        //apiBaseUri("https://demotiles.maplibre.org/tiles/tiles.json")
        camera(
            CameraPosition.Builder()
                .bearing(0.0)
                .target(LatLng(48.8619, 2.3417))
                .zoom(10.0)
                .tilt(0.0)
                .build()
        )
        maxZoomPreference(14.0)
        minZoomPreference(10.0)
        localIdeographFontFamily("Droid Sans")
        zoomGesturesEnabled(true)
        compassEnabled(true)
        compassFadesWhenFacingNorth(true)
        scrollGesturesEnabled(true)
        rotateGesturesEnabled(true)
        tiltGesturesEnabled(true)
        debugActive(true)
    }
    MapLibre.getInstance(context)

    val mapView = remember { MapView(context, maplibreMapOptions) }

    // Makes MapView follow the lifecycle of this composable
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, mapView) {
        val lifecycleObserver = getMapLifecycleObserver(mapView)
        lifecycle.addObserver(lifecycleObserver)
        onDispose {
            lifecycle.removeObserver(lifecycleObserver)
        }
    }

    return mapView
}

private fun getMapLifecycleObserver(mapView: MapView): LifecycleEventObserver =
    LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_CREATE -> mapView.onCreate(Bundle())
            Lifecycle.Event.ON_START -> mapView.onStart()
            Lifecycle.Event.ON_RESUME -> mapView.onResume()
            Lifecycle.Event.ON_PAUSE -> mapView.onPause()
            Lifecycle.Event.ON_STOP -> mapView.onStop()
            Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
            else -> throw IllegalStateException()
        }
    }

// ----------------------------------------------------------

fun getMBTilesAbsoluteFilePath(context: Context, fileName: String): String {
    val inputStream: InputStream = context.assets.open(fileName)
    val assetSize = inputStream.available()
    val cacheDir = context.cacheDir
    val cachedFile = File(cacheDir, fileName)

    if (cachedFile.exists()) {
        val cachedFileSize = cachedFile.length().toInt()
        if (cachedFileSize == assetSize) {
            inputStream.close()
            return cachedFile.absolutePath
        }
    }

    FileOutputStream(cachedFile).use { outputStream ->
        inputStream.copyTo(outputStream)
    }
    inputStream.close()
    return cachedFile.absolutePath
}

fun getStyleUri(context: Context, eventId: String): Uri {
    val mbtilesFilePath = getMBTilesAbsoluteFilePath(context, "$eventId.mbtiles")
    val cacheDir = context.cacheDir
    val styleFile = File(cacheDir, "style-$eventId.json")

    // Check if the cached JSON file already exists // TODO. uncomment
    //if (styleFile.exists()) {
    //    return Uri.fromFile(styleFile)
    //}

    val styleJsonInputStream: InputStream = context.assets.open("mapstyle.json")

    // Copy the original JSON content to the new file
    styleJsonInputStream.use { input ->
        FileOutputStream(styleFile).use { output ->
            input.copyTo(output)
        }
    }

    // Replace the placeholder with the URI of the mbtiles file
    val newFileStr = styleFile.readText().replace("___FILE_URI___", "mbtiles:///$mbtilesFilePath")
    BufferedWriter(FileWriter(styleFile)).use { out ->
        out.write(newFileStr)
    }

    return Uri.fromFile(styleFile)
}