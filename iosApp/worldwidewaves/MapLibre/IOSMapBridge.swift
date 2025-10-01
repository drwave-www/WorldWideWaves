/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * https://www.apache.org/licenses/LICENSE-2.0
 */

import Foundation
import Shared
import CoreLocation

/**
 * Bridge between Kotlin and Swift MapLibreViewWrapper.
 * Provides @objc methods that Kotlin can call to interact with the map.
 *
 * This solves the issue of MapLibreViewWrapper being in iosApp (not accessible from shared module).
 */
@objc public class IOSMapBridge: NSObject {
    /**
     * Renders wave polygons on the map for a specific event.
     * Retrieves the wrapper from the registry and calls its methods.
     */
    @objc public static func renderWavePolygons(
        eventId: String,
        polygons: [[CLLocationCoordinate2D]],
        clearExisting: Bool
    ) {
        guard let wrapper = Shared.MapWrapperRegistry.shared.getWrapper(eventId: eventId) as? MapLibreViewWrapper else {
            WWWLog.w("IOSMapBridge", "No wrapper found for event: \(eventId)")
            return
        }

        WWWLog.i("IOSMapBridge", "Rendering \(polygons.count) wave polygons for event: \(eventId)")
        wrapper.addWavePolygons(polygons: polygons, clearExisting: clearExisting)
    }

    /**
     * Checks for pending polygons in the registry and renders them if found.
     * This is called periodically after the map is loaded to render polygons stored by Kotlin.
     */
    @objc public static func renderPendingPolygons(eventId: String) {
        guard let wrapper = Shared.MapWrapperRegistry.shared.getWrapper(eventId: eventId) as? MapLibreViewWrapper else {
            WWWLog.v("IOSMapBridge", "No wrapper found for event: \(eventId)")
            return
        }

        guard Shared.MapWrapperRegistry.shared.hasPendingPolygons(eventId: eventId) else {
            WWWLog.v("IOSMapBridge", "No pending polygons for event: \(eventId)")
            return
        }

        guard let polygonData = Shared.MapWrapperRegistry.shared.getPendingPolygons(eventId: eventId) else {
            return
        }

        WWWLog.i("IOSMapBridge", "Rendering \(polygonData.coordinates.count) pending polygons for event: \(eventId)")

        // Convert coordinate pairs to CLLocationCoordinate2D arrays
        let coordinateArrays: [[CLLocationCoordinate2D]] = polygonData.coordinates.map { polygon in
            polygon.compactMap { coordPair -> CLLocationCoordinate2D? in
                guard let lat = coordPair.first?.doubleValue, let lng = coordPair.second?.doubleValue else {
                    return nil
                }
                return CLLocationCoordinate2D(latitude: lat, longitude: lng)
            }
        }

        // Render on map
        wrapper.addWavePolygons(polygons: coordinateArrays, clearExisting: polygonData.clearExisting)

        // Clear pending polygons after successful rendering
        Shared.MapWrapperRegistry.shared.clearPendingPolygons(eventId: eventId)
        WWWLog.d("IOSMapBridge", "Successfully rendered and cleared pending polygons for event: \(eventId)")
    }

    /**
     * Clears all wave polygons from the map.
     */
    @objc public static func clearWavePolygons(eventId: String) {
        guard let wrapper = Shared.MapWrapperRegistry.shared.getWrapper(eventId: eventId) as? MapLibreViewWrapper else {
            WWWLog.w("IOSMapBridge", "No wrapper found for event: \(eventId)")
            return
        }

        WWWLog.d("IOSMapBridge", "Clearing wave polygons for event: \(eventId)")
        wrapper.clearWavePolygons()
    }
}
