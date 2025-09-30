package com.worldwidewaves.shared.map

/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * https://www.apache.org/licenses/LICENSE-2.0
 */

/**
 * Kotlin interface mirroring MapLibreViewWrapper Swift class.
 *
 * This interface defines the contract between IOSMapLibreAdapter and the
 * Swift MapLibreViewWrapper. The iOS app creates a MapLibreViewWrapper instance
 * and passes it to IOSMapLibreAdapter.setMap().
 *
 * Since we can't directly import Swift classes in Kotlin shared module,
 * we use duck typing: the wrapper is stored as Any and methods are called
 * via this interface for type safety.
 */
interface IMapLibreWrapper {
    // Map Setup
    fun setStyle(
        styleURL: String,
        completion: () -> Unit,
    )

    // Dimensions
    fun getWidth(): Double

    fun getHeight(): Double

    // Camera Position
    fun getCameraCenterLatitude(): Double

    fun getCameraCenterLongitude(): Double

    fun getCameraZoom(): Double

    fun getVisibleBounds(): List<Double>

    // Camera Movement
    fun moveCamera(
        latitude: Double,
        longitude: Double,
        zoom: Double?,
    )

    fun animateCamera(
        latitude: Double,
        longitude: Double,
        zoom: Double?,
        callback: Any?,
    )

    fun animateCameraToBounds(
        swLat: Double,
        swLng: Double,
        neLat: Double,
        neLng: Double,
        padding: Int,
        callback: Any?,
    )

    // Camera Constraints
    fun setBoundsForCameraTarget(
        swLat: Double,
        swLng: Double,
        neLat: Double,
        neLng: Double,
    )

    fun setMinZoom(minZoom: Double)

    fun setMaxZoom(maxZoom: Double)

    fun getMinZoom(): Double

    // Wave Polygons
    fun addWavePolygons(
        polygons: List<List<Any>>,
        clearExisting: Boolean,
    )

    fun clearWavePolygons()

    // Override BBox
    fun drawOverrideBbox(
        swLat: Double,
        swLng: Double,
        neLat: Double,
        neLng: Double,
    )

    // Event Listeners
    fun setOnMapClickListener(listener: (Double, Double) -> Unit)

    fun setOnCameraIdleListener(listener: () -> Unit)
}
