package com.worldwidewaves.compose

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.worldwidewaves.R
import com.worldwidewaves.shared.AndroidPlatform
import com.worldwidewaves.shared.events.WWWEvent
import com.worldwidewaves.shared.events.getMapBbox
import com.worldwidewaves.shared.events.getMapCenter
import com.worldwidewaves.shared.events.getMapStyleUri
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.plugins.annotation.Symbol
import org.maplibre.android.plugins.annotation.SymbolManager
import org.maplibre.android.plugins.annotation.SymbolOptions
import org.maplibre.android.utils.BitmapUtils
import java.io.File

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

class WWWEventMap(private val event: WWWEvent) {

    enum class CameraPosition {
        BOUNDS,
        DEFAULT_CENTER
    }

    @Composable
    fun Screen(
        modifier: Modifier,
        initialCameraPosition: CameraPosition? = CameraPosition.BOUNDS,
    ) {
        val mapView = rememberMapViewWithLifecycle()
        val styleUri = remember { mutableStateOf<Uri?>(null) }

        // Prepare location marker
        var symbolManager: SymbolManager
        var symbol: Symbol
        val drawable = ResourcesCompat.getDrawable(
            (AndroidPlatform.getContext() as Context).resources,
            R.drawable.position_marker, null
        )
        val markerBitmap = remember { BitmapUtils.getBitmapFromDrawable(drawable)!! }

        LaunchedEffect(event) {
            styleUri.value = event.getMapStyleUri()?.let { Uri.fromFile(File(it)) }
        }

        AndroidView(
            modifier = modifier
                .fillMaxWidth()
                .height(300.dp),
            factory = { mapView },
            update = { mv ->
                mv.getMapAsync { map ->
                    styleUri.value?.let { uri ->
                        map.setStyle(
                            Style.Builder()
                                .fromUri(uri.toString())
                                .withImage("position-marker", markerBitmap)
                        ) { style ->
                            map.uiSettings.setAttributionMargins(15, 0, 0, 15)

                            val (cLat, cLng) = event.getMapCenter() // TODO: change this for location
                            symbolManager = SymbolManager(mapView, map, style)
                            symbolManager.iconAllowOverlap = true
                            symbolManager.iconIgnorePlacement = true
                            symbol = symbolManager.create(
                                SymbolOptions()
                                    .withLatLng(LatLng(cLat, cLng))
                                    .withIconImage("position-marker")
                                    .withIconSize(0.2f)
                                    .withIconAnchor("bottom")
                            )
                            symbolManager.update(symbol)
                        }
                    }

                    this.setCameraPosition(initialCameraPosition, map)

                }
            }
        )
    }

    // -- Private functions ---------------------------------------------------

    private fun setCameraPosition(
        initialCameraPosition: CameraPosition?,
        map: MapLibreMap
    ) {
        when (initialCameraPosition) {
            CameraPosition.DEFAULT_CENTER -> {
                val (cLat, cLng) = event.getMapCenter()
                map.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(cLat, cLng),
                        event.mapDefaultzoom ?: event.mapMinzoom.toDouble()
                    )
                )
            }

            CameraPosition.BOUNDS -> {
                val (swLng, swLat, neLng, neLat) = event.getMapBbox()
                val bounds = LatLngBounds.Builder()
                    .include(LatLng(swLat, swLng)) // Southwest corner
                    .include(LatLng(neLat, neLng)) // Northeast corner
                    .build()
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, -20))
            }

            null -> {}
        }
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
