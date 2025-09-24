package com.worldwidewaves.shared.testing

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

import com.worldwidewaves.shared.events.utils.BoundingBox
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.map.MapCameraCallback
import com.worldwidewaves.shared.map.MapLibreAdapter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Mock implementation of MapLibreAdapter for testing map-related functionality.
 *
 * Provides controllable map behavior, interaction recording, and state verification
 * for comprehensive testing of map-dependent components.
 */
class MockMapAdapter : MapLibreAdapter<Any> {
    // Map properties
    private var mockMap: Any? = null
    private var currentStyle: String? = null
    private var mapWidth: Double = 800.0
    private var mapHeight: Double = 600.0

    // Camera state
    private val _currentPosition = MutableStateFlow<Position?>(TestHelpers.TestLocations.PARIS)
    override val currentPosition: StateFlow<Position?> = _currentPosition.asStateFlow()

    private val _currentZoom = MutableStateFlow(10.0)
    override val currentZoom: StateFlow<Double> = _currentZoom.asStateFlow()

    private var cameraPosition: Position? = TestHelpers.TestLocations.PARIS
    private var visibleRegion: BoundingBox = createDefaultBounds()
    private var constraintBounds: BoundingBox? = null

    // Zoom constraints
    private var minZoom: Double = 1.0
    private var maxZoom: Double = 20.0

    // Attribution margins
    private var attributionMargins = AttributionMargins(0, 0, 0, 0)

    // Interaction tracking
    private val cameraAnimations = mutableListOf<CameraAnimation>()
    private val wavePolygonOperations = mutableListOf<WavePolygonOperation>()
    private val styleChanges = mutableListOf<StyleChange>()
    private val boundsOperations = mutableListOf<BoundsOperation>()

    // Listeners
    private var mapClickListener: ((Double, Double) -> Unit)? = null
    private val cameraIdleListeners = mutableListOf<() -> Unit>()

    // Simulation flags
    private var animationDuration: Long = 0 // 0 = instant for testing
    private var shouldFailAnimations: Boolean = false
    private var shouldDelayCallbacks: Boolean = false

    override fun setMap(map: Any) {
        this.mockMap = map
    }

    override fun setStyle(
        stylePath: String,
        callback: () -> Unit?,
    ) {
        currentStyle = stylePath
        styleChanges.add(StyleChange(stylePath, System.currentTimeMillis()))

        if (!shouldDelayCallbacks) {
            callback()
        }
    }

    override fun getWidth(): Double = mapWidth

    override fun getHeight(): Double = mapHeight

    override fun getCameraPosition(): Position? = cameraPosition

    override fun getVisibleRegion(): BoundingBox = visibleRegion

    override fun moveCamera(bounds: BoundingBox) {
        visibleRegion = bounds
        cameraPosition = calculateCenter(bounds)
        _currentPosition.value = cameraPosition
        boundsOperations.add(BoundsOperation("moveCamera", bounds, System.currentTimeMillis()))
    }

    override fun animateCamera(
        position: Position,
        zoom: Double?,
        callback: MapCameraCallback?,
    ) {
        val animation =
            CameraAnimation(
                type = "animateCamera",
                targetPosition = position,
                targetZoom = zoom,
                timestamp = System.currentTimeMillis(),
            )
        cameraAnimations.add(animation)

        if (!shouldFailAnimations) {
            // Simulate successful animation
            cameraPosition = position
            _currentPosition.value = position
            zoom?.let {
                _currentZoom.value = it
            }

            if (!shouldDelayCallbacks) {
                callback?.onFinish()
            }
        } else {
            if (!shouldDelayCallbacks) {
                callback?.onCancel()
            }
        }
    }

    override fun animateCameraToBounds(
        bounds: BoundingBox,
        padding: Int,
        callback: MapCameraCallback?,
    ) {
        val animation =
            CameraAnimation(
                type = "animateCameraToBounds",
                targetBounds = bounds,
                padding = padding,
                timestamp = System.currentTimeMillis(),
            )
        cameraAnimations.add(animation)

        if (!shouldFailAnimations) {
            // Simulate successful animation
            visibleRegion = bounds
            cameraPosition = calculateCenter(bounds)
            _currentPosition.value = cameraPosition

            if (!shouldDelayCallbacks) {
                callback?.onFinish()
            }
        } else {
            if (!shouldDelayCallbacks) {
                callback?.onCancel()
            }
        }
    }

    override fun setBoundsForCameraTarget(constraintBounds: BoundingBox) {
        this.constraintBounds = constraintBounds
        boundsOperations.add(
            BoundsOperation("setBoundsForCameraTarget", constraintBounds, System.currentTimeMillis()),
        )
    }

    override fun getMinZoomLevel(): Double = minZoom

    override fun setMinZoomPreference(minZoom: Double) {
        this.minZoom = minZoom
    }

    override fun setMaxZoomPreference(maxZoom: Double) {
        this.maxZoom = maxZoom
    }

    override fun setAttributionMargins(
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
    ) {
        attributionMargins = AttributionMargins(left, top, right, bottom)
    }

    override fun addWavePolygons(
        polygons: List<Any>,
        clearExisting: Boolean,
    ) {
        wavePolygonOperations.add(
            WavePolygonOperation(
                polygons = polygons,
                clearExisting = clearExisting,
                timestamp = System.currentTimeMillis(),
            ),
        )
    }

    override fun setOnMapClickListener(listener: ((Double, Double) -> Unit)?) {
        mapClickListener = listener
    }

    override fun addOnCameraIdleListener(callback: () -> Unit) {
        cameraIdleListeners.add(callback)
    }

    override fun drawOverridenBbox(bbox: BoundingBox) {
        boundsOperations.add(BoundsOperation("drawOverridenBbox", bbox, System.currentTimeMillis()))
    }

    // Testing utilities

    /**
     * Simulates a map click at the specified coordinates.
     */
    fun simulateMapClick(
        latitude: Double,
        longitude: Double,
    ) {
        mapClickListener?.invoke(latitude, longitude)
    }

    /**
     * Simulates camera idle event.
     */
    fun simulateCameraIdle() {
        cameraIdleListeners.forEach { it() }
    }

    /**
     * Sets the map dimensions for testing.
     */
    fun setMapDimensions(
        width: Double,
        height: Double,
    ) {
        mapWidth = width
        mapHeight = height
    }

    /**
     * Sets the visible region for testing.
     */
    fun setVisibleRegion(bounds: BoundingBox) {
        visibleRegion = bounds
        cameraPosition = calculateCenter(bounds)
        _currentPosition.value = cameraPosition
    }

    /**
     * Configures animation behavior for testing.
     */
    fun configureAnimations(
        shouldFail: Boolean = false,
        shouldDelay: Boolean = false,
        duration: Long = 0,
    ) {
        shouldFailAnimations = shouldFail
        shouldDelayCallbacks = shouldDelay
        animationDuration = duration
    }

    /**
     * Manually triggers animation completion for delayed callbacks.
     */
    fun completeDelayedAnimations() {
        // This would be used in tests that need to control animation timing
    }

    /**
     * Gets recorded camera animations for verification.
     */
    fun getCameraAnimations(): List<CameraAnimation> = cameraAnimations.toList()

    /**
     * Gets recorded wave polygon operations for verification.
     */
    fun getWavePolygonOperations(): List<WavePolygonOperation> = wavePolygonOperations.toList()

    /**
     * Gets recorded style changes for verification.
     */
    fun getStyleChanges(): List<StyleChange> = styleChanges.toList()

    /**
     * Gets recorded bounds operations for verification.
     */
    fun getBoundsOperations(): List<BoundsOperation> = boundsOperations.toList()

    /**
     * Gets current style path.
     */
    fun getCurrentStyle(): String? = currentStyle

    /**
     * Gets current constraint bounds.
     */
    fun getConstraintBounds(): BoundingBox? = constraintBounds

    /**
     * Gets current attribution margins.
     */
    fun getAttributionMargins(): AttributionMargins = attributionMargins

    /**
     * Gets zoom constraints.
     */
    fun getZoomConstraints(): ZoomConstraints = ZoomConstraints(minZoom, maxZoom)

    /**
     * Clears all recorded operations for fresh testing.
     */
    fun clearHistory() {
        cameraAnimations.clear()
        wavePolygonOperations.clear()
        styleChanges.clear()
        boundsOperations.clear()
    }

    /**
     * Resets the mock to default state.
     */
    fun reset() {
        mockMap = null
        currentStyle = null
        mapWidth = 800.0
        mapHeight = 600.0
        _currentPosition.value = TestHelpers.TestLocations.PARIS
        _currentZoom.value = 10.0
        cameraPosition = TestHelpers.TestLocations.PARIS
        visibleRegion = createDefaultBounds()
        constraintBounds = null
        minZoom = 1.0
        maxZoom = 20.0
        attributionMargins = AttributionMargins(0, 0, 0, 0)
        mapClickListener = null
        cameraIdleListeners.clear()
        animationDuration = 0
        shouldFailAnimations = false
        shouldDelayCallbacks = false
        clearHistory()
    }

    /**
     * Verifies that a specific camera animation was performed.
     */
    fun verifyCameraAnimationTo(
        position: Position,
        zoom: Double? = null,
    ) {
        val matchingAnimation =
            cameraAnimations.find {
                it.targetPosition == position && (zoom == null || it.targetZoom == zoom)
            }
        if (matchingAnimation == null) {
            throw AssertionError(
                "Expected camera animation to position $position with zoom $zoom, but none found. Actual animations: $cameraAnimations",
            )
        }
    }

    /**
     * Verifies that a specific bounds animation was performed.
     */
    fun verifyCameraAnimationToBounds(
        bounds: BoundingBox,
        padding: Int = 0,
    ) {
        val matchingAnimation =
            cameraAnimations.find {
                it.targetBounds == bounds && it.padding == padding
            }
        if (matchingAnimation == null) {
            throw AssertionError(
                "Expected camera animation to bounds $bounds with padding $padding, but none found. Actual animations: $cameraAnimations",
            )
        }
    }

    /**
     * Verifies that wave polygons were added.
     */
    fun verifyWavePolygonsAdded(
        expectedCount: Int? = null,
        clearExisting: Boolean? = null,
    ) {
        val operations =
            if (clearExisting != null) {
                wavePolygonOperations.filter { it.clearExisting == clearExisting }
            } else {
                wavePolygonOperations
            }

        if (expectedCount != null) {
            val totalPolygons = operations.sumOf { it.polygons.size }
            if (totalPolygons != expectedCount) {
                throw AssertionError("Expected $expectedCount wave polygons, but found $totalPolygons")
            }
        }

        if (operations.isEmpty()) {
            throw AssertionError("Expected wave polygons to be added, but none were found")
        }
    }

    private fun calculateCenter(bounds: BoundingBox): Position {
        val centerLat = (bounds.sw.latitude + bounds.ne.latitude) / 2.0
        val centerLng = (bounds.sw.longitude + bounds.ne.longitude) / 2.0
        return Position(centerLat, centerLng)
    }

    private fun createDefaultBounds(): BoundingBox {
        val center = TestHelpers.TestLocations.PARIS
        return BoundingBox(
            center.latitude - 0.01,
            center.longitude - 0.01,
            center.latitude + 0.01,
            center.longitude + 0.01,
        )
    }

    // Data classes for tracking operations

    data class CameraAnimation(
        val type: String,
        val targetPosition: Position? = null,
        val targetBounds: BoundingBox? = null,
        val targetZoom: Double? = null,
        val padding: Int = 0,
        val timestamp: Long,
    )

    data class WavePolygonOperation(
        val polygons: List<Any>,
        val clearExisting: Boolean,
        val timestamp: Long,
    )

    data class StyleChange(
        val stylePath: String,
        val timestamp: Long,
    )

    data class BoundsOperation(
        val operation: String,
        val bounds: BoundingBox,
        val timestamp: Long,
    )

    data class AttributionMargins(
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int,
    )

    data class ZoomConstraints(
        val min: Double,
        val max: Double,
    )

    companion object {
        /**
         * Creates a mock map adapter configured for city testing.
         */
        fun forCity(city: String): MockMapAdapter {
            val position =
                when (city.lowercase()) {
                    "paris" -> TestHelpers.TestLocations.PARIS
                    "london" -> TestHelpers.TestLocations.LONDON
                    "newyork", "new_york" -> TestHelpers.TestLocations.NEW_YORK
                    "tokyo" -> TestHelpers.TestLocations.TOKYO
                    "sydney" -> TestHelpers.TestLocations.SYDNEY
                    "saopaulo", "sao_paulo" -> TestHelpers.TestLocations.SAO_PAULO
                    else -> TestHelpers.TestLocations.PARIS
                }

            return MockMapAdapter().apply {
                cameraPosition = position
                _currentPosition.value = position
                visibleRegion =
                    BoundingBox(
                        position.latitude - 0.01,
                        position.longitude - 0.01,
                        position.latitude + 0.01,
                        position.longitude + 0.01,
                    )
            }
        }

        /**
         * Creates a mock map adapter that simulates animation failures.
         */
        fun withAnimationFailures(): MockMapAdapter =
            MockMapAdapter().apply {
                configureAnimations(shouldFail = true)
            }

        /**
         * Creates a mock map adapter with delayed callbacks for testing async behavior.
         */
        fun withDelayedCallbacks(): MockMapAdapter =
            MockMapAdapter().apply {
                configureAnimations(shouldDelay = true)
            }

        /**
         * Creates a mock map adapter with custom dimensions.
         */
        fun withDimensions(
            width: Double,
            height: Double,
        ): MockMapAdapter =
            MockMapAdapter().apply {
                setMapDimensions(width, height)
            }
    }
}
